(ns vimsical.backend.system.fixture
  (:require
   [clojure.core.async :as async]
   [clojure.spec :as s]
   [com.stuartsierra.component :as cp]
   [io.pedestal.http :as http]
   [vimsical.backend.adapters.cassandra :as cassandra]
   [vimsical.backend.adapters.cassandra.fixture :as cassandra.fixture]
   [vimsical.backend.adapters.redis :as redis]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.datomic.fixture :as datomic.fixture]
   [vimsical.backend.components.delta-store.protocol :as delta-store.protocol]
   [vimsical.backend.components.session-store :as session-store]
   [vimsical.backend.data :as data]
   [vimsical.backend.system :as system]
   [vimsical.common.env :as env]
   [vimsical.user :as user]))

;;
;; * State
;;

(def ^:dynamic *system* nil)
(def ^:dynamic *service-fn* nil)

;;
;; * User
;;

(def ^:dynamic *user-uid* nil)

(defn with-user
  [{:keys [db/uid] :as user}]
  (s/assert* ::user/user user)
  (fn user-fixture [f]
    (if-some [datomic (-> *system* :datomic)]
      (do
        ;; Write user to datomic
        (datomic/transact datomic user)
        ;; Bind user-uid
        (binding [*user-uid* uid]
          (f)))
      (throw (ex-info "`with-user` fixture should be nest inside the `system` fixture." {})))))

;;
;; * Session
;;

(def ^:dynamic *session-key* nil)

(defn session
  ([] (session #()))
  ([f]
   (if-some [session-store (-> *system* :session-store)]
     (if (nil? *user-uid*)
       (throw (ex-info "`session` fixture should be nested inside the `user` fixture." {}))
       (let [session {::user/uid *user-uid*}]
         (binding [*session-key* (session-store/write-session* session-store nil session)]
           (f))))
     (throw (ex-info "`session` fixture should be nested inside the `system` fixture" {})))))

;;
;; * Vims
;;

(defn vims+deltas
  ([] (vims+deltas #()))
  ([f]
   (if (some? *system*)
     (if (nil? *user-uid*)
       (throw (ex-info "`vims` fixture should be nested inside the `user` fixture." {}))
       (let [{:keys [delta-store datomic]} *system*]
         (datomic/transact datomic data/vims)
         (async/<!!
          (delta-store.protocol/insert-deltas-chan
           delta-store (-> data/vims :db/uid) data/deltas))
         (f)))
     (throw (ex-info "`vims` fixture should be nested inside the `system` fixture" {})))))

;;
;; * System
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
            (some-> *system* :session-store redis/flushall!)
            (cp/stop *system*)))))))
