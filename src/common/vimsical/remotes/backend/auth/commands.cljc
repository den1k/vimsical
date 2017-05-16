(ns vimsical.remotes.backend.auth.commands
  (:require
   [clojure.spec :as s]
   [vimsical.user :as user]))

(s/def ::login-args (s/keys :req  [::user/email ::user/password]))
(s/def ::logout-args empty?)
(s/def ::register-args (s/keys :req [::user/first-name ::user/last-name ::user/email ::user/password]))
