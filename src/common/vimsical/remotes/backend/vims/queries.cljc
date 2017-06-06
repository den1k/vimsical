(ns vimsical.remotes.backend.vims.queries
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj [clojure.core.async.impl.protocols :as ap])
   [vimsical.remotes.event :as event]
   [vimsical.vcs.delta :as delta]
   [vimsical.vims :as vims]))

;;
;; * Vims
;;

(defmethod event/event-spec  ::vims [_] (s/cat :id any? :vims-uid ::vims/uid))
(defmethod event/result-spec ::vims [_] (s/cat :id any? :vims ::vims/vims))

;;
;; * Deltas
;;
(s/def ::chan #?(:cljs (fn [_] false) :clj  (fn [x] (satisfies? ap/ReadPort x))))
(s/def ::deltas-result (s/or :frontend (s/every ::delta/delta) :backend ::chan))

(defmethod event/event-spec  ::deltas [_] (s/cat :id any? :vims-uid ::vims/uid))
(defmethod event/result-spec ::deltas [_] (s/cat :id any? :deltas ::deltas-result))
