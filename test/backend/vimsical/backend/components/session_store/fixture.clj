(ns vimsical.backend.components.session-store.fixture
  (:require
   [com.stuartsierra.component :as cp]
   [vimsical.backend.components.session-store :as sut]
   [vimsical.backend.adapters.redis :as redis]
   [vimsical.common.env :as env]))

;;
;; * State
;;

(def ^:dynamic *session-store*)

;;
;; * Helpers
;;

(defn- new-session-store []
  (sut/->session-store
   {::sut/host (env/required :redis-host ::env/string)
    ::sut/port (env/required :redis-port ::env/int)}))

;;
;; * Fixture
;;

(defn session-store
  [f]
  (binding [*session-store* (cp/start (new-session-store))]
    (try
      (f)
      (finally
        (redis/flushall! *session-store*)
        (cp/stop *session-store*)))))
