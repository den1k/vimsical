(ns vimsical.vcs.state.files
  "Keep track of the deltas, string and cursor position for files."
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.editor :as editor]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.data.indexed.vector :as indexed]
   [vimsical.vcs.data.splittable :as splittable]
   [vimsical.vcs.alg.topo :as topo]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.edit-event :as edit-event]
   [vimsical.vcs.file :as file]))

;; * Spec
;; ** Singe file state

(s/def ::idx nat-int?)
(s/def ::idx-range (s/tuple ::idx ::idx))
(s/def ::amt pos-int?)
(s/def ::cursor (s/or :idx ::idx :idx-range ::idx-range))
(s/def ::deltas (s/and (s/every ::delta/delta) ::indexed/vector))
(s/def ::string string?)
(s/def ::state (s/keys :req [::deltas ::string ::cursor]))
(s/def ::new-deltas (s/and (s/every ::delta/delta) topo/sorted?))

(def ^:private empty-state {::deltas (indexed/vector-by :id) ::string "" ::cursor 0})


;; ** State by file id

(s/def ::state-by-file-id (s/every-kv ::file/id ::state))

(def empty-state-by-file-id {})


;; * Internal
;; ** Helpers

;; *** State update

;; XXX This op is hacky because it reuses the format of the delta/op but with
;; indexes instead of uuid, this datatype is not valid anywhere else in the vcs
;; and is only meant to be used internally
(defmulti ^:private update-state-op-spec first)
(defmethod update-state-op-spec :str/ins  [_] (s/tuple #{:str/ins} ::idx string?))
(defmethod update-state-op-spec :str/rem  [_] (s/tuple #{:str/rem} ::idx ::amt))
(defmethod update-state-op-spec :crsr/mv  [_] (s/tuple #{:crsr/mv} ::idx))
(defmethod update-state-op-spec :crsr/sel [_] (s/tuple #{:crsr/sel} ::idx-range))

(s/def ::update-op (s/multi-spec update-state-op-spec first))

(s/fdef update-state
        :args (s/cat :state ::state :op ::update-op :delta ::delta/delta)
        :ret ::state)

(defmulti ^:private update-state
  (fn [state [op-type] delta] op-type))

(defmethod update-state :str/ins
  [state  [_ idx diff] {:keys [prev-id] :as delta}]
  (if (nil? prev-id)
    (assoc state ::deltas (indexed/vec-by :id [delta]) ::string diff)
    (-> state
        (update ::deltas splittable/splice idx (indexed/vec-by :id [delta]))
        (update ::string splittable/splice idx diff))))

(defmethod update-state :str/rem
  [state  [_ idx amt] _]
  (-> state
      (update ::deltas splittable/omit idx amt)
      (update ::string splittable/omit idx amt)))

(defmethod update-state :crsr/mv  [state  [_ idx] _]       (assoc state ::cursor idx))
(defmethod update-state :crsr/sel [state  [_ idx-range] _] (assoc state ::cursor idx-range))


;; *** Indexes and ids

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
        ;; Better be in deltas...
        :else           (indexed/index-of deltas op-id))
      (throw
       (ex-info "Id not found" {:op-id op-id :deltas deltas}))))


(s/fdef op-idx->op-id
        :args (s/cat :state ::state :op-idx ::idx)
        :ret ::delta/prev-id)

(defn- op-idx->op-id
  [{::keys [deltas] :as state} op-idx]
  (let [idx (dec (long op-idx))]
    (when (<= 0 (long idx))
      (:id (nth deltas (long idx))))))

(defn prev-delta-id [deltas start-delta-id] (or (-> deltas peek :id) start-delta-id))


;; ** Player API Internals -- adding existing deltas

(s/fdef add-delta-rf
        :args (s/cat :state ::state :delta ::delta/delta)
        :ret  ::state)

(defmulti ^:private add-delta-rf
  (fn [state delta]
    (-> delta :op first)))

(defmethod add-delta-rf :crsr/mv [state delta]
  (let [op-id  (delta/op-id delta)
        op-idx (op-id->op-idx state op-id)]
    (update-state state [:crsr/mv op-idx] delta)))

(defmethod add-delta-rf :crsr/sel [state {[_ from-id to-id] :op :as delta}]
  (let [from-op-idx (op-id->op-idx state from-id)
        to-op-idx   (op-id->op-idx state to-id)]
    (update-state state [:crsr/sel [from-op-idx to-op-idx]] delta)))

(defmethod add-delta-rf :str/ins
  [state {:keys [id prev-id] :as delta}]
  (let [op-id      (delta/op-id delta)
        op-diff    (delta/op-diff delta)
        op-idx     (op-id->op-idx state op-id)
        ;; TODO document that the idx position is like a caret that sits left
        ;; of the character while still pointing "at it". In order to insert at
        ;; that character we need to 'skip' over it.
        op-idx-ins (inc (long op-idx))]
    (update-state state [:str/ins op-idx-ins op-diff] delta)))

(defmethod add-delta-rf :str/rem
  [{::keys [deltas string] :as state} delta]
  (let [op-id      (delta/op-id delta)
        op-amt     (delta/op-amt delta)
        op-idx     (op-id->op-idx state op-id)]
    (update-state state [:str/rem op-idx op-amt] delta)))


;; ** Editor API internals -- adding edit-events
;; *** Event splicing -- 1 edit-event -> * edit-events

(defmulti ^:private splice-edit-event ::edit-event/op)

(defmethod splice-edit-event :default [e] [e])

(defmethod splice-edit-event :str/ins
  [{::edit-event/keys [op idx diff]}]
  (let [idxs  (range idx (+ (long idx) (count diff)))
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
   ;; Since we're deleting characters one by one, we perform the operations
   ;; starting from the right, so that the indexes of the previous characters
   ;; don't change
   (reverse (range idx (+ (long amt) (long idx))))))

(defmethod splice-edit-event :str/rplc
  [{::edit-event/keys [idx amt diff] :as evt}]
  (let [evts [{::edit-event/op :str/rem ::edit-event/idx idx ::edit-event/amt amt}
              {::edit-event/op :str/ins ::edit-event/idx idx ::edit-event/diff diff}]
        xf   (comp (map splice-edit-event) cat)]
    (transduce xf conj [] evts)))


(s/def ::delta-id ::delta/prev-id)
(s/def ::file-id ::file/id)
(s/def ::branch-id ::branch/id)
(s/def ::current-str-op-id ::delta-id)
(s/def ::add-edit-event-rf-ret-state (s/tuple ::state (s/every ::delta/delta) ::current-str-op-id))

(s/fdef add-edit-event-rf
        :args (s/cat :state ::state
                     :editor-effects ::editor/effects
                     :deltas (s/every ::delta/delta)
                     :file-id ::file/id
                     :branch-id ::branch/id
                     :delta-id ::delta/prev-id
                     :edit-event ::edit-event/edit-event)
        :ret ::add-edit-event-rf-ret-state)

(defmulti ^:private add-edit-event-rf
  (fn [state editor-effects deltas file-id branch-id delta-id edit-event]
    (-> edit-event ::edit-event/op)))

(defmethod add-edit-event-rf :str/ins
  [state {:as editor-effects ::editor/keys [pad-fn uuid-fn timestamp-fn]} deltas file-id branch-id delta-id edit-event]
  (reduce
   (fn add-edit-event-str-ins-rf
     [[state deltas] {:as edit-event ::edit-event/keys [idx diff]}]
     (let [op-id        (op-idx->op-id state idx)
           op           [:str/ins op-id diff]
           prev-id      (prev-delta-id deltas delta-id)
           new-delta-id (uuid-fn edit-event)
           pad          (pad-fn edit-event)
           timestamp    (timestamp-fn edit-event)
           delta        (delta/new-delta
                         {:branch-id branch-id
                          :file-id   file-id
                          :prev-id   prev-id
                          :id        new-delta-id
                          :op        op
                          :pad       pad
                          :timestamp timestamp})
           deltas'      (conj deltas delta)
           state'       (update-state state [:str/ins idx diff] delta)]
       [state' deltas' new-delta-id]))
   ;; Start with the delta-id provided by the `editor-state` as the current id,
   ;; then for each spliced event, ensure that we return the newly created delta-id
   [state deltas delta-id] (splice-edit-event edit-event)))

(defmethod add-edit-event-rf :str/rem
  [state {:as editor-effects ::editor/keys [pad-fn uuid-fn timestamp-fn]} deltas file-id branch-id delta-id edit-event]
  (reduce
   (fn add-edit-event-rf-str-rem-rf
     [[state deltas delta-id] {:as edit-event ::edit-event/keys [idx amt]}]
     (let [op-id        (op-idx->op-id state idx)
           op           [:str/rem op-id amt]
           prev-id      (prev-delta-id deltas delta-id)
           new-delta-id (uuid-fn edit-event)
           pad          (pad-fn edit-event)
           timestamp    (timestamp-fn edit-event)
           delta        (delta/new-delta
                         {:branch-id branch-id
                          :file-id   file-id
                          :prev-id   prev-id
                          :id        new-delta-id
                          :op        op
                          :pad       pad
                          :timestamp timestamp})
           deltas'      (conj deltas delta)
           state'       (update-state state [:str/rem idx amt] delta)]
       [state' deltas' new-delta-id]))
   [state deltas delta-id] (splice-edit-event edit-event)))

(defmethod add-edit-event-rf :crsr/mv
  [state {:as editor-effects ::editor/keys [pad-fn uuid-fn timestamp-fn]} deltas file-id branch-id delta-id {:as edit-event ::edit-event/keys [idx]}]
  (let [op-id        (op-idx->op-id state idx)
        op           [:crsr/mv op-id]
        prev-id      (prev-delta-id deltas delta-id)
        new-delta-id (uuid-fn edit-event)
        pad          (pad-fn edit-event)
        timestamp    (timestamp-fn edit-event)
        delta        (delta/new-delta
                      {:branch-id branch-id
                       :file-id   file-id
                       :prev-id   prev-id
                       :id        new-delta-id
                       :op        op
                       :pad       pad
                       :timestamp timestamp})
        deltas'      (conj deltas delta)]
    [state deltas' op-id]))


;; ** Updates

;; *** Adding deltas -- reading a vims

(s/fdef add-delta
        :args (s/cat :state ::state-by-file-id :delta ::delta/delta)
        :ret ::state-by-file-id)

(defn add-delta
  [state-by-file-id {:keys [file-id] :as delta}]
  (update state-by-file-id file-id (fnil add-delta-rf empty-state) delta))

(s/fdef add-deltas
        :args (s/cat :state-by-file-id ::state-by-file-id :deltas (s/every ::delta/delta))
        :ret ::state-by-file-id)

(defn add-deltas
  [state-by-file-id deltas]
  (reduce (fnil add-delta empty-state-by-file-id) state-by-file-id deltas))


;; *** Adding editing events -- editing a vims

(def ^:private add-edit-event-state-update-fn (fnil add-edit-event-rf empty-state))

;; Private version with internal deltas accumulator
(defn- add-edit-event*
  [state-by-file-id editor-effects deltas file-id branch-id delta-id edit-event]
  (let [state                      (get state-by-file-id file-id)
        rf                         (fnil add-edit-event-rf empty-state)
        [state' deltas' delta-id'] (rf state editor-effects deltas file-id branch-id delta-id edit-event)
        state-by-file-id'                    (assoc state-by-file-id file-id state')]
    [state-by-file-id' deltas' delta-id']))

(s/fdef add-edit-event
        :args (s/cat :state-by-file-id ::state-by-file-id
                     :editor-effects ::editor/effects
                     :file-id ::file/id
                     :branch-id ::branch/id
                     :delta-id ::delta/prev-id
                     :edit-event ::edit-event/edit-event)
        :ret (s/tuple ::state-by-file-id ::new-deltas ::current-str-op-id))

(defn add-edit-event
  "Update `state-by-file-id` by adding the given `edit-event`. The `editor-state` should
  contain the pointers the editor used to retrieve its files' state-by-file-id at the time
  the `edit-event` was created.

  Return a map of `::state-by-file-id` with the updated `state-by-file-id` and `::current-str-op-id`
  a value for the latest delta id that was created as part of the processing of
  `edit-event`.

  Note that this fn handles splicing `edit-event` into multiple deltas when
  required."
  [state-by-file-id editor-effects file-id branch-id delta-id edit-event]
  (add-edit-event* state-by-file-id editor-effects [] file-id branch-id delta-id edit-event))

(defn add-edit-events
  [state-by-file-id editor-effects file-id branch-id delta-id edit-events]
  (reduce
   (fn add-edit-events-rf
     [[state-by-file-id deltas delta-id] edit-event]
     (add-edit-event* state-by-file-id editor-effects deltas file-id branch-id delta-id edit-event))
   [state-by-file-id [] delta-id] edit-events))
