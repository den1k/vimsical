(ns vimsical.vcs.core
  "* TODO

  - branching

  should we make the client create and add a new branch before accepting edit
  events?

  * NOTES

  - The delta-id tracked in the state corresponds to a str delta id: when
  adding edit events it is the id of the last delat that point to a string
  operation (in other words, a delta that makes up the file). The state would
  invalid if that id pointed to a crsr delta."
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.edit-event :as edit-event]
   [vimsical.vcs.editor :as editor]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.state.branch-pointers :as state.branch-pointers]
   [vimsical.vcs.state.branches :as state.branches]
   [vimsical.vcs.state.files :as state.files]
   [vimsical.vcs.state.timeline :as state.timeline]))

;; * State

;; Pointers
(s/def ::branch-id ::branch/id)
(s/def ::delta-id ::delta/prev-id)

;; State
(s/def ::branches (s/every ::branch/branch))
(s/def ::state (s/keys :req [::state.files/state-by-file-id]))

;; State indexed by delta id
(s/def ::state-by-delta-id (s/every-kv ::delta-id ::state))

;; Top-level
(s/def ::vcs (s/keys :opt [::branch-id ::delta-id
                           ::timeline
                           ::state.branch-pointers/branch-pointers-by-branch-id
                           ::state.branches/deltas-by-branch-id
                           ::state-by-delta-id]))

(defn empty-vcs
  [branches]
  (letfn [(master-branch [branches] (first (filter branch/master? branches)))]
    {::branch-id         (:db/id (master-branch branches))
     ::delta-id          nil
     ::state-by-delta-id {}
     ::branches          branches
     ::timeline          state.timeline/empty-timeline}))


;; * Internal

;; * API

;; ** State accessors

;; *** Deltas

(defn delta-at-time
  [{::keys [timeline] :as vcs} time]
  (state.timeline/delta-at-time timeline time))


;; *** Files

(defn file-string
  ([{::keys [delta-id] :as vcs} file-id] (file-string vcs file-id delta-id))
  ([vcs file-id delta-id]
   (get-in vcs [::state-by-delta-id delta-id ::state.files/state-by-file-id file-id ::state.files/string])))


;; *** Timeline

(defn timeline-duration
  [{::keys [timeline] :as vcs}]
  (get timeline ::state.timeline/duration 0))


;; ** Reading vims

(defn add-delta  [vcs delta])

(defn add-deltas [vcs deltas]
  (reduce add-delta vcs deltas))


;; * Writing vims

(s/fdef add-edit-event
        :args (s/cat :vcs ::vcs :effects ::editor/effects :file-id ::file/id :edit-event ::edit-event/edit-event)
        :ret ::vcs)

(defn add-edit-event
  [{:as vcs ::keys [branches branch-id delta-id state-by-delta-id timeline]} effects file-id edit-event]
  (let [state                                       (get state-by-delta-id delta-id)
        files-state-by-file-id                      (get state ::state.files/state-by-file-id state.files/empty-state-by-file-id)
        deltas-by-branch-id                         (get state ::state.branches/deltas-by-branch-id state.branches/empty-deltas-by-branch-id)
        branch-pointers-by-branch-id                (get state ::state.branch-pointers/branch-pointers-by-branch-id state.branch-pointers/empty-branch-pointers-by-branch-id)
        [files-state-by-file-id' deltas' delta-id'] (state.files/add-edit-event files-state-by-file-id effects file-id branch-id delta-id edit-event)
        deltas-by-branch-id'                        (state.branches/add-deltas deltas-by-branch-id deltas')
        branch-pointers-by-branch-id'               (state.branch-pointers/add-deltas branch-pointers-by-branch-id deltas')
        timeline'                                   (state.timeline/add-deltas timeline deltas-by-branch-id' branches deltas')]
    (-> vcs
        (assoc-in [::state-by-delta-id delta-id' ::state.files/state-by-file-id] files-state-by-file-id')
        (assoc-in [::state-by-delta-id delta-id' ::state.branch-pointers/branch-pointers-by-branch-id] branch-pointers-by-branch-id')
        (assoc-in [::state-by-delta-id delta-id' ::state.branches/deltas-by-branch-id] deltas-by-branch-id')
        (assoc ::delta-id delta-id' ::timeline timeline'))))
