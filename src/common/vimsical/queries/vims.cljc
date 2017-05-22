(ns vimsical.queries.vims
  (:require
   [vimsical.vims :as vims]
   [vimsical.queries.branch :as branch]))

(def pull-query
  [:db/uid
   ::vims/title
   ::vims/cast
   {::vims/owner [:db/uid]}
   {::vims/branches branch/pull-query}])
