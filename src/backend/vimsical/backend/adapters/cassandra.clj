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
   [vimsical.backend.adapters.cassandra.protocol :as protocol]
   [vimsical.backend.adapters.cassandra.util :as util]
   [vimsical.common.util.core :as common.util])
  (:import [com.datastax.driver.core PreparedStatement ResultSetFuture Session Statement]))

;;
;; * Driver
;;

(nippy/set-nippy-decoder!)
(nippy/set-nippy-serializable-encoder!)

;;
;; * Spec
;;

(s/def ::Session (partial instance? Session))
(s/def ::PreparedStatement (partial instance? PreparedStatement))
(s/def ::Statement (partial instance? Statement))
(s/def ::ResultSetFuture (partial instance? ResultSetFuture))

;;
;; * Queries
;;

(s/def ::raw-query (s/or :raw string? :hayt map?))
(s/def ::query-identifier keyword?)
(s/def ::alia-executable (s/or :raw ::raw-query :prep ::PreparedStatement :stmt ::Statement))
(s/def ::executable (s/or :key ::query-identifier :exec ::alia-executable))
(s/def ::values (s/map-of :keyword any?))
(s/def ::command (s/or :exec (s/tuple ::executable) :with-vals (s/tuple ::executable ::values)))
(s/def ::queries (s/map-of ::query-identifier ::raw-query))
(s/def ::prepared (s/map-of ::query-identifier ::PreparedStatement))

;;
;; * Prepared statemenents
;;

;; NOTE Can't spec those due to stateful components

(defn ->prepared
  "Upgrade raw queries to prepared statements."
  [session queries]
  (common.util/map-vals #(alia/prepare session %) queries))

(defn prepared
  [connection key]
  (get-in connection [:prepared key]))

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
;; * Connection options
;;

(defn- session-fetch-size
  [^Session session]
  (-> session .getCluster .getConfiguration .getQueryOptions .getFetchSize))

(def default-options
  {:result-set-fn #(into [] (map util/underscores->hyphens) %)})

(defn options [opts] (merge default-options opts))

(defn async-options
  [opts success error]
  (assoc (options opts) :success success :error error))

;;
;; * Connection
;;

(defprotocol ICassandraInternal
  (create-keyspace! [_])
  (drop-keyspace!   [_]))

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
    (do
      ;; Create the keyspace before trying to connect
      (create-keyspace! this)
      ;; Establish session and prepare queries
      (let [session            (alia/connect cluster keyspace)
            default-fetch-size (session-fetch-size session)
            this'              (assoc this
                                      :session session
                                      :default-fetch-size default-fetch-size)]
        (protocol/prepare-queries this' queries))))
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
    (if-let [executable (if (keyword? executable) (prepared this executable) executable)]
      (let [statement (command->statement this executable)
            opts'     (async-options opts success error)]
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
      (let [statements (commands->statements this commands)
            batch      (alia/batch statements batch-type)
            opts'      (async-options opts success error)]
        (alia/execute-async session batch opts'))
      (catch Throwable t
        (error t))))

  protocol/ICassandraChan
  (execute-chan
    [this executable]
    (protocol/execute-chan this executable nil))
  (execute-chan
    [{:as this :keys [session]} executable {:as opts :keys [channel] :or {channel (async/chan 1)}}]
    {:pre [session]}
    (if-let [executable (if (keyword? executable) (prepared this executable) executable)]
      (let [statement (command->statement this executable)
            opts'     (options opts)]
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
      (let [statements (commands->statements this commands)
            batch      (alia/batch statements batch-type)
            opts'      (options opts)]
        (alia.async/execute-chan-buffered session batch opts'))
      (catch Throwable t
        (doto channel
          (async/put! t)
          (async/close!))))))

(s/def ::connection (partial instance? Connection))

;;
;; * Conf & constructors
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

(s/fdef ->cluster :args (s/cat :conf ::cluster-conf) :ret ::cluster/cluster)

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
