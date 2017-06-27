(ns vimsical.backend.adapters.cassandra
  (:require
   [clojure.core.async :as async]
   [clojure.spec.alpha :as s]
   [com.stuartsierra.component :as cp]
   [qbits.alia :as alia]
   [qbits.alia.async :as alia.async]
   [qbits.alia.codec.nippy :as nippy]
   [vimsical.backend.adapters.cassandra.cluster :as cluster]
   [vimsical.backend.adapters.cassandra.cql :as cql]
   [vimsical.backend.adapters.cassandra.protocol :as protocol]
   [vimsical.backend.adapters.cassandra.util :as util]
   [vimsical.common.util.core :as common.util])
  (:import
   [com.datastax.driver.core PreparedStatement ResultSetFuture Session Statement]))

;;
;; * Async helpers
;;

(defn new-error-chan
  ([] (new-error-chan 2))
  ([n] (async/chan n (map identity) identity)))

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
(s/def ::values (s/or :keywords (s/map-of ::query-identifier any?) :positional (s/every any?)))
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
  (transduce
   (map (fn [command]
          (command->statement connection command)))
   (fnil conj [])
   nil
   commands))

;;
;; * Connection options
;;

(defn- session-fetch-size
  [^Session session]
  (-> session .getCluster .getConfiguration .getQueryOptions .getFetchSize))

(def default-options
  {:result-set-fn #(into [] (map util/underscores->hyphens) %)})

(defn new-options [options] (merge default-options options))

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
        (update :session #(future (alia/shutdown %)))
        (dissoc :prepared :session :cluster :default-fetch-size :keyspace)))

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

  protocol/ICassandraChan
  (execute-chan
    [this executable]
    (protocol/execute-chan this executable nil))
  (execute-chan
    [{:as this :keys [session]} executable {:as options :keys [channel] :or {channel (new-error-chan)}}]
    {:pre [session]}
    (if-some [statement (command->statement this executable)]
      (let [options' (new-options options)]
        ;; Note: alia/execute-chan-buffered because of
        ;; https://github.com/mpenet/alia/issues/29
        (alia.async/execute-chan-buffered session statement options'))
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
    [{:keys [session] :as this} commands batch-type options]
    (assert (-> options :channel nil?) "execute-batch-chan doesn't allow for custom channels")
    ;; NOTE in order to ensure writes do not timeout we might spread the
    ;; commands over multiple requests, which makes it inconvenient to let api
    ;; clients pass their own channel.
    (try
      (if-some [statements (commands->statements this commands)]
        (async/merge
         (for [statements (sequence (partition-all default-fetch-size) statements)]
           (let [batch    (alia/batch statements batch-type)
                 options' (new-options options)]
             (alia.async/execute-chan-buffered session batch options'))))
        (throw (ex-info "invalid commands" {:commands commands})))
      (catch Throwable t
        (doto (new-error-chan)
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
      (common.util/unqualify-keys)
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

