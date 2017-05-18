(ns vimsical.backend.components.snapshot-store.fixture
  (:require
   [clojure.test :as t]
   [com.stuartsierra.component :as cp]
   [vimsical.backend.adapters.cassandra.fixture :as cassandra]
   [vimsical.backend.components.snapshot-store :as snapshot-store]))

;;
;; * State
;;

(def ^:dynamic *snapshot-store* nil)

;;
;; * Helpers
;;

(defn- ->snapshot-store []
  (-> (snapshot-store/->snapshot-store)
      (assoc :cassandra cassandra/*connection*)
      (cp/start)))

(defn- snapshot-store
  [f]
  {:pre [cassandra/*connection*]}
  (binding [*snapshot-store* (->snapshot-store)]
    (try
      (f)
      (finally
        (cp/stop *snapshot-store*)))))

;;
;; * Fixtures
;;

(def once cassandra/cluster)

(def each (t/compose-fixtures cassandra/connection snapshot-store))
