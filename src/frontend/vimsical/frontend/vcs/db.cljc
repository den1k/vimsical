(ns vimsical.frontend.vcs.db
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.vcs.core :as vcs]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.state.timeline :as timeline]
   [vimsical.frontend.util.mapgraph :as util.mg]))

(s/def ::branch-uid ::branch/uid)
(s/def ::delta-uid ::delta/prev-uid)
(s/def ::playhead-entry (s/nilable ::timeline/entry))
(s/def ::skimhead-entry (s/nilable ::timeline/entry))
(s/def ::state (s/keys :req [::branch-uid ::delta-uid] :opt [::playhead-entry ::skimhead-entry]))
(s/def ::vcs (s/merge ::vcs/vcs ::state ))

(s/fdef get-playhead-entry :args (s/cat :vcs ::vcs) :ret ::playhead-entry)
(defn get-playhead-entry [vcs] (get vcs ::playhead-entry))

(s/fdef set-playhead-entry :args (s/cat :vcs ::vcs :entry ::playhead-entry) :ret ::vcs)
(defn set-playhead-entry [vcs entry] (assoc vcs ::playhead-entry entry))

(s/fdef get-skimhead-entry :args (s/cat :vcs ::vcs)  :ret ::skimhead-entry)
(defn get-skimhead-entry [vcs] (get vcs ::skimhead-entry))

(s/fdef set-skimhead-entry :args (s/cat :vcs ::vcs :entry ::skimhead-entry) :ret ::vcs)
(defn set-skimhead-entry [vcs entry] (assoc vcs ::skimhead-entry entry))

;;
;; * VCS Normalization
;;

;; NOTE mg/add will recursively traverse a datastructure looking for entities,
;; eventhough the vcs is normalized in the db, this fn gets expensive as the vcs
;; gets large. So far the vcs only contains references for branches in
;; ::vcs/branches making it simple to manually normalize and assoc into the db

(defn- normalize
  [db vcs]
  (update vcs ::vcs/branches
          (fn [branches]
            (mapv (partial util.mg/->ref db) branches))))

(defn add
  [db vcs]
  (let [ref   (util.mg/->ref db vcs)
        normd (normalize db vcs)]
    (assoc db ref normd)))
