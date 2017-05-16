(ns vimsical.remotes.event
  (:require [clojure.spec :as s]))

(s/def ::id keyword?)
(s/def ::args (s/* any?))
(s/def ::event (s/cat :id ::id :args ::args))
