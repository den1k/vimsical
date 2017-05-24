(ns vimsical.user
  (:require
   [clojure.spec :as s]
   [vimsical.vims :as vims]
   [vimsical.user.settings :as settings]))

(s/def ::uid uuid?)
(s/def :db/uid ::uid)
(s/def ::first-name string?)
(s/def ::last-name string?)
(s/def ::email string?)
(s/def ::password string?)
(s/def ::vimsae (s/every ::vims/vims))
(s/def ::settings (s/every ::settings/settings))

(s/def ::user
  (s/keys :opt [:db/uid
                ::first-name
                ::last-name
                ::email
                ::password
                ::vimsae
                ::settings]))
