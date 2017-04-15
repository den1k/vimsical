(ns vimsical.vcs.state.vims.files.latest
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.data.indexed.vector :as indexed.vector]
   [vimsical.common.core :refer [some-val]]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.file :as file]))

(s/def ::latest
  (s/map-of ::branch/id (s/map-of ::file/id ::delta/delta)))

(defn add-deltas
  [delta-index deltas]
  (reduce
   (fn [delta-index {:keys [file-id branch-id] :as delta}]
     (assoc-in delta-index [branch-id file-id] delta))
   delta-index deltas))

(defn get-deltas
  [delta-index branch-lineage file-id]
  (some-val
   (fn [{:keys [db/id] :as branch}]
     (get-in delta-index [id file-id]))
   branch-lineage))
