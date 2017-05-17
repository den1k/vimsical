(ns vimsical.backend.components.datomic.fixture
  (:require
   [clojure.string :as str]
   [com.stuartsierra.component :as cp]
   [datomic.api :as d]
   [vimsical.backend.components.datomic :as sut])
  (:import java.util.UUID))

;;
;; * State
;;

(def ^:dynamic *datomic*)

;;
;; * Helpers
;;

(defn name-uuid
  [name]
  (-> name (str "-test-" (UUID/randomUUID)) (str/replace "-" "_")))

(defn- test-config
  []
  (-> (sut/env-conf)
      (update :api.datomic/name name-uuid)))

;;
;; * Fixture
;;

(defn datomic
  [f]
  (binding [*datomic* (cp/start (sut/->datomic (test-config)))]
    (try
      (sut/create-schema! *datomic*)
      (f)
      (finally
        (sut/delete-database! *datomic*)
        (cp/stop *datomic*)))))
