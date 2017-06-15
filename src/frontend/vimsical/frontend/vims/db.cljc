(ns vimsical.frontend.vims.db
  (:require
   [clojure.spec.alpha :as s]
   [com.stuartsierra.mapgraph :as mg]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.vims :as vims]
   [vimsical.vcs.delta :as delta]))

;;
;; * Remote state (transient)
;;

;; We use those keys to keep the deltas response (cassandra) around until we get
;; the vims response (datomic)

(s/def ::deltas (s/every ::delta/delta))

(defn- path [db vims-uid]
  [(util.mg/->ref db vims-uid) ::deltas])

(defn set-deltas [db vims-uid deltas]
  {:pre [vims-uid]}
  (assoc-in db (path db vims-uid) deltas))

(defn get-deltas [db vims-uid]
  {:pre [vims-uid]}
  (get-in db (path db vims-uid)))

;;
;; * Predicates
;;

(defn loaded?
  [db vims-ref-ent-or-uid]
  (let [ref                 (util.mg/->ref db vims-ref-ent-or-uid)
        {::vims/keys [vcs]} (mg/pull db [::vims/vcs] ref)]
    (some? vcs)))
