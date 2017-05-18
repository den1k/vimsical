(ns vimsical.backend.adapters.cassandra.cluster
  (:require
   [clojure.spec :as s]
   [com.stuartsierra.component :as cp]
   [qbits.alia.policy.load-balancing :as balance]
   [qbits.alia.policy.retry :as retry])
  (:import
   (com.datastax.driver.core Cluster)
   (com.datastax.driver.core.policies LoadBalancingPolicy RetryPolicy)))

;;
;; * Spec
;;

(s/def ::cluster (partial instance? Cluster))
(s/def ::load-balancing-policy (partial instance? LoadBalancingPolicy))
(s/def ::retry-policy (partial instance? RetryPolicy))
(s/def ::contact-points (s/every string?))
(s/def ::port pos-int?)
(s/def ::keyspace string?)

;;
;; * Retry policy
;;

(s/fdef ->retry-policy :args (s/cat :target (s/nilable keyword?)) :ret  ::retry-policy)

(defn ->retry-policy
  [option]
  (case option
    :default              (retry/default-retry-policy)
    :fallthrough          (retry/fallthrough-retry-policy)
    :downgrading          (retry/downgrading-consistency-retry-policy)
    :logging->default     (retry/logging-retry-policy (retry/default-retry-policy))
    :logging->fallthrough (retry/logging-retry-policy (retry/fallthrough-retry-policy))
    :logging->downgrading (retry/logging-retry-policy (retry/downgrading-consistency-retry-policy))
    (retry/default-retry-policy)))

;;
;; * Load balancing policy
;;

(s/fdef ->load-balancing-policy :args (s/cat :target (s/nilable keyword?)) :ret  ::load-balancing-policy)

(defn ->load-balancing-policy
  [option]
  (case option
    :round-robin (balance/round-robin-policy)
    (balance/token-aware-policy (balance/round-robin-policy))))

;;
;; * Conf
;;

(s/def ::conf
  (s/keys :req-un [::contact-points ::port ::retry-policy ::load-balancing-policy]))

;;
;; * Lifecycle
;;

(extend-type Cluster
  cp/Lifecycle
  (start [this]
    (.init this))
  (stop [this]
    (future (.close this))
    this))
