(ns vimsical.queries.owner
  (:require [vimsical.user :as user]))

(def pull-query
  [:db/uid
   ::user/first-name
   ::user/last-name
   ::user/email])
