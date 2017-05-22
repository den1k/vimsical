;; Need to be sourced in the relevant profiles in project.clj
{:env.backend/dev
 {:env
  {:env                          "dev"
   ;; HTTP server
   :http-bind-host               "localhost"
   :http-port                    "8080"
   :csrf                         "false"
   ;; Datomic
   :datomic-protocol             "dev"
   :datomic-host                 "localhost"
   :datomic-port                 "4334"
   :datomic-name                 "vimsical"
   ;; Cassandra
   :cassandra-contact-points     "localhost"
   :cassandra-port               "9042"
   :cassandra-keyspace           "vimsical"
   :cassandra-replication-factor "1"
   ;; Redis
   :redis-host                   "localhost"
   :redis-port                   "6379"}}

 :env.backend/test
 {:env
  {:env                          "test"}}

 :env.frontend/dev
 {:env
  {:remote-protocol "http"
   :remote-host     "localhost"
   :remote-path     "/remote"
   :remote-port     "8080"}}}
