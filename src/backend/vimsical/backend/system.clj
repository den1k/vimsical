(ns vimsical.backend.system
  "TODO
  - Have a strategy to measure latency and throughput
  - Add an nRepl server component for prod"
  (:require
   [clojure.spec.alpha :as s]
   [com.stuartsierra.component :as cp]
   [vimsical.backend.adapters.cassandra :as cassandra]
   [vimsical.backend.adapters.cassandra.cluster :as cassandra-cluster]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.delta-store :as delta-store]
   [vimsical.backend.components.server :as server]
   [vimsical.backend.components.service :as service]
   [vimsical.backend.components.session-store :as session-store]
   [vimsical.backend.components.snapshot-store :as snapshot-store]
   [vimsical.common.env :as env]))

;;
;; * Spec
;;

(s/def ::cassandra-cluster ::cassandra-cluster/cluster)
(s/def ::cassandra-connection ::cassandra/connection)
(s/def ::delta-store ::delta-store/delta-store)
(s/def ::datomic ::datomic/datomic)
(s/def ::session-store ::session-store/session-store)
(s/def ::server ::server/server)

;; NOTE use unqualified keys to avoid circular dependencies in downstream
;; pedestal context consumers (see deps interceptor)
(s/def ::system
  (s/keys :req-un [::cassandra-cluster
                   ::cassandra-connection
                   ::delta-store
                   ::datomic
                   ::session-store
                   ::server]))

;;
;; * System map
;;

(s/fdef new-system :ret ::system)

(defn new-system []
  (cp/system-map

   ;;
   ;; * Cassandra delta-store
   ;;

   :cassandra-cluster
   (cassandra/->cluster
    {::cassandra/contact-points (env/required :cassandra-contact-points (env/comma-separated ::env/string))
     ::cassandra/port           (env/required :cassandra-port ::env/int)
     ::cassandra/retry-policy   :default})

   :cassandra-connection
   (cp/using
    (cassandra/->connection
     {::cassandra/keyspace           (env/required :cassandra-keyspace ::env/string)
      ::cassandra/replication-factor (env/required :cassandra-replication-factor ::env/int)})
    {:cluster :cassandra-cluster})

   :delta-store
   (cp/using
    (delta-store/->delta-store)
    {:cassandra :cassandra-connection})

   :snapshot-store
   (cp/using
    (snapshot-store/->snapshot-store)
    {:cassandra :cassandra-connection})

   ;;
   ;; * Datomic database
   ;;

   :datomic
   (datomic/->datomic (datomic/env-conf))

   ;;
   ;; * Redis sessions-store
   ;;

   :session-store
   (session-store/->session-store
    {::session-store/host (env/required :redis-host ::env/string)
     ::session-store/port (env/required :redis-port ::env/int)})

   ;;
   ;; * Immutant <> Pedestal HTTP stack
   ;;

   :service-map service/service-map

   :server
   (cp/using
    (server/->server)
    [:datomic
     :delta-store
     :snapshot-store
     :session-store
     :service-map])))
