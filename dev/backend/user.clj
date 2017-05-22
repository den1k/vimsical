(ns backend.user
  (:require
   [com.stuartsierra.component :as cp]
   [vimsical.backend.adapters.cassandra :as cassandra]
   [vimsical.backend.adapters.redis :as redis]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.system :as sys]))

;;
;; * State
;;

(def system (sys/new-system))

(defn drop!
  ([] (drop! system))
  ([{:api.system/keys [datomic redis cassandra]}]
   (datomic/delete-database! datomic)
   (redis/flushall! redis)
   (cassandra/drop-keyspace! cassandra)))

;;
;; * Reloaded
;;

(defn start!   [] (alter-var-root #'system cp/start))
(defn stop!    [] (alter-var-root #'system cp/stop))
(defn restart! [] (do (try (stop!) (catch Throwable _)) (start!)))

(defn refresh! []
  (restart!)
  (drop!)
  (restart!))

;;
;; * Actions
;;

(comment
  (start!)
  (stop!)
  (restart!)
  (refresh!))

(comment
  (do
    (require '[clojure.spec.test :as st])
    (st/instrument)
    (s/check-asserts true))
  (st/instrument))
