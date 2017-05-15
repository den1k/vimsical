(ns vimsical.user.settings
  (:require
   [clojure.spec :as s]
   [vimsical.user.settings.playback :as playback]))

(s/def ::settings (s/keys :req [::playback/speed]))
