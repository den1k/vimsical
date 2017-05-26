(ns vimsical.remotes.backend.status.queries
  (:require
   [clojure.spec :as s]
   [vimsical.remotes.event :as event]))

(s/def ::status #{:ok})
(s/def ::result (s/keys :req-un [::status]))

(defmethod event/event-spec  ::status [_] (s/cat :id any?))
(defmethod event/result-spec ::status [_] (s/cat :id any? :result ::result))
