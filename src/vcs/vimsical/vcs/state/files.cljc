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

(declare op-uid->op-idx op-idx->op-uid)
;;
;; ** Singe file state
;;

(s/def ::cursor (s/or :idx ::edit-event/idx :idx-range ::edit-event/range))
(s/def ::deltas (s/every ::delta/delta :kind indexed/vector?))
(s/def ::string string?)

(defn op-uid<->op-idx-equivlaence
  [{::keys [deltas string] :as state}]
  (letfn [(delta-equiv? [delta]
            (let [expect-uid (delta/op-uid delta)
                  idx        (some->> expect-uid (op-uid->op-idx state))
                  uid        (some->> idx (op-idx->op-uid state))]
              (= expect-uid uid)))]
    (every? delta-equiv? deltas)))

(defn deltas<->string-equivalence
  [{::keys [deltas string]}]
  (let [xf (comp (map :op) (map #(nth % 2)))]
    (= string (transduce xf str "" deltas))))

(s/def ::state
  (s/and
   (s/keys :req [::deltas ::string ::cursor])
   deltas<->string-equivalence))

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
;; *** Indexes and ids
;;

(s/def ::op-uid (s/or :id ::delta/prev-uid :range (s/tuple ::delta/prev-uid ::delta/prev-uid)))

(s/fdef op-uid->op-idx
        :args (s/cat :state ::state :op-uid ::op-uid)
        :ret  ::edit-event/idx)

(defn op-uid->op-idx
  [{::keys [deltas] :as state} op-uid]
  (or (cond
        ;; First delta
        (nil? op-uid)    0
        ;; Moved to a new file
        (empty? deltas)  0
        ;; range
        (vector? op-uid) (op-uid->op-idx state (second op-uid))
        ;; uid
        :else            (inc (indexed/index-of deltas op-uid)))
      (throw
       (ex-info "Id not found" {:op-uid op-uid :deltas deltas}))))

(s/fdef op-idx->op-uid
        :args (s/cat :state ::state :op-idx ::edit-event/idx)
        :ret ::delta/prev-uid)

(defn op-idx->op-uid
  [{::keys [deltas]} op-idx]
  (try
    (some->> op-idx dec (nth deltas) :uid)
    (catch #?(:clj Throwable :cljs js/Error) e)))

;;
;; ** Player API Internals -- adding existing deltas
;;

(defn cursor-update [cursor f & args]
  (max 0 (apply f (cond-> cursor (vector? cursor) (second)) args)))

(s/fdef add-delta-rf
        :args (s/cat :state ::state :delta ::delta/delta)
        :ret  ::state)

(defmulti ^:private add-delta-rf
  (fn [state delta]
    (-> delta :op first)))

(defmethod add-delta-rf :crsr/mv [state delta]
  (let [op-uid (delta/op-uid delta)
        op-idx (op-uid->op-idx state op-uid)]
    (assoc state ::cursor op-idx)))

(defmethod add-delta-rf :crsr/sel [state {[_ [from-uid to-uid]] :op :as delta}]
  (let [from-op-idx (op-uid->op-idx state from-uid)
        to-op-idx   (op-uid->op-idx state to-uid)]
    (assoc state ::cursor [from-op-idx to-op-idx])))

(defmethod add-delta-rf :str/ins
  [{::keys [deltas string cursor] :as state} {:keys [prev-uid] :as delta}]
  (let [op-uid  (delta/op-uid delta)
        op-diff (delta/op-diff delta)
        op-idx  (op-uid->op-idx state op-uid)]
    (cond
      ;;
      ;; Initialize state
      (nil? prev-uid)
      (assoc state
             ::cursor (count op-diff)
             ::deltas (indexed/vec-by :uid [delta])
             ::string op-diff)
      ;;
      ;;  Append
      (== op-idx (count deltas))
      (assoc state
             ::cursor (+ op-idx (delta/prospective-idx-offset delta))
             ::deltas (conj deltas delta)
             ::string (str string op-diff))
      ;;
      ;; Splice
      :else
      (assoc state
             ::cursor (+ op-idx (delta/prospective-idx-offset delta))
             ::deltas (splittable/splice deltas op-idx (indexed/vec-by :uid [delta]))
             ::string (splittable/splice string op-idx op-diff)))))

(defmethod add-delta-rf :str/rem
  [{::keys [deltas string] :as state} delta]
  (let [op-uid (delta/op-uid delta)
        op-amt (delta/op-amt delta)
        op-idx (op-uid->op-idx state op-uid)]
    ;; Delete left to-right, so we don't move
    (assoc state
           ::cursor (+ op-idx (delta/prospective-idx-offset delta))
           ::deltas (splittable/omit deltas op-idx op-amt)
           ::string (splittable/omit string op-idx op-amt))))

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
   ;; starting from the right deleting chars one by one, so that the indexes of
   ;; the previous characters don't change
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

(defmulti add-edit-event-rf
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
           state'        (add-delta-rf state delta)]
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
           state'        (add-delta-rf state delta)]
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
        state'        (add-delta-rf state delta)]
    [state' deltas' prev-delta-uid]))

(defmethod add-edit-event-rf :crsr/sel
  [state {:as editor-effects ::editor/keys [pad-fn uuid-fn timestamp-fn]} deltas file-uid branch-uid prev-delta-uid {:as edit-event [from-idx to-idx] ::edit-event/range}]
  (let [from-uid      (op-idx->op-uid state from-idx)
        to-uid        (op-idx->op-uid state to-idx)
        op            [:crsr/sel [from-uid to-uid]]
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
        state'        (add-delta-rf state delta)]
    [state' deltas' prev-delta-uid]))

;;
;; * API
;;

;;
;; ** Updates
;;

;;
;; *** Adding deltas -- reading a vims
;;

(def ^:private add-delta-rf* (fnil add-delta-rf empty-state))

(s/fdef add-delta
        :args (s/cat :state ::state-by-file-uid :delta ::delta/delta)
        :ret ::state-by-file-uid)

(defn add-delta
  [state-by-file-uid {:keys [file-uid] :as delta}]
  (update state-by-file-uid file-uid add-delta-rf* delta))

(def ^:private add-delta* (fnil add-delta empty-state-by-file-uid))

(s/fdef add-deltas
        :args (s/cat :state-by-file-uid ::state-by-file-uid :deltas (s/every ::delta/delta))
        :ret ::state-by-file-uid)

(defn add-deltas
  [state-by-file-uid deltas]
  (reduce add-delta* state-by-file-uid deltas))

;;
;; *** Adding editing events -- editing a vims
;;

(def ^:private add-edit-event-rf* (fnil add-edit-event-rf empty-state))

;; Private version with internal deltas accumulator
(defn- add-edit-event*
  [state-by-file-uid editor-effects deltas file-uid branch-uid prev-delta-uid edit-event]
  (let [state                            (get state-by-file-uid file-uid)

        [state' deltas' prev-delta-uid'] (add-edit-event-rf* state editor-effects deltas file-uid branch-uid prev-delta-uid edit-event)
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
