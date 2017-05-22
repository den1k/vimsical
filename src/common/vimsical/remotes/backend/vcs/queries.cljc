(ns vimsical.remotes.backend.vcs.queries
  (:require [clojure.spec :as s]
            [vimsical.remotes.event :as event]
            [vimsical.vims :as vims]))

(defmethod event/event-spec ::latest-deltas-by-branch-uid [_] (s/cat :id any? :deltas ::vims/uid))
