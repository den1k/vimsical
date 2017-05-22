(ns vimsical.backend.adapters.redis.fixture
  (:require
   [com.stuartsierra.component :as cp]
   [vimsical.backend.adapters.redis :as sut]
   [vimsical.common.env :as env]))

;;
;; * State
;;

(def ^:dynamic *redis*)

;;
;; * Helpers
;;

(defn- new-redis []
  (cp/start
   (sut/->redis
    {:host (env/required :redis-host ::env/string)
     :port (env/required :redis-port ::env/int)})))

;;
;; * Fixture
;;

(defn redis
  [f]
  (binding [*redis* (new-redis)]
    (try
      (f)
      (finally
        (sut/flushall! *redis*)
        (cp/stop *redis*)))))
