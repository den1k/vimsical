(ns vimsical.user.settings
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.user.settings.playback :as playback]))

(s/def ::uid uuid?)
(s/def :db/uid ::uid)
(s/def ::settings (s/keys :req [:db/uid ::playback/speed]))

(def pull-query
  [:db/uid ::playback/speed])
