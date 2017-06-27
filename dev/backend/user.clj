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

(defonce system nil)

(defn drop!
  ([] (drop! system))
  ([{:keys [datomic session-store cassandra-connection]}]
   (datomic/delete-database! datomic)
   (redis/flushall! session-store)
   (cassandra/drop-keyspace! cassandra-connection)))

;;
;; * Reloaded
;;

(defn start!   [] (alter-var-root #'system (fn [_] (cp/start (sys/new-system)))))
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
      (require '[clojure.spec.test.alpha :as st])
      (st/instrument)
      (s/check-asserts true))
  (st/instrument))
