(ns vimsical.user.settings
  (:require
   [clojure.spec :as s]
   [vimsical.user.settings.playback :as playback]))

(s/def ::uid uuid?)
(s/def :db/uid ::uid)
(s/def ::settings (s/keys :req [:db/uid ::playback/speed]))
