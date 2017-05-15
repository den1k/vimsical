(ns vimsical.backend.components.datomic.fixture
  (:require
   [clojure.string :as str]
   [com.stuartsierra.component :as cp]
   [datomic.api :as datomic]
   [vimsical.backend.components.datomic :as sut])
  (:import java.util.UUID))

;;
;; * State
;;

(def ^:dynamic *datomic*)

;;
;; * Helpers
;;

(defn- uuid-name
  [name]
  (-> name (str "-test-" (UUID/randomUUID)) (str/replace "-" "_")))

(defn- test-config
  []
  (-> (sut/env-conf)
      (update :api.datomic/name uuid-name)))

;;
;; * Fixture
;;

(defn datomic
  [f]
  (let [config                    (test-config)
        {:keys [uri] :as datomic} (cp/start (sut/->datomic config))]
    (try
      (binding [*datomic* datomic]
        (sut/create-schema *datomic*)
        (f))
      (finally
        (datomic/delete-database uri)
        (cp/stop datomic)))))
