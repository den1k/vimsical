(ns vimsical.remotes.backend.status.queries
  (:require
   [clojure.spec :as s]
   [vimsical.remotes.event :as event]))

(defmethod event/event-spec ::status [_] (s/cat :id any?))
