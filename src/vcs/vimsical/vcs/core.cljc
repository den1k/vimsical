(ns vimsical.vcs.core
  "* TODO

  - constant time next-delta look up for playback scheduling
  - add branch/start end to chunks using branch-pointers
  - update chunks inside branches

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
   [vimsical.vcs.state.deltas :as state.deltas]
   [vimsical.vcs.state.files :as state.files]
   [vimsical.vcs.state.timeline :as state.timeline]))

;; * State

;; Pointers
(s/def ::branch-id ::branch/id)
(s/def ::delta-id ::delta/prev-id)

;; State
(s/def ::branches (s/every ::branch/branch))
(s/def ::state
  (s/keys :req [::state.files/state-by-file-id
                ::state.branch-pointers/branch-pointers-by-branch-id
                ::state.branches/deltas-by-branch-id
                ::state.deltas/deltas]))

;; State indexed by delta id
(s/def ::state-by-delta-id
  (s/every-kv ::delta-id ::state))

;; Top-level
(s/def ::vcs
  (s/keys
   :req [::branches ::timeline ::state-by-delta-id]
   :opt [::branch-id ::delta-id]))


;; * Updates
;; ** Initilization

(s/fdef empty-vcs
        :args (s/cat :branches (s/every ::branch/branch))
        :ret ::vcs)

(defn empty-vcs
  [branches]
  (letfn [(master-branch [branches] (first (filter branch/master? branches)))]
    {::branch-id         (:db/id (master-branch branches))
     ::delta-id          nil
     ::state-by-delta-id {}
     ::branches          branches
     ::timeline          state.timeline/empty-timeline}))


;; ** Reading (existing vims deltas')

(defn add-delta
  [{:as vcs ::keys [branches branch-id delta-id state-by-delta-id timeline]} uuid-fn {:keys [id] :as delta}]
  (let [state                         (get state-by-delta-id delta-id)
        files-state-by-file-id        (get state ::state.files/state-by-file-id state.files/empty-state-by-file-id)
        deltas-by-branch-id           (get state ::state.branches/deltas-by-branch-id state.branches/empty-deltas-by-branch-id)
        branch-pointers-by-branch-id  (get state ::state.branch-pointers/branch-pointers-by-branch-id state.branch-pointers/empty-branch-pointers-by-branch-id)
        files-state-by-file-id'       (state.files/add-delta files-state-by-file-id delta)
        deltas-by-branch-id'          (state.branches/add-delta deltas-by-branch-id delta)
        branch-pointers-by-branch-id' (state.branch-pointers/add-delta branch-pointers-by-branch-id delta)
        timeline'                     (state.timeline/add-delta timeline deltas-by-branch-id' branches uuid-fn delta)]
    (-> vcs
        (assoc-in [::state-by-delta-id id ::state.files/state-by-file-id] files-state-by-file-id')
        (assoc-in [::state-by-delta-id id ::state.branch-pointers/branch-pointers-by-branch-id] branch-pointers-by-branch-id')
        (assoc-in [::state-by-delta-id id ::state.branches/deltas-by-branch-id] deltas-by-branch-id')
        (assoc ::delta-id id ::timeline timeline'))))


;; ** Writing (events from the editor)

(s/fdef add-edit-event
        :args (s/cat :vcs ::vcs :effects ::editor/effects :file-id ::file/id :edit-event ::edit-event/edit-event)
        :ret ::vcs)

(defn add-edit-event
  [{:as vcs ::keys [branches branch-id delta-id state-by-delta-id timeline]}
   {:as effects ::editor/keys [uuid-fn]}
   file-id
   edit-event]
  (let [state                                       (get state-by-delta-id delta-id)
        all-deltas                                  (get state ::state.deltas/deltas state.deltas/empty-deltas)
        files-state-by-file-id                      (get state ::state.files/state-by-file-id state.files/empty-state-by-file-id)
        deltas-by-branch-id                         (get state ::state.branches/deltas-by-branch-id state.branches/empty-deltas-by-branch-id)
        branch-pointers-by-branch-id                (get state ::state.branch-pointers/branch-pointers-by-branch-id state.branch-pointers/empty-branch-pointers-by-branch-id)
        [files-state-by-file-id' deltas' delta-id'] (state.files/add-edit-event files-state-by-file-id effects file-id branch-id delta-id edit-event)
        all-deltas'                                 (state.deltas/add-deltas all-deltas deltas')
        deltas-by-branch-id'                        (state.branches/add-deltas deltas-by-branch-id deltas')
        branch-pointers-by-branch-id'               (state.branch-pointers/add-deltas branch-pointers-by-branch-id deltas')
        timeline'                                   (state.timeline/add-deltas timeline deltas-by-branch-id' branches uuid-fn deltas')]
    (-> vcs
        (assoc-in [::state-by-delta-id delta-id' ::state.deltas/deltas] all-deltas')
        (assoc-in [::state-by-delta-id delta-id' ::state.files/state-by-file-id] files-state-by-file-id')
        (assoc-in [::state-by-delta-id delta-id' ::state.branch-pointers/branch-pointers-by-branch-id] branch-pointers-by-branch-id')
        (assoc-in [::state-by-delta-id delta-id' ::state.branches/deltas-by-branch-id] deltas-by-branch-id')
        (assoc ::delta-id delta-id' ::timeline timeline'))))


;; * Queries

;; TODO move the accessors to state.files

(defn file-deltas
  ([{::keys [delta-id] :as vcs} file-id] (file-deltas vcs file-id delta-id))
  ([vcs file-id delta-id]
   (get-in vcs [::state-by-delta-id delta-id ::state.files/state-by-file-id file-id ::state.files/deltas])))

(defn file-string
  ([{::keys [delta-id] :as vcs} file-id] (file-string vcs file-id delta-id))
  ([vcs file-id delta-id]
   (get-in vcs [::state-by-delta-id delta-id ::state.files/state-by-file-id file-id ::state.files/string])))

(defn file-cursor
  ([{::keys [delta-id] :as vcs} file-id] (file-cursor vcs file-id delta-id))
  ([vcs file-id delta-id]
   (get-in vcs [::state-by-delta-id delta-id ::state.files/state-by-file-id file-id ::state.files/cursor])))

(defn timeline-delta-at-time [{::keys [timeline]} time]
  (state.timeline/delta-at-absolute-time timeline time))

(defn timeline-duration [{::keys [timeline]}]
  (state.timeline/duration timeline))
