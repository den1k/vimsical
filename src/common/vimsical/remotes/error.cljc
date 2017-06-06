(ns vimsical.remotes.error
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::remote keyword?)

(s/def ::backoff int?)
(s/def ::reason string?)
(s/def ::message string?)

(s/def ::error (s/keys :req [::remote ::event] :opt [::backoff ::reason ::message]))
