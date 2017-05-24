(ns vimsical.remotes.backend.vims.queries
  (:require
   [clojure.spec :as s]
   [vimsical.remotes.event :as event]
   [vimsical.vims :as vims]
   [vimsical.vcs.delta :as delta]))

;;
;; * Vims
;;

(defmethod event/event-spec  ::vims [_] (s/cat :id any? :vims-uid ::vims/uid))
(defmethod event/result-spec ::vims [_] (s/cat :id any? :vims ::vims/vims))

;;
;; * Deltas
;;

(defmethod event/event-spec  ::deltas [_] (s/cat :id any? :vims-uid ::vims/uid))
(defmethod event/result-spec ::deltas [_] (s/cat :id any? :deltas (s/every ::delta/delta)))
