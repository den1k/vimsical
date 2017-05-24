(ns vimsical.queries.settings
  (:require
   [vimsical.user.settings :as settings]))

(def pull-query
  [:db/uid
   ::settings/default-playback-speed])

(def datomic-pull-query pull-query)
