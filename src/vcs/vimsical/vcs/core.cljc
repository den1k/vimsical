(ns vimsical.vcs.core
  "* TODO

  - rename ::delta-uid to ::last-delta-uid

  Make it clear we're not tracking vcr state, only the bare minimum to be
  consistent, the vcr can add ::timeline-entry and use it as a sub without us
  knowing about it

  - remove set-delta

  - remove ::branch-uid and add branch-uid as an argument to add-edit-event

  same rationale

  - cleanup naming

  alias state.foo/bar to ::bar

  - branching

  should we make the client create and add a new branch before accepting edit
  events?
  "
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.edit-event :as edit-event]
   [vimsical.vcs.editor :as editor]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.state.deltas :as state.deltas]
   [vimsical.vcs.state.files :as state.files]
   [vimsical.vcs.state.timeline :as state.timeline]))

;;
;; * State
;;

(s/def ::branches (s/every ::branch/branch))
(s/def ::state (s/keys :req [::state.files/state-by-file-uid ::state.deltas/deltas]))
(s/def ::state-by-delta-uid (s/every-kv ::delta/prev-uid ::state))
(s/def ::vcs (s/keys :req [::branches ::timeline ::state-by-delta-uid]))

;;
;; * Updates
;;

;;
;; ** Initilization
;;

(s/fdef empty-vcs
        :args (s/cat :branches (s/every ::branch/branch))
        :ret ::vcs)

(defn empty-vcs
  [branches]
  (letfn [(master-branch [branches] (first (filter branch/master? branches)))]
    {::state-by-delta-uid {}
     ::branches           branches
     ::timeline           state.timeline/empty-timeline}))

;;
;; ** Reading (existing vims deltas')
;;

(s/fdef add-delta
        :args (s/cat :vcs ::vcs
                     :uuid-fn ::editor/uuid-fn
                     :delta ::delta/delta)
        :ret  ::vcs)

(defn add-delta
  [{:as vcs ::keys [branches state-by-delta-uid timeline]} uuid-fn {:keys [prev-uid branch-uid uid] :as delta}]
  (let [state                    (get state-by-delta-uid prev-uid)
        all-deltas               (get state ::state.deltas/deltas state.deltas/empty-deltas)
        all-deltas'              (state.deltas/add-delta all-deltas delta)
        files-state-by-file-uid  (get state ::state.files/state-by-file-uid state.files/empty-state-by-file-uid)
        files-state-by-file-uid' (state.files/add-delta files-state-by-file-uid delta)
        timeline'                (state.timeline/add-delta timeline branches uuid-fn delta)]
    (-> vcs
        (assoc-in [::state-by-delta-uid uid ::state.deltas/deltas] all-deltas')
        (assoc-in [::state-by-delta-uid uid ::state.files/state-by-file-uid] files-state-by-file-uid')
        (assoc ::timeline timeline'))))


;;
;; ** Writing (events from the editor)
;;

(s/fdef add-edit-event
        :args (s/cat :vcs ::vcs
                     :effects ::editor/effects
                     :file-uid ::file/uid
                     :branch-uid ::branch/uid
                     :delta-uid ::delta/prev-uid
                     :edit-event ::edit-event/edit-event)
        :ret (s/tuple ::vcs ::delta/uid))

(defn add-edit-event
  [{:as vcs ::keys [branches state-by-delta-uid timeline]}
   {:as effects ::editor/keys [uuid-fn]}
   file-uid
   branch-uid
   delta-uid
   edit-event]
  (let [state                   (get state-by-delta-uid delta-uid)
        all-deltas              (get state ::state.deltas/deltas state.deltas/empty-deltas)
        files-state-by-file-uid (get state ::state.files/state-by-file-uid state.files/empty-state-by-file-uid)
        [files-state-by-file-uid'
         deltas'
         delta-uid']            (state.files/add-edit-event files-state-by-file-uid effects file-uid branch-uid delta-uid edit-event)
        all-deltas'             (state.deltas/add-deltas all-deltas deltas')
        timeline'               (state.timeline/add-deltas timeline branches uuid-fn deltas')
        vcs'                    (-> vcs
                                    (assoc-in [::state-by-delta-uid delta-uid' ::state.deltas/deltas] all-deltas')
                                    (assoc-in [::state-by-delta-uid delta-uid' ::state.files/state-by-file-uid] files-state-by-file-uid')
                                    (assoc ::timeline timeline'))]
    [vcs' delta-uid']))

;;
;; * Queries
;;

;;
;; ** Deltas
;;

(defn- delta-uid->state
  [vcs delta-uid]
  (get-in vcs [::state-by-delta-uid delta-uid]))

(defn- delta-uid->state-by-file-uid
  [vcs delta-uid]
  (get-in vcs [::state-by-delta-uid delta-uid ::state.files/state-by-file-uid]))

(defn deltas
  [vcs delta-uid]
  (some-> vcs (delta-uid->state delta-uid) ::state.deltas/deltas))

;;
;; * Files
;;

(s/fdef file-deltas
        :args (s/cat :vcs ::vcs :file-uid ::file/uid :delta-uid ::delta/prev-uid)
        :ret  (s/every ::delta/delta))

(defn file-deltas
  [vcs file-uid delta-uid]
  (some-> vcs
          (delta-uid->state-by-file-uid delta-uid)
          (state.files/deltas file-uid)))

(s/fdef file-string
        :args (s/cat :vcs ::vcs :file-uid ::file/uid :delta-uid ::delta/prev-uid)
        :ret  (s/nilable string?))

(defn file-string
  [vcs file-uid delta-uid]
  (some-> vcs
          (delta-uid->state-by-file-uid delta-uid)
          (state.files/string file-uid)))

(s/fdef file-cursor
        :args (s/cat :vcs ::vcs :file-uid ::file/uid :delta-uid ::delta/prev-uid)
        :ret  (s/nilable nat-int?))

(defn file-cursor
  [vcs file-uid delta-uid]
  (some-> vcs
          (delta-uid->state-by-file-uid delta-uid)
          (state.files/cursor file-uid)))

;;
;; * Timeline
;;

(s/fdef timeline-duration
        :args (s/cat :vcs ::vcs)
        :ret  ::state.timeline/duration)

(defn timeline-duration [{::keys [timeline]}]
  (state.timeline/duration timeline))

(s/fdef timeline-delta-at-time
        :args (s/cat :vcs ::vcs :time ::state.timeline/absolute-time)
        :ret  (s/nilable ::delta/delta))

(defn timeline-delta-at-time [{::keys [timeline]} time]
  (state.timeline/delta-at-absolute-time timeline time))

(s/fdef timeline-entry-at-time
        :args (s/cat :vcs ::vcs :time ::state.timeline/absolute-time)
        :ret  ::state.timeline/entry)

(defn timeline-entry-at-time [{::keys [timeline]} time]
  (state.timeline/entry-at-absolute-time timeline time))

(s/fdef timeline-chunks-by-absolute-start-time
        :args (s/cat :vcs ::vcs)
        :ret  ::state.timeline/chunks-by-absolute-start-time)

(defn timeline-chunks-by-absolute-start-time [{::keys [timeline]}]
  (state.timeline/chunks-by-absolute-start-time timeline))

(s/fdef timeline-first-entry
        :args (s/cat :vcs ::vcs)
        :ret  (s/nilable ::state.timeline/entry))

(defn timeline-first-entry [{::keys [timeline]}] (state.timeline/first-entry timeline))

(s/fdef timeline-next-entry
        :args (s/cat :vcs ::vcs :entry (s/nilable ::state.timeline/entry))
        :ret  (s/nilable ::state.timeline/entry))

(defn timeline-next-entry [{::keys [timeline]} entry] (state.timeline/next-entry timeline entry))
