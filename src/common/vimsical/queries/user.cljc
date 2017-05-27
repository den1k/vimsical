(ns vimsical.queries.user
  (:require
   [vimsical.queries.settings :as settings]
   [vimsical.queries.vims :as vims]
   [vimsical.user :as user]))

(def pull-query
  [:db/uid
   ::user/first-name
   ::user/last-name
   ::user/email
   ::user/password
   {::user/vimsae vims/pull-query}
   {::user/settings settings/pull-query}])

(def frontend-pull-query
  [:db/uid
   ::user/first-name
   ::user/last-name
   ::user/email
   ::user/password
   {::user/vimsae vims/frontend-pull-query}
   {::user/settings settings/pull-query}])

(def datomic-pull-query
  [:db/uid
   ::user/first-name
   ::user/last-name
   ::user/email
   ::user/password
   {::user/vimsae vims/datomic-pull-query}
   {::user/settings settings/datomic-pull-query}])
