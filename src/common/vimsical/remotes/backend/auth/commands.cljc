(ns vimsical.remotes.backend.auth.commands
  (:require
   [clojure.spec :as s]
   [vimsical.remotes.event :as event]
   [vimsical.user :as user]))

;;
;; * Register
;;

(s/def ::register-user
  (s/keys :req [:db/uid
                ::user/first-name ::user/last-name
                ::user/email ::user/password]))

(defmethod event/event-spec ::register [_] (s/cat :id any? :user ::register-user))

;;
;; * Login
;;

(s/def ::login-user
  (s/keys :req [::user/email ::user/password]))

(defmethod event/event-spec ::login [_] (s/cat :id any? :user ::login-user))

;;
;; * Logout
;;

(defmethod event/event-spec ::logout [_] (s/cat :id any?))
