(ns vimsical.backend.adapters.cassandra.fixture
  (:require
   [clojure.string :as str]
   [com.stuartsierra.component :as cp]
   [vimsical.backend.adapters.cassandra :as sut]
   [vimsical.common.env :as env])
  (:import java.util.UUID))

;;
;; * Test conf
;;

(def cluster-conf
  {::sut/contact-points     (env/required :cassandra-contact-points (env/comma-separated ::env/string))
   ::sut/port               (env/required :cassandra-port ::env/int)
   ::sut/retry-policy       :default})

(def connection-conf
  {::sut/replication-factor (env/required :cassandra-replication-factor ::env/int)})

(def ^:dynamic *keyspace* nil)
(def ^:dynamic *cluster* nil)
(def ^:dynamic *connection* nil)

(defn keyspace-uuid []
  (-> (str "deltas-test-" (UUID/randomUUID))
      (str/replace "-" "_")))

;;
;; * Fixtures
;;

(defn cluster
  [f]
  (let [cluster (-> cluster-conf sut/->cluster cp/start)]
    (try
      (binding [*cluster* cluster]
        (f))
      (finally
        (future
          (cp/stop cluster))))))

(defn new-connection
  [keyspace cluster]
  (let [conf {::sut/keyspace keyspace}]
    (-> (merge connection-conf conf)
        sut/->connection
        (assoc :cluster *cluster*))))

;; NOTE the connection automatically creates its keyspace when started, we only
;; need to drop it in the finally clause, alternatively we could use a truncate
;; approach which would be faster
(defn connection
  [f]
  {:pre [*cluster*]}
  (binding [*keyspace* (keyspace-uuid)]
    (binding [*connection* (cp/start (new-connection *keyspace* *cluster*))]
      (try
        (f)
        (finally
          (sut/drop-keyspace! *connection*)
          (cp/stop *connection*))))))
