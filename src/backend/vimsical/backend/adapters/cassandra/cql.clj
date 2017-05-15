(ns vimsical.backend.adapters.cassandra.cql
  (:require
   [clojure.spec :as s]
   [qbits.hayt :as cql]))

;;
;; * Spec
;;

(s/def ::cql (s/or :raw string? :hayt map?))
(s/def ::keyspace string?)
(s/def ::replication-factor int?)

;;
;; * Keyspace
;;

(s/fdef create-keyspace
        :args (s/cat :keyspace ::keyspace :rep-fact ::replication-factor)
        :ret  ::cql)

(defn create-keyspace [keyspace replication-factor]
  (cql/->raw
   (cql/create-keyspace
    (keyword keyspace)
    (cql/if-exists false)
    (cql/with {:replication
               {:class "SimpleStrategy"
                :replication_factor replication-factor}}))))

(s/fdef truncate-keyspace :args (s/cat :keyspace ::keyspace) :ret ::cql)

(defn truncate-keyspace [keyspace]
  (cql/->raw
   (cql/truncate keyspace)))

(s/fdef drop-keyspace :args (s/cat :keyspace ::keyspace) :ret ::cql)

(defn drop-keyspace [keyspace]
  (cql/->raw
   (cql/drop-keyspace keyspace)))

;;
;; * Tables
;;

(s/fdef create-table
        :args (s/cat :table keyword? :col-def (s/or :map map? :tuples (s/every (s/tuple keyword any?))))
        :ret  ::cql)

(defn create-table
  [table column-definitions]
  (cql/->raw
   (cql/create-table table (cql/column-definitions column-definitions))))
