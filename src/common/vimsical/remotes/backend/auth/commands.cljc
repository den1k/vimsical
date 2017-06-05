(ns vimsical.remotes.backend.auth.commands
  (:require
   [clojure.spec :as s]
   [vimsical.remotes.event :as event]
   [vimsical.user :as user]))

;;
;; * Signup
;;

(s/def ::signup-user (s/keys :req [:db/uid ::user/first-name ::user/last-name ::user/email ::user/password]))

(defmethod event/event-spec  ::signup [_] (s/cat :id any? :user ::signup-user))
(defmethod event/result-spec ::signup [_] (s/cat :id any? :user ::user/user))

;;
;; * Login
;;

(s/def ::login-user (s/keys :req [::user/email ::user/password]))

(defmethod event/event-spec  ::login [_] (s/cat :id any? :user ::login-user))
(defmethod event/result-spec ::login [_] (s/cat :id any? :user ::user/user))

;;
;; * Logout
;;

(defmethod event/event-spec  ::logout [_] (s/cat :id any?))
(defmethod event/result-spec ::logout [_] (s/cat :id any?))
