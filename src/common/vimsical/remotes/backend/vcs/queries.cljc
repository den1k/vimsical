(ns vimsical.remotes.backend.vcs.queries
  (:require [clojure.spec.alpha :as s]
            [vimsical.remotes.event :as event]
            [vimsical.vims :as vims]))

(defmethod event/event-spec ::delta-by-branch-uid [_] (s/cat :id any? :vims-uid ::vims/uid))
