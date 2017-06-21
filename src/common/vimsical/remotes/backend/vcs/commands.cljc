(ns vimsical.remotes.backend.vcs.commands
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.remotes.event :as event]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.lib :as lib]))


(defmethod event/event-spec ::add-deltas    [_] (s/cat :id any? :vims-uid uuid? :deltas (s/every ::delta/delta)))
(defmethod event/event-spec ::add-branch    [_] (s/cat :id any? :branch ::branch/branch))
(defmethod event/event-spec ::add-lib       [_] (s/cat :id any? :branch-uid ::branch/uid :lib ::lib/lib))
(defmethod event/event-spec ::remove-lib    [_] (s/cat :id any? :branch-uid ::branch/uid :lib ::lib/lib))
