(ns vimsical.vcs.state.deltas
  (:require
   [taoensso.tufte :as tufte :refer (defnp p profiled profile)]
   [clojure.spec.alpha :as s]
   [vimsical.vcs.alg.topo :as topo]
   [vimsical.vcs.delta :as delta]))

(s/def ::deltas
  (s/and (s/every ::delta/delta :kind vector?) topo/sorted? delta/ops-point-to-str-id?))

(def empty-deltas [])

(defnp add-delta
  [deltas delta]
  (conj deltas delta))

(defnp add-deltas
  [deltas deltas']
  (into deltas deltas'))
