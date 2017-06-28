(ns vimsical.user
  (:require
   [clojure.spec.alpha :as s]
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
(s/def ::token string?)

(s/def ::user
  (s/keys :opt [:db/uid
                ::first-name
                ::last-name
                ::email
                ::password
                ::vimsae
                ::settings]))

(defn anon? [{::keys [email]}] (nil? email))

(def logged-in? (complement anon?))

(defn full-name [{::keys [first-name middle-name last-name]}]
  (str first-name " " (some-> middle-name (str " ")) last-name))
