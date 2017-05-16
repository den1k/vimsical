(ns vimsical.backend.system
  "TODO
  - Have a strategy to measure latency and throughput
  - Add an nRepl server component for prod
  - Logging"
  (:require

   [com.stuartsierra.component :as cp]
   [vimsical.backend.adapters.cassandra :as cassandra]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.service :as service]
   [vimsical.backend.components.server :as server]
   [vimsical.backend.components.delta-store :as delta-store]
   [vimsical.backend.components.session-store :as session-store]
   [vimsical.common.env :as env]))

(defn new-system []
  (cp/system-map

   ;;
   ;; * Cassandra delta-store
   ;;

   ::cassandra-cluster
   (cassandra/->cluster
    {::cassandra/contact-points (env/required :cassandra-contact-points (env/comma-separated ::env/string))
     ::cassandra/port           (env/required :cassandra-port ::env/int)
     ::cassandra/retry-policy   :default})

   ::cassandra-connection
   (cp/using
    (cassandra/->connection
     {::cassandra/keyspace (env/required :cassandra-keyspace ::env/string)})
    {:cluster ::cassandra-cluster})

   ::delta-store
   (cp/using
    (delta-store/->delta-store)
    {:cassandra ::cassandra-connection})

   ;;
   ;; * Datomic database
   ;;

   ::datomic
   (datomic/->datomic (datomic/env-conf))

   ;;
   ;; * Redis sessions-store
   ;;

   ::session-store
   (session-store/->session-store
    {::session-store/host (env/required :redis-host ::env/string)
     ::session-store/port (env/required :redis-port ::env/int)})

   ;;
   ;; * Immutant <> Pedestal HTTP stack
   ;;

   ::server
   (cp/using
    (server/->server
     {::server/service-map service/service-map})
    [::datomic ::delta-store ::session-store])))
