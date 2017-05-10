(ns vimsical.vcs.state.deltas
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.alg.topo :as topo]
   [vimsical.vcs.delta :as delta]))

(s/def ::deltas
  (s/and (s/every ::delta/delta :kind vector?) topo/sorted? delta/ops-point-to-str-id?))

(def empty-deltas [])

(defn add-delta
  [deltas delta]
  (conj deltas delta))

(defn add-deltas
  [deltas deltas']
  (into deltas deltas'))
