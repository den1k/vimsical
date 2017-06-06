(ns vimsical.remotes.backend.user.queries
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.remotes.event :as event]))

;;
;; * Me (app user)
;;

(defmethod event/event-spec ::me [_] (s/cat :id any?))
