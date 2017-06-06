(ns vimsical.frontend.vims.db
  (:require
   [clojure.spec :as s]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.vcs.delta :as delta]))

(s/def ::deltas (s/every ::delta/delta))

(defn- path [db vims-uid]
  [(util.mg/->ref db vims-uid) ::deltas])

(defn set-deltas [db vims-uid deltas]
  {:pre [vims-uid]}
  (assoc-in db (path db vims-uid) deltas))

(defn get-deltas [db vims-uid]
  {:pre [vims-uid]}
  (get-in db (path db vims-uid)))
