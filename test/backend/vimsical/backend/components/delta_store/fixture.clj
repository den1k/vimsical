(ns vimsical.backend.components.delta-store.fixture
  (:require
   [clojure.test :as t]
   [com.stuartsierra.component :as cp]
   [vimsical.backend.adapters.cassandra.fixture :as cassandra]
   [vimsical.backend.components.delta-store :as delta-store]))

;;
;; * State
;;

(def ^:dynamic *delta-store* nil)

;;
;; * Helpers
;;

(defn- ->delta-store []
  (-> (delta-store/->delta-store)
      (assoc :cassandra cassandra/*connection*)
      (cp/start)))

(defn- delta-store
  [f]
  {:pre [cassandra/*connection*]}
  (binding [*delta-store* (->delta-store)]
    (try
      (f)
      (finally
        (cp/stop *delta-store*)))))

;;
;; * Fixtures
;;

(def once cassandra/cluster)

(def each (t/compose-fixtures cassandra/connection delta-store))
