(ns vimsical.backend.adapters.cassandra
  (:require
   [clojure.core.async :as async]
   [clojure.spec :as s]
   [com.stuartsierra.component :as cp]
   [qbits.alia :as alia]
   [qbits.alia.async :as alia.async]
   [qbits.alia.codec.nippy :as nippy]
   [vimsical.backend.adapters.cassandra.cluster :as cluster]
   [vimsical.backend.adapters.cassandra.cql :as cql]
   [vimsical.common.util.core :as util])
  (:import
   (com.datastax.driver.core Cluster ResultSetFuture Session Statement)))

;;
;; * Driver
;;

(nippy/set-nippy-decoder!)
(nippy/set-nippy-serializable-encoder!)

;;
;; * Spec
;;

(s/def ::Session (partial instance? Session))
(s/def ::Statement (partial instance? Statement))
(s/def ::ResultSetFuture (partial instance? ResultSetFuture))

;;
;; * Queries
;;

(s/def ::raw-query (s/or :raw string? :hayt map?))
(s/def ::query-identifier keyword?)
(s/def ::alia-executable (s/or :raw ::raw-query :prep ::prepared-statement :stmt ::Statement))
(s/def ::executable (s/or :key ::query-identifier :exec ::alia-executable))
(s/def ::values (s/or :vec (s/every any?) :map (s/map-of ::query-identifier any?)))
(s/def ::command (s/or :exec (s/tuple ::executable) :with-vals (s/tuple ::executable ::values)))
(s/def ::queries (s/map-of ::query-identifier ::raw-query))
(s/def ::prepared (s/map-of ::query-identifier ::prepared))

;;
;; * Prepared statemenents
;;

(s/fdef ->prepared
        :args (s/cat :session ::Session :queries ::queries)
        :ret  ::queries)

(defn ->prepared
  "Upgrade raw queries to prepared statements."
  [session queries]
  (common.util/map-vals #(alia/prepare session %) queries))

(defn prepared
  [connection key]
  (get-in connection [::prepared key]))

(defn prepared?
  [connection queries]
  (every? #(prepared connection %) (keys queries)))

(defn command->statement
  [connection [executable values :as command]]
  (cond
    (string? command) command
    (vector? command)
    (if-some [executable (if (keyword? executable) (prepared connection executable) executable)]
      (alia/query->statement executable values)
      (throw (ex-info "invalid executable" {:executable executable})))))

(defn commands->statements
  [connection commands]
  (not-empty
   (map
    (fn [command]
      (command->statement connection command))
    commands)))

;;
;; * Execution
;;

(defn execute
  ([connection executable] (execute connection executable nil))
  ([{:as connection :keys [session]}
    executable
    {:as opts :keys [channel]:or {channel (async/chan 1)}}]
   {:pre [session]}
   (if-let [executable (if (keyword? executable) (prepared connection executable) executable)]
     ;; Note: alia/execute-chan-buffered because of https://github.com/mpenet/alia/issues/29
     (alia.async/execute-chan-buffered session executable opts)
     (doto channel
       (async/put! (ex-info "invalid executable" {:executable executable}))
       (async/close!)))))

(defn execute-batch
  ([connection commands] (execute-batch connection commands :logged))
  ([connection commands batch-type] (execute-batch connection commands batch-type nil))
  ([{:keys [session] :as connection} commands batch-type {:keys [channel] :as opts :or {channel (async/chan 1)}}]
   (try
     (let [statements (commands->statements connection commands)
           batch      (alia/batch statements batch-type)]
       (alia.async/execute-chan-buffered session batch opts))
     (catch Throwable t
       (doto channel
         (async/put! t)
         (async/close!))))))

;;
;; * Connection
;;

(defn- session-fetch-size
  [session]
  (-> session .getCluster .getConfiguration .getQueryOptions .getFetchSize))

(defn- create-keyspace!
  [{:keys [keyspace replication-factor cluster]}]
  (let [session     (alia/connect cluster)
        cql         (cql/create-keyspace keyspace replication-factor)]
    (alia/execute session cql)))

(defrecord Connection
    [keyspace
     replication-factor
     queries
     prepared
     session
     cluster
     default-fetch-size]
  cp/Lifecycle
  (start [this]
    (assert (and cluster keyspace) "cluster and/or keyspace cannot be nil")
    (create-keyspace! this)
    (let [session            (alia/connect cluster keyspace)
          default-fetch-size (session-fetch-size session)]
      (cond-> (assoc this
                     :session session
                     :default-fetch-size default-fetch-size)
        (seq queries) (assoc :prepared (->prepared session queries)))))
  (stop [this]
    (-> this
        (update :session alia/shutdown)
        (dissoc :prepared)))

  ICassandraInternal
  (create-keyspace! [_]
    ;; Need a connection that doesn't specify the keyspace since we haven't
    ;; created it yet!
    (let [session (alia/connect cluster)
          cql     (cql/create-keyspace keyspace replication-factor)]
      (alia/execute session cql)))
  (drop-keyspace! [_]
    (let [cql (cql/drop-keyspace keyspace)]
      (do (alia/execute session cql) nil)))

  protocol/ICassandra
  (create-schema! [_ schema]
    (doseq [table schema] (alia/execute session table)))
  (prepare-queries [this queries]
    (cond-> this
      (seq queries) (update :prepared merge (->prepared session queries))))

  protocol/ICassandraAsync
  (execute-async
    [this executable success error]
    (protocol/execute-async this executable nil success error))
  (execute-async
    [{:as this :keys [session]} executable opts success error]
    {:pre [session]}
    (if-some [statement (command->statement this executable)]
      (let [opts' (async-options opts success error)]
        (alia/execute-async session statement opts'))
      (error (ex-info "invalid executable" {:executable executable}))))

  (execute-batch-async
    [this commands success error]
    (protocol/execute-batch-async this commands :logged success error))
  (execute-batch-async
    [this commands batch-type success error]
    (protocol/execute-batch-async this commands batch-type nil success error))
  (execute-batch-async
    [{:keys [session] :as this} commands batch-type opts success error]
    (try
      (if-some [statements (commands->statements this commands)]
        (let [batch (alia/batch statements batch-type)
              opts' (async-options opts success error)]
          (alia/execute-async session batch opts'))
        (error (ex-info "invalid commands" {:commands commands})))
      (catch Throwable t
        (error t))))

  protocol/ICassandraChan
  (execute-chan
    [this executable]
    (protocol/execute-chan this executable nil))
  (execute-chan
    [{:as this :keys [session]} executable {:as opts :keys [channel] :or {channel (async/chan 1)}}]
    {:pre [session]}
    (if-some [statement (command->statement this executable)]
      (let [opts' (options opts)]
        ;; Note: alia/execute-chan-buffered because of
        ;; https://github.com/mpenet/alia/issues/29
        (alia.async/execute-chan-buffered session statement opts'))
      (doto channel
        (async/put! (ex-info "invalid executable" {:executable executable}))
        (async/close!))))
  (execute-batch-chan
    [this commands]
    (protocol/execute-batch-chan this commands :logged))
  (execute-batch-chan
    [this commands batch-type]
    (protocol/execute-batch-chan this commands batch-type nil))
  (execute-batch-chan
    [{:keys [session] :as this} commands batch-type {:keys [channel] :as opts :or {channel (async/chan 1)}}]
    (try
      (if-some [statements (commands->statements this commands)]
        (let [batch (alia/batch statements batch-type)
              opts' (options opts)]
          (alia.async/execute-chan-buffered session batch opts'))
        (throw (ex-info "invalid commands" {:commands commands})))
      (catch Throwable t
        (doto channel
          (async/put! t)
          (async/close!))))))

(s/def ::connection (partial instance? Connection))

;;
;; * Conf
;;

;;
;; ** Cluster
;;

(s/def ::contact-points (s/every string?))
(s/def ::port pos-int?)
(s/def ::retry-policy keyword?)
(s/def ::load-balancing-policy keyword?)

(s/def ::cluster-conf
  (s/keys :req [::contact-points ::port] :opt [::retry-policy ::load-balancing-policy]))

(s/fdef ->cluster-conf  :args (s/cat :conf ::cluster-conf) :ret ::cluster/conf)

(defn ->cluster-conf [conf]
  (-> conf
      (cluster/remove-ns)
      (update :retry-policy cluster/->retry-policy)
      (update :load-balancing-policy cluster/->load-balancing-policy)))

(s/fdef ->cluster :args (s/cat :conf ::cluster-conf) :ret ::Cluster)

(defn ->cluster [conf]
  (alia/cluster (->cluster-conf conf)))

;;
;; ** Connection
;;

(s/def ::conf
  (s/keys :req [::keyspace ::replication-factor] :opt [::queries]))

(s/fdef ->connection
        :args (s/cat :conf ::conf)
        :ret  ::connection)

(defn ->connection
  [{::keys [keyspace replication-factor queries]}]
  (map->Connection
   {:keyspace           keyspace
    :replication-factor replication-factor
    :queries            queries}))
