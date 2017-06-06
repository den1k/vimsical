(ns vimsical.vcs.state.files
  "Keep track of the deltas, string and cursor position for files."
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.data.indexed.vector :as indexed]
   [vimsical.vcs.data.splittable :as splittable]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.edit-event :as edit-event]
   [vimsical.vcs.editor :as editor]
   [vimsical.vcs.file :as file]))

;;
;; * Spec
;;

;;
;; ** Singe file state
;;

(s/def ::cursor (s/or :idx ::edit-event/idx :idx-range ::edit-event/range))
(s/def ::deltas (s/every ::delta/delta :kind indexed/vector?))
(s/def ::string string?)
(s/def ::state (s/keys :req [::deltas ::string ::cursor]))
(s/def ::new-deltas (s/every ::delta/delta :kind vector?))

(def ^:private empty-state
  {::deltas (indexed/vector-by :uid)
   ::string ""
   ::cursor 0})

;;
;; ** State by file uid
;;

(s/def ::state-by-file-uid (s/every-kv ::file/uid ::state))

(def empty-state-by-file-uid {})

;;
;; * Internal
;;

;;
;; ** Helpers
;;

;;
;; *** State update
;;

;; XXX This op is hacky because it reuses the format of the delta/op but with
;; indexes instead of uuid, this datatype is not valid anywhere else in the vcs
;; and is only meant to be used internally
(defmulti ^:private update-state-op-spec first)
(defmethod update-state-op-spec :str/ins  [_] (s/tuple #{:str/ins} ::edit-event/idx string?))
(defmethod update-state-op-spec :str/rem  [_] (s/tuple #{:str/rem} ::edit-event/idx ::edit-event/amt))
(defmethod update-state-op-spec :crsr/mv  [_] (s/tuple #{:crsr/mv} ::edit-event/idx))
(defmethod update-state-op-spec :crsr/sel [_] (s/tuple #{:crsr/sel} ::edit-event/range))

(s/def ::update-op (s/multi-spec update-state-op-spec first))

(s/fdef update-state
        :args (s/cat :state ::state :op ::update-op :delta ::delta/delta)
        :ret ::state)

(defmulti ^:private update-state
  (fn [state [op-type] delta] op-type))

(defmethod update-state :str/ins
  [{::keys [deltas] :as state}  [_ idx diff] {:keys [prev-uid] :as delta}]
  (cond
    (nil? prev-uid)
    (assoc state ::deltas (indexed/vec-by :uid [delta]) ::string diff)

    (== idx (count deltas))
    (-> state
        (update ::deltas conj delta)
        (update ::string str diff))

    :else
    (-> state
        (update ::deltas splittable/splice idx (indexed/vec-by :uid [delta]))
        (update ::string splittable/splice idx diff))))

(defmethod update-state :str/rem
  [state  [_ idx amt] _]
  (-> state
      (update ::deltas splittable/omit idx amt)
      (update ::string splittable/omit idx amt)))

(defmethod update-state :crsr/mv  [state  [_ idx] _]       (assoc state ::cursor idx))
(defmethod update-state :crsr/sel [state  [_ idx-range] _] (assoc state ::cursor idx-range))

;;
;; *** Indexes and ids
;;

(s/fdef op-uid->op-idx
        :args (s/cat :state ::state :op-uid ::delta/prev-uid)
        :ret  ::edit-event/idx)

(defn- op-uid->op-idx
  [{::keys [deltas] :as state} op-uid]
  (or (cond
        ;; First delta
        (nil? op-uid)   0
        ;; Moved to a new file
        (empty? deltas) 0
        ;; Better be in deltas...
        :else           (indexed/index-of deltas op-uid))
      (throw
       (ex-info "Id not found" {:op-uid op-uid :deltas deltas}))))


(s/fdef op-idx->op-uid
        :args (s/cat :state ::state :op-idx ::edit-event/idx)
        :ret ::delta/prev-uid)

(defn- op-idx->op-uid
  [{::keys [deltas]} op-idx]
  (let [idx (dec (long op-idx))]
    (when (<= 0 (long idx))
      (:uid (nth deltas (long idx))))))

;;
;; ** Player API Internals -- adding existing deltas
;;

(s/fdef add-delta-rf
        :args (s/cat :state ::state :delta ::delta/delta)
        :ret  ::state)

(defmulti ^:private add-delta-rf
  (fn [state delta]
    (-> delta :op first)))

(defmethod add-delta-rf :crsr/mv [state delta]
  (let [op-uid (delta/op-uid delta)
        op-idx (op-uid->op-idx state op-uid)]
    (update-state state [:crsr/mv op-idx] delta)))

(defmethod add-delta-rf :crsr/sel [state {[_ from-uid to-uid] :op :as delta}]
  (let [from-op-idx (op-uid->op-idx state from-uid)
        to-op-idx   (op-uid->op-idx state to-uid)]
    (update-state state [:crsr/sel [from-op-idx to-op-idx]] delta)))

(defmethod add-delta-rf :str/ins
  [state {:keys [prev-uid] :as delta}]
  (let [op-uid     (delta/op-uid delta)
        op-diff    (delta/op-diff delta)
        op-idx     (op-uid->op-idx state op-uid)
        ;; TODO document that the idx position is like a caret that sits left
        ;; of the character while still pointing "at it". In order to insert at
        ;; that character we need to 'skip' over it.
        op-idx-ins (inc (long op-idx))]
    (update-state state [:str/ins op-idx-ins op-diff] delta)))

(defmethod add-delta-rf :str/rem
  [{::keys [deltas string] :as state} delta]
  (let [op-uid (delta/op-uid delta)
        op-amt (delta/op-amt delta)
        op-idx (op-uid->op-idx state op-uid)]
    (update-state state [:str/rem op-idx op-amt] delta)))


;;
;; ** Editor API internals -- adding edit-events
;;

;;
;; *** Event splicing -- 1 edit-event -> * edit-events
;;

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


(s/def ::delta-uid ::delta/prev-uid)
(s/def ::file-uid ::file/uid)
(s/def ::branch-uid ::branch/uid)
(s/def ::current-str-op-uid ::delta-uid)
(s/def ::add-edit-event-rf-ret-state (s/tuple ::state (s/every ::delta/delta) ::current-str-op-uid))

(s/fdef add-edit-event-rf
        :args (s/cat :state ::state
                     :editor-effects ::editor/effects
                     :deltas (s/every ::delta/delta)
                     :file-uid ::file/uid
                     :branch-uid ::branch/uid
                     :prev-delta-uid ::delta/prev-uid
                     :edit-event ::edit-event/edit-event)
        :ret ::add-edit-event-rf-ret-state)

(defmulti ^:private add-edit-event-rf
  (fn [state editor-effects deltas file-uid branch-uid prev-delta-uid edit-event]
    (-> edit-event ::edit-event/op)))

(defmethod add-edit-event-rf :str/ins
  [state {:as editor-effects ::editor/keys [pad-fn uuid-fn timestamp-fn]} deltas file-uid branch-uid prev-delta-uid edit-event]
  (reduce
   (fn add-edit-event-str-ins-rf
     [[state deltas prev-delta-uid] {:as edit-event ::edit-event/keys [idx diff]}]
     (let [op-uid        (op-idx->op-uid state idx)
           op            [:str/ins op-uid diff]
           new-delta-uid (uuid-fn edit-event)
           pad           (pad-fn edit-event)
           timestamp     (timestamp-fn edit-event)
           delta         (delta/new-delta
                          {:branch-uid branch-uid
                           :file-uid   file-uid
                           :prev-uid   prev-delta-uid
                           :uid        new-delta-uid
                           :op         op
                           :pad        pad
                           :timestamp  timestamp})
           deltas'       (conj deltas delta)
           state'        (update-state state [:str/ins idx diff] delta)]
       [state' deltas' new-delta-uid]))
   ;; Start with the delta-uid provided by the `editor-state` as the current uid,
   ;; then for each spliced event, ensure that we return the newly created delta-uid
   [state deltas prev-delta-uid] (splice-edit-event edit-event)))

(defmethod add-edit-event-rf :str/rem
  [state {:as editor-effects ::editor/keys [pad-fn uuid-fn timestamp-fn]} deltas file-uid branch-uid prev-delta-uid edit-event]
  (reduce
   (fn add-edit-event-rf-str-rem-rf
     [[state deltas prev-delta-uid] {:as edit-event ::edit-event/keys [idx amt]}]
     (let [op-uid        (op-idx->op-uid state idx)
           ;; NOTE shouln't we use the following for the prev-delta-uid we return?
           op            [:str/rem op-uid amt]
           new-delta-uid (uuid-fn edit-event)
           pad           (pad-fn edit-event)
           timestamp     (timestamp-fn edit-event)
           delta         (delta/new-delta
                          {:branch-uid branch-uid
                           :file-uid   file-uid
                           :prev-uid   prev-delta-uid
                           :uid        new-delta-uid
                           :op         op
                           :pad        pad
                           :timestamp  timestamp})
           deltas'       (conj deltas delta)
           state'        (update-state state [:str/rem idx amt] delta)]
       [state' deltas' new-delta-uid]))
   [state deltas prev-delta-uid] (splice-edit-event edit-event)))

(defmethod add-edit-event-rf :str/rplc
  [state editor-effects deltas file-uid branch-uid prev-delta-uid edit-event]
  (reduce
   (fn add-edit-event-rf-str-rplc-rf
     [[state deltas prev-delta-uid] edit-event]
     (add-edit-event-rf state editor-effects deltas file-uid branch-uid prev-delta-uid edit-event))
   [state deltas prev-delta-uid] (splice-edit-event edit-event)))

(defmethod add-edit-event-rf :crsr/mv
  [state {:as editor-effects ::editor/keys [pad-fn uuid-fn timestamp-fn]} deltas file-uid branch-uid prev-delta-uid {:as edit-event ::edit-event/keys [idx]}]
  (let [op-uid        (op-idx->op-uid state idx)
        op            [:crsr/mv op-uid]
        new-delta-uid (uuid-fn edit-event)
        pad           (pad-fn edit-event)
        timestamp     (timestamp-fn edit-event)
        delta         (delta/new-delta
                       {:branch-uid branch-uid
                        :file-uid   file-uid
                        :prev-uid   prev-delta-uid
                        :uid        new-delta-uid
                        :op         op
                        :pad        pad
                        :timestamp  timestamp})
        deltas'       (conj deltas delta)
        state'        (update-state state [:crsr/mv idx] delta)]
    [state' deltas' new-delta-uid]))

(defmethod add-edit-event-rf :crsr/sel
  [state {:as editor-effects ::editor/keys [pad-fn uuid-fn timestamp-fn]} deltas file-uid branch-uid prev-delta-uid {:as edit-event [from-idx to-idx] ::edit-event/range}]
  (let [from-uid      (op-idx->op-uid state from-idx)
        to-uid        (op-idx->op-uid state to-idx)
        op            [:crsr/sel from-uid to-uid]
        new-delta-uid (uuid-fn edit-event)
        pad           (pad-fn edit-event)
        timestamp     (timestamp-fn edit-event)
        delta         (delta/new-delta
                       {:branch-uid branch-uid
                        :file-uid   file-uid
                        :prev-uid   prev-delta-uid
                        :uid        new-delta-uid
                        :op         op
                        :pad        pad
                        :timestamp  timestamp})
        deltas'       (conj deltas delta)
        state'        (update-state state [:crsr/sel from-idx to-idx] delta)]
    [state' deltas' new-delta-uid]))

;;
;; * API
;;

;;
;; ** Updates
;;

;;
;; *** Adding deltas -- reading a vims
;;

(s/fdef add-delta
        :args (s/cat :state ::state-by-file-uid :delta ::delta/delta)
        :ret ::state-by-file-uid)

(defn add-delta
  [state-by-file-uid {:keys [file-uid] :as delta}]
  (update state-by-file-uid file-uid (fnil add-delta-rf empty-state) delta))

(s/fdef add-deltas
        :args (s/cat :state-by-file-uid ::state-by-file-uid :deltas (s/every ::delta/delta))
        :ret ::state-by-file-uid)

(defn add-deltas
  [state-by-file-uid deltas]
  (reduce (fnil add-delta empty-state-by-file-uid) state-by-file-uid deltas))

;;
;; *** Adding editing events -- editing a vims
;;

(def ^:private add-edit-event-state-update-fn (fnil add-edit-event-rf empty-state))

;; Private version with internal deltas accumulator
(defn- add-edit-event*
  [state-by-file-uid editor-effects deltas file-uid branch-uid prev-delta-uid edit-event]
  (let [state                            (get state-by-file-uid file-uid)
        rf                               (fnil add-edit-event-rf empty-state)
        [state' deltas' prev-delta-uid'] (rf state editor-effects deltas file-uid branch-uid prev-delta-uid edit-event)
        state-by-file-uid'               (assoc state-by-file-uid file-uid state')]
    [state-by-file-uid' deltas' prev-delta-uid']))

(s/def ::add-edit-event-acc
  (s/tuple ::state-by-file-uid ::new-deltas ::current-str-op-uid))

(s/fdef add-edit-event
        :args (s/cat :state-by-file-uid ::state-by-file-uid
                     :editor-effects ::editor/effects
                     :file-uid ::file/uid
                     :branch-uid ::branch/uid
                     :prev-delta-uid ::delta/prev-uid
                     :edit-event ::edit-event/edit-event)
        :ret ::add-edit-event-acc)

(defn add-edit-event
  "Update `state-by-file-uid` by adding the given `edit-event`. The `editor-state` should
  contain the pointers the editor used to retrieve its files' state-by-file-uid at the time
  the `edit-event` was created.

  Return a map of `::state-by-file-uid` with the updated `state-by-file-uid` and `::current-str-op-uid`
  a value for the latest delta uid that was created as part of the processing of
  `edit-event`.

  Note that this fn handles splicing `edit-event` into multiple deltas when
  required."
  [state-by-file-uid editor-effects file-uid branch-uid delta-uid edit-event]
  (add-edit-event* state-by-file-uid editor-effects [] file-uid branch-uid delta-uid edit-event))

(defn add-edit-events
  [state-by-file-uid editor-effects file-uid branch-uid delta-uid edit-events]
  (reduce
   (fn add-edit-events-rf
     [[state-by-file-uid deltas prev-delta-uid] edit-event]
     (add-edit-event* state-by-file-uid editor-effects deltas file-uid branch-uid prev-delta-uid edit-event))
   [state-by-file-uid [] delta-uid] edit-events))

;;
;; ** Queries
;;

(s/fdef deltas :args (s/cat :state-by-file-uid ::state-by-file-uid :file-uid ::file/uid) :ret  (s/every ::delta/delta) )

(defn deltas
  [state-by-file-uid file-uid]
  (get-in state-by-file-uid [file-uid ::deltas]))

(s/fdef string :args (s/cat :state-by-file-uid ::state-by-file-uid :file-uid ::file/uid) :ret  (s/nilable string?))

(defn string
  [state-by-file-uid file-uid]
  (get-in state-by-file-uid [file-uid ::string]))

(s/fdef cursor :args (s/cat :state-by-file-uid ::state-by-file-uid :file-uid ::file/uid) :ret  nat-int?)

(defn cursor
  [state-by-file-uid file-uid]
  (get-in state-by-file-uid [file-uid ::cursor]))
