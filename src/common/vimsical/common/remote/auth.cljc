(ns vimsical.common.remote.auth
  (:require
   [clojure.spec :as s]))

;; ;; Auth
;; ;;; Mutations

;; :vimsical.common.remote.auth/login!
;; (s/def :vimsical.common.remote.auth.login!/request
;;   (s/keys [::user/email ::user/password]))

;; :vimsical.common.remote.auth/logout!
;; (s/def :vimsical.common.remote.auth.logout!/request empty?)

;; :vimsical.common.remote.auth/register!
;; (s/def :vimsical.common.remote.auth.register!/request
;;   (s/key [::user/email ::user/password ::user/first-name ::user/last-name]))

;; ;; User
;; ;;; Reads

;; :vimsical.common.remote/user
;; :vimsical.common.remote.user/previews


;; ;; Vims
;; ;;; Reads

;; :vimsical.common.remote/vims
;; :vimsical.common.remote.vims/preview


;; ;;; Mutations

;; :vimsical.common.remote.vims/new!
;; :vimsical.common.remote.vims/add-deltas!
;; :vimsical.common.remote.vims/set-title!
;; :vimsical.common.remote.vims/set-preview!
