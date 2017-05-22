(ns vimsical.backend.system.fixture
  "NOTE:
  - We're not clearing the session state after running the fixture"
  (:require
   [com.stuartsierra.component :as cp]
   [io.pedestal.http :as http]
   [vimsical.backend.adapters.cassandra :as cassandra]
   [vimsical.backend.adapters.cassandra.fixture :as cassandra.fixture]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.datomic.fixture :as datomic.fixture]
   [vimsical.backend.system :as system]
   [vimsical.common.env :as env]))

;;
;; * State
;;

(def ^:dynamic *system* nil)
(def ^:dynamic *service-fn* nil)

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
      (binding [*service-fn* (get-in *system* [:server :service ::http/service-fn])]
        (try
          (f)
          (finally
            (some-> *system* :datomic datomic/delete-database!)
            (some-> *system* :cassandra-connection cassandra/drop-keyspace!)
            (cp/stop *system*)))))))
