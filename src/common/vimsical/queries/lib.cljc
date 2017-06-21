(ns vimsical.queries.lib
  (:require
   [vimsical.vcs.lib :as lib]))

(def pull-query
  [:db/uid
   ::lib/src
   ::lib/type
   ::lib/sub-type
   ::lib/name
   ::lib/version])
