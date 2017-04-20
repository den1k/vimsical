(ns vimsical.vcs.state.files
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.data.splittable :as splittable]
   [vimsical.vcs.data.indexed.vector :as indexed]
   [vimsical.vcs.edit-event :as edit-event]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.branch :as branch]))


;; * Spec
;; ** Singe file state

(s/def ::idx nat-int?)
(s/def ::deltas (s/and ::indexed/vector (s/every ::delta/delta)))
(s/def ::string string?)
(s/def ::state (s/keys :req [::deltas ::string]))

(def ^:private empty-state {::deltas (indexed/vector-by :id) ::string ""})


;; ** Files' states by delta id

(s/def ::states (s/map-of ::delta/id (s/map-of ::file/id ::state)))

(def empty-states {})


;; * Internal
;; ** Helpers

;; TODO DRY updates so we can more easily switch the underlying data structures

;; (s/fdef update-state
;;         :args (s/cat :state ::state :op ::delta/op))

;; (defmulti update-state
;;   (fn [state [op-type]] op-type))


;; ** Player API Internals -- adding existing deltas

(s/fdef op-id->op-idx
        :args (s/cat :state ::state :op-id ::delta/prev-id)
        :ret  ::idx)

(defn- op-id->op-idx
  [{::keys [deltas] :as state} op-id]
  (or (cond
        ;; First delta
        (nil? op-id)    0
        ;; Moved to a new file
        (empty? deltas) 0
        ;;
        :else           (indexed/index-of deltas op-id))
      (throw
       (ex-info
        "Id not found"
        {:op-id op-id :deltas deltas}))))

(s/fdef add-delta-rf
        :args (s/cat :state ::state :delta ::delta/delta)
        :ret  ::state)

(defmulti ^:private add-delta-rf
  (fn [state delta]
    (-> delta :op first)))

(defmethod add-delta-rf :crsr/mv [state delta] state)

(defmethod add-delta-rf :str/ins
  [state {:keys [id prev-id] :as delta}]
  (if (nil? prev-id)
    (assoc state
           ::deltas (indexed/vec-by :id [delta])
           ::string (delta/op-diff delta))
    (let [op-id      (delta/op-id delta)
          op-diff    (delta/op-diff delta)
          op-idx     (op-id->op-idx state op-id)
          ;; TODO document that the idx position is like a caret that sits left
          ;; of the character while still pointing "at it". In order to inser at
          ;; that character we need to skip over it.
          op-idx-ins (inc op-idx)]
      (-> state
          (update ::deltas splittable/splice op-idx-ins (indexed/vec-by :id [delta]))
          (update ::string splittable/splice op-idx-ins op-diff)))))

(defmethod add-delta-rf :str/rem
  [{::keys [deltas string] :as state} delta]
  (let [op-id      (delta/op-id delta)
        op-amt     (delta/op-amt delta)
        op-idx     (op-id->op-idx state op-id)]
    (-> state
        (update ::deltas splittable/omit op-idx op-amt)
        (update ::string splittable/omit op-idx op-amt))))


;; ** Editor API internals -- adding edit-events
;; *** Event splicing -- 1 edit-event -> * edit-events

(defmulti ^:private splice-edit-event ::edit-event/op)

(defmethod splice-edit-event :default [e] [e])

(defmethod splice-edit-event :str/ins
  [{::edit-event/keys [op idx diff]}]
  (let [idxs  (range idx (+ idx (count diff)))
        chars (seq diff)]
    (mapv
     (fn splice-edit-event-str-ins-rf
       [[idx char]]
       {::edit-event/op   op
        ::edit-event/idx  idx
        ::edit-event/diff (str char)})
     (map vector idxs chars))))

(defmethod splice-edit-event :str/rem
  [{::edit-event/keys [op idx amt] :as evt}]
  (mapv
   (fn splice-edit-event-str-rem-rf
     [idx]
     {::edit-event/op  op
      ::edit-event/idx idx
      ::edit-event/amt 1})
   ;; We want to delete the characters backwards so that the indexes of the
   ;; deltas that remain to be deleted do not change in between the application
   ;; of our operation
   (reverse (range idx (+ amt idx)))))

(s/fdef op-idx->op-id
        :args (s/cat :state ::state :op-idx ::idx)
        :ret ::delta/prev-id)

(defn- op-idx->op-id
  [{::keys [deltas] :as state} op-idx]
  (let [idx (dec op-idx)]
    (try
      (println {:idx idx :deltas (count deltas)})
      (when (<= 0 idx)
        (:id (nth deltas idx)))
      (catch Throwable t
        ;; (throw
        ;;  (ex-info
        ;;   "op-idx->op-id: Index not found"
        ;;   {:op-idx op-idx :detlas deltas}))
        ))))

(s/def ::uuid-fn ifn?)
(s/def ::pad-fn ifn?)
(s/def ::timestamp-fn ifn?)
(s/def ::editor-effects (s/keys :req [::uuid-fn ::pad-fn ::timestamp-fn]))
(s/def ::delta-id ::delta/prev-id)
(s/def ::file-id ::file/id)
(s/def ::branch-id ::branch/id)
(s/def ::editor-state (s/keys :req [::delta-id ::file-id ::branch-id]))
(s/def ::current-delta-id ::delta/id)   ; can't be nil
(s/def ::add-edit-event-rf-ret-state (s/tuple ::state ::current-delta-id))

(s/fdef add-edit-event-rf
        :args (s/cat :state ::add-edit-event-rf-arg-state
                     :editor-state ::editor-state
                     :editor-effects ::editor-effects
                     :edit-event ::edit-event/edit-event)
        :ret ::add-edit-event-rf-ret-state)

(defmulti ^:private add-edit-event-rf
  (fn [state editor-state editor-effects edit-event]
    (-> edit-event ::edit-event/op)))

(defmethod add-edit-event-rf :str/ins
  [state
   {:as editor-state ::keys [file-id branch-id delta-id]}
   {:as editor-effects ::keys [pad-fn uuid-fn timestamp-fn]}
   edit-event]
  (reduce
   (fn add-edit-event-str-ins-rf
     [[state current-delta-id] {:as edit-event ::edit-event/keys [idx diff]}]
     (let [op-id        (op-idx->op-id state idx)
           op           [:str/ins op-id diff]
           new-delta-id (uuid-fn edit-event)
           pad          (pad-fn edit-event)
           timestamp    (timestamp-fn edit-event)
           delta        (delta/new-delta
                         {:branch-id branch-id
                          :file-id   file-id
                          :prev-id   current-delta-id
                          :id        new-delta-id
                          :op        op
                          :pad       pad
                          :timestamp timestamp})
           state'       (if (nil? current-delta-id)
                          (assoc state
                                 ::deltas (indexed/vec-by :id [delta])
                                 ::string diff)
                          (-> state
                              (update ::deltas splittable/splice idx (indexed/vec-by :id [delta]))
                              (update ::string splittable/splice idx diff)))]
       [state' new-delta-id]))
   ;; Start with the delta-id provided by the editor as the current id, if we
   ;; insert multiple deltas, the reduction needs to ensure updating this value
   ;; with the latest id generated by the uuid-fn
   [state delta-id] (splice-edit-event edit-event)))

(defmethod add-edit-event-rf :str/rem
  [state
   {:as editor-state ::keys [file-id branch-id delta-id]}
   {:as editor-effects ::keys [pad-fn uuid-fn timestamp-fn]}
   edit-event]
  #dbg
  (reduce
   (fn add-edit-event-rf-str-rem-rf
     [[state current-delta-id] {:as edit-event ::edit-event/keys [idx amt]}]
     (let [op-id        (op-idx->op-id state idx)
           op           [:str/rem op-id amt]
           new-delta-id (uuid-fn edit-event)
           pad          (pad-fn edit-event)
           timestamp    (timestamp-fn edit-event)
           delta        (delta/new-delta
                         {:branch-id branch-id
                          :file-id   file-id
                          :prev-id   current-delta-id
                          :id        new-delta-id
                          :op        op
                          :pad       pad
                          :timestamp timestamp})
           state'       (-> state
                            (update ::deltas splittable/omit idx amt)
                            (update ::string splittable/omit idx amt))]
       [state' new-delta-id]))
   [state delta-id] (splice-edit-event edit-event)))

(defmethod add-edit-event-rf :crsr/mv
  [state
   {:as editor-state ::keys [file-id branch-id delta-id]}
   {:as editor-effects ::keys [pad-fn uuid-fn timestamp-fn]}
   {:as edit-event ::edit-event/keys [idx]}]
  [state (op-idx->op-id state idx)])


;; * Public API

;; ** File(s) state -- retrieve the state of file(s) at a specific delta id

(s/fdef get-file-state
        :args (s/cat :states ::states :delta-id ::delta/id :file-id ::file/id)
        :ret ::state)

(defn get-file-state
  [states delta-id file-id]
  (get-in states [delta-id file-id]))

(s/fdef get-file-deltas
        :args (s/cat :states ::states :delta-id ::delta/id :file-id ::file/id)
        :ret ::deltas)

(defn get-file-deltas
  [states delta-id file-id]
  (get-in states [delta-id file-id ::deltas]))

(s/fdef get-file-string
        :args (s/cat :states ::states :delta-id ::delta/id :file-id ::file/id)
        :ret ::string)

(defn get-file-string
  [states delta-id file-id]
  (get-in states [delta-id file-id ::string]))

(s/fdef get-files-states
        :args (s/cat :states ::states :delta-id ::delta/id)
        :ret  (s/map-of ::file/id ::state))

(defn get-files-states
  [states delta-id]
  (get states delta-id))

(s/fdef get-files-deltas
        :args (s/cat :states ::states :delta-id ::delta/id)
        :ret  (s/map-of ::file/id ::deltas))

(defn get-files-deltas
  [states delta-id]
  (reduce-kv
   (fn [m file-id {::keys [deltas]}]
     (assoc m file-id deltas))
   {} (get states delta-id)))

(s/fdef get-files-strings
        :args (s/cat :states ::states :delta-id ::delta/id)
        :ret  (s/map-of ::file/id ::string))

(defn get-files-strings
  [states delta-id]
  (reduce-kv
   (fn [m file-id {::keys [string]}]
     (assoc m file-id string))
   {} (get states delta-id)))


;; ** Updates

(defn- wrap-update-debug
  [f]
  (fn [states file-id from-delta-id to-delta-id-fn ff & args]
    (println "----------------------------------------------------------------------")
    (println f)
    (println {:bef {:arg (last args) :states states}})
    (let [ret (apply f states file-id from-delta-id to-delta-id-fn ff args)]
      (println {:aft {:arg (last args) :states ret}})
      ret)))


;; *** Adding deltas -- reading a vims

(s/fdef add-delta
        :args (s/cat :states ::states :delta ::delta/delta)
        :ret ::states)

(defn add-delta
  [states {:keys [file-id prev-id id] :as delta}]
  ;; NOTE don' get/update/assoc-IN, because we want to grab all the other files
  ;; that exist for the previous delta though we only update the given file-id
  (let [state-by-file-id  (get states prev-id {file-id empty-state})
        rf                (fnil add-delta-rf empty-state)
        state-by-file-id' (update state-by-file-id file-id rf delta)]
    (assoc states id state-by-file-id')))

(s/fdef add-deltas
        :args (s/cat :states ::states :delta (s/every ::delta/delta))
        :ret ::states)

(defn add-deltas [states deltas] (reduce add-delta states deltas))


;; *** Adding editing events -- editing a vims

(def ^:private add-edit-event-state-update-fn (fnil add-edit-event-rf empty-state))

(s/fdef add-edit-event
        :args (s/cat :states ::states
                     :editor-state ::editor-state
                     :editor-effects ::editor-effects
                     :edit-event ::edit-event/edit-event)
        :ret (s/tuple ::states ::current-delta-id))

(defn add-edit-event
  "Update `states` by adding the given `edit-event`. The `editor-state` should
  contain the pointers the editor used to retrieve its files' states at the time
  the `edit-event` was created.

  Return a map of `::states` with the updated `states` and `::current-delta-id`
  a value for the latest delta id that was created as part of the processing of
  `edit-event`.

  Note that this fn handles splicing `edit-event` into multiple deltas when
  required."
  [states {:as editor-state ::keys [file-id delta-id]} editor-effects edit-event]
  (let [state-by-file-id   (get states delta-id {file-id empty-state})
        state              (get state-by-file-id file-id)
        rf                 (fnil add-edit-event-rf empty-state)
        [state' delta-id'] (rf state editor-state editor-effects edit-event)
        state-by-file-id'  (assoc state-by-file-id file-id state')
        states'            (assoc states delta-id' state-by-file-id')]
    [states' delta-id']))

(s/fdef add-edit-events
        :args (s/cat :states ::states
                     :editor-state ::editor-state
                     :editor-effects ::editor-effects
                     :edit-events (s/every ::edit-event/edit-event))
        :ret (s/tuple ::states ::current-delta-id))

(defn add-edit-events
  [states {:as editor-state ::keys [delta-id]} editor-effects edit-events]
  (reduce
   (fn add-edit-events-rf
     [[states current-delta-id] edit-event]
     (let [editor-state' (assoc editor-state ::delta-id current-delta-id)]
       (add-edit-event states editor-state' editor-effects edit-event)))
   [states delta-id] edit-events))
