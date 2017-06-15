(ns vimsical.remotes.backend.auth.queries
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.remotes.event :as event]
   [vimsical.user :as user]))

;;
;; * Invite
;;

(s/def ::invite-user (s/keys :req [:db/uid ::user/first-name ::user/last-name ::user/token]))

(defmethod event/event-spec  ::invite [_] (s/cat :id any? :token string?))
(defmethod event/result-spec ::invite [_] (s/cat :id any? :user ::invite-user))
