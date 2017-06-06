(ns vimsical.remotes.backend.vcs.commands
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.remotes.event :as event]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]))


(defmethod event/event-spec ::add-deltas [_] (s/cat :id any? :vims-uid uuid? :deltas (s/every ::delta/delta)))

(defmethod event/event-spec ::add-branch [_] (s/cat :id any? :deltas ::branch/branch))
