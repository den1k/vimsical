(ns vimsical.backend.system.fixture
  (:require
   [vimsical.common.env :as env]
   [com.stuartsierra.component :as cp]
   [vimsical.backend.system :as system]
   [vimsical.backend.adapters.cassandra :as cassandra]
   [vimsical.backend.adapters.cassandra.fixture :as cassandra.fixture]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.datomic.fixture :as datomic.fixture]))

;;
;; * State

(def ^:dynamic *system* nil)

;;
;; * Fixture
;;

(defn system
  [f]
  ;; 1. Force the :test env, will make datomic run in-memory
  ;; 2. Generate new databases
  ;; 3. Tear down dbs
  (env/with-env
    {:env                :test
     :cassandra-keyspace (cassandra.fixture/keyspace-uuid)
     :datomic-name       (datomic.fixture/name-uuid (env/required :datomic-name ::env/string))}
    (binding [*system* (cp/start (system/new-system))]
      (try
        (f)
        (finally
          (some-> *system* ::system/datomic datomic/delete-database!)
          (some-> *system* ::system/cassandra-connection cassandra/drop-keyspace!)
          (cp/stop *system*))))))
