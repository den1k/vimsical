(ns vimsical.vcs.state.files
  "Keep track of the deltas, string and cursor position for files."
  (:require
   [taoensso.tufte :as tufte :refer (defnp p profiled profile)]
   [net.cgrand.xforms.rfs :as rfs]
   [clojure.spec.alpha :as s]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.data.indexed.vector :as indexed]
   [vimsical.vcs.data.splittable :as splittable]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.edit-event :as edit-event]
   [vimsical.vcs.editor :as editor]
   [vimsical.vcs.file :as file]
   [clojure.string :as str]))

;;
;; * Spec
;;

(declare op-uid->op-idx op-idx->op-uid)

;;
;; ** File state (per delta)
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
;; ** Mapping between index positions and uids using the indexed delta vector
;;

(s/fdef op-uid->op-idx
        :args (s/cat :state ::state :op-uid ::delta/op-uid)
        :ret  ::edit-event/idx)

(defnp op-uid->op-idx
  [{::keys [deltas cursor] :as state} op-uid]
  (let [start (max 0 (dec (if (vector? cursor) (second cursor) cursor)))]
    (or (cond
          ;; First delta
          (nil? op-uid)    0
          ;; Moved to a new file
          (empty? deltas)  0
          ;; range
          (vector? op-uid) (op-uid->op-idx state (second op-uid))
          ;; uid
          :else            (when-some [i (indexed/index-of deltas op-uid start)] (inc ^long i)))
        (throw
         (ex-info "Id not found" {:op-uid op-uid :deltas deltas})))))

(s/fdef op-idx->op-uid
        :args (s/cat :state ::state :op-idx ::edit-event/idx)
        :ret ::delta/op-uid)

(defnp op-idx->op-uid
  [{::keys [deltas]} op-idx]
  (try
    (some->> op-idx dec (nth deltas) :uid)
    (catch #?(:clj Throwable :cljs js/Error) e)))

;;
;; ** Updating a file state with one or multiple deltas
;;

;;
;; *** Single delta update
;;

(s/fdef update-state-with-delta
        :args (s/cat :state ::state :delta ::delta/delta)
        :ret  ::state)

(defmulti ^:private update-state-with-delta
  (fn [state delta]
    (-> delta :op first)))

(defmethod update-state-with-delta :crsr/mv [state delta]
  (p :update-state-with-delta/crsr-mv
     (let [op-uid (delta/op-uid delta)
           op-idx (op-uid->op-idx state op-uid)]
       (assoc state ::cursor op-idx))))

(defmethod update-state-with-delta :crsr/sel [state {[_ [from-uid to-uid]] :op :as delta}]
  (p :update-state-with-delta/crsr-sel
     (let [from-op-idx (op-uid->op-idx state from-uid)
           to-op-idx   (op-uid->op-idx state to-uid)]
       (assoc state ::cursor [from-op-idx to-op-idx]))))

(defmethod update-state-with-delta :str/ins
  [{::keys [deltas string cursor] :as state} {:keys [prev-uid] :as delta}]
  (p :update-state-with-delta/str-ins
     (let [op-uid  (delta/op-uid delta)
           op-idx  (op-uid->op-idx state op-uid)
           op-diff (delta/op-diff delta)]
       (cond
         ;;
         ;; Initialize state
         (nil? prev-uid)
         (p :update-state-with-delta/str-ins-init
            (assoc state
                   ::cursor (count op-diff)
                   ::deltas (indexed/vec-by :uid [delta])
                   ::string op-diff))
         ;;
         ;;  Append
         (== op-idx (count deltas))
         (p :update-state-with-delta/str-ins-append
            (assoc state
                   ::cursor (+ op-idx (delta/prospective-idx-offset delta))
                   ::deltas (conj deltas delta)
                   ::string (str string op-diff)))
         ;;
         ;; Splice
         :else
         (p :update-state-with-delta/str-ins-splice
            (assoc state
                   ::cursor (+ op-idx (delta/prospective-idx-offset delta))
                   ::deltas (p :update-state-with-delta/str-ins-splice-deltas (splittable/insert deltas op-idx delta))
                   ::string (p :update-state-with-delta/str-ins-splice-string (splittable/splice string op-idx op-diff))))))))

(defmethod update-state-with-delta :str/rem
  [{::keys [deltas string] :as state} delta]
  (p :update-state-with-delta/str-rem
     (let [op-uid (delta/op-uid delta)
           op-idx (op-uid->op-idx state op-uid)
           op-amt (delta/op-amt delta)]
       ;; Delete left to-right, so we don't move
       (assoc state
              ::cursor op-idx
              ::deltas (splittable/omit deltas op-idx op-amt)
              ::string (splittable/omit string op-idx op-amt)))))

;;
;; *** Splice deltas update
;;

(s/fdef update-state-with-splice-deltas
        :args (s/cat :state ::state :deltas (s/every ::delta/delta) :edit-event ::edit-event/edit-event)
        :ret  ::state)

(defmulti ^:private update-state-with-splice-deltas
  (fn [state deltas edit-event]
    (-> deltas first delta/op-type)))

(defmethod update-state-with-splice-deltas :str/ins
  [{::keys [deltas string cursor] :as state}
   [{:keys [prev-uid] :as delta} :as deltas']
   {op-idx ::edit-event/idx op-diff ::edit-event/diff :as edit-event}]
  (p :update-state-with-splice-deltas/str-ins
     (cond
       ;;
       ;; Initialize state
       (nil? prev-uid)
       (assoc state
              ::cursor (count op-diff)
              ::deltas (indexed/vec-by :uid deltas')
              ::string op-diff)
       ;;
       ;;  Append
       (== op-idx (count deltas))
       (assoc state
              ::cursor (count op-diff)
              ::deltas (splittable/append deltas (indexed/vec-by :uid deltas'))
              ::string (str string op-diff))
       ;;
       ;; Splice
       :else
       (assoc state
              ::cursor (reduce + op-idx (map delta/prospective-idx-offset deltas'))
              ::deltas (splittable/splice deltas op-idx (indexed/vec-by :uid deltas'))
              ::string (splittable/splice string op-idx op-diff)))))

;;
;; ** Edit events api
;;

;;
;; *** Splicing
;;

(defmulti ^:private splice-edit-event? ::edit-event/op)
(defmethod ^:private splice-edit-event? :default  [_] false)
(defmethod ^:private splice-edit-event? :str/ins  [{::edit-event/keys [diff]}] (< 1 (count diff)))
(defmethod ^:private splice-edit-event? :str/rplc [edit-event] true)

(defmulti ^:private splice-edit-event ::edit-event/op)

(defmethod splice-edit-event :str/ins
  [{::edit-event/keys [op idx diff] :as edit-event}]
  (p :splice-edit-event/str-ins
     (let [idxs  (range idx (+ (long idx) (count diff)))
           chars (seq diff)]
       (mapv
        (fn splice-edit-event-str-ins-rf
          [[idx char]]
          {::edit-event/op   op
           ::edit-event/idx  idx
           ::edit-event/diff (str char)})
        (map vector idxs chars)))))

(defmethod splice-edit-event :str/rplc
  [{::edit-event/keys [idx amt diff]}]
  (p :splice-edit-event/str-rplc
     [{::edit-event/op :str/rem ::edit-event/idx idx ::edit-event/amt amt}
      {::edit-event/op :str/ins ::edit-event/idx idx ::edit-event/diff diff}]))

(s/def ::delta-uid ::delta/prev-uid)
(s/def ::file-uid ::file/uid)
(s/def ::branch-uid ::branch/uid)
(s/def ::current-str-op-uid ::delta-uid)

;;
;; *** Add edit event
;;

;; NOTE the logic here is to use some intermediary state to generate deltas from
;; the edit event, then reuse the state update functions we use when we build
;; our state directly from deltas

(s/def ::add-edit-event-args
  (s/cat :state ::state
         :editor-effects ::editor/effects
         :deltas (s/every ::delta/delta)
         :file-uid ::file/uid
         :branch-uid ::branch/uid
         :prev-delta-uid ::delta/prev-uid
         :edit-event ::edit-event/edit-event))

;; NOTE For performance reasons we distinguish between adding a simple delta and
;; a delta that needs to be spliced

(s/def ::add-edit-event-ret (s/tuple ::state (s/every ::delta/delta) ::current-str-op-uid))

(s/fdef add-simple-edit-event :args ::add-edit-event-args :ret ::add-edit-event-ret)

(defmulti add-simple-edit-event
  (fn [state editor-effects deltas file-uid branch-uid prev-delta-uid edit-event]
    (-> edit-event ::edit-event/op)))

(s/fdef add-splice-edit-event :args ::add-edit-event-args :ret ::add-edit-event-ret)

(defmulti add-splice-edit-event
  (fn [state editor-effects deltas file-uid branch-uid prev-delta-uid edit-event]
    (-> edit-event ::edit-event/op)))

(defn- add-edit-event-internal
  [state editor-effects deltas file-uid branch-uid prev-delta-uid edit-event]
  (if (splice-edit-event? edit-event)
    (add-splice-edit-event state editor-effects deltas file-uid branch-uid prev-delta-uid edit-event)
    (add-simple-edit-event state editor-effects deltas file-uid branch-uid prev-delta-uid edit-event)))

(defmethod add-simple-edit-event :str/ins
  [state {:as editor-effects ::editor/keys [pad-fn uuid-fn timestamp-fn]} deltas file-uid branch-uid prev-delta-uid {::edit-event/keys [idx diff] :as edit-event}]
  (p :add-simple-edit-event/str-ins
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
           state'        (update-state-with-delta state delta)]
       [state' deltas' new-delta-uid])))

(defmethod add-splice-edit-event :str/ins
  [state {:as editor-effects ::editor/keys [pad-fn uuid-fn timestamp-fn]} deltas file-uid branch-uid prev-delta-uid {::edit-event/keys [idx diff] :as edit-event}]
  (p :add-splice-edit-event/str-ins
     (let [[deltas'! _ last-delta-uid] (reduce
                                        (fn spliced-add-edit-event-str-ins-rf
                                          [[deltas! op-uid prev-delta-uid] {:as edit-event ::edit-event/keys [diff]}]
                                          (let [op            [:str/ins op-uid diff]
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
                                                deltas!'      (conj! deltas! delta)]
                                            [deltas!' new-delta-uid new-delta-uid]))
                                        [(transient []) (op-idx->op-uid state idx) prev-delta-uid] (splice-edit-event edit-event))
           deltas' (persistent! deltas'!)]
       [(update-state-with-splice-deltas state deltas' edit-event) (into deltas deltas') last-delta-uid])))

(defmethod add-simple-edit-event :str/rem
  [state {:as editor-effects ::editor/keys [pad-fn uuid-fn timestamp-fn]} deltas file-uid branch-uid prev-delta-uid {:as edit-event ::edit-event/keys [idx amt]}]
  (p :add-simple-edit-event/str-rem
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
           state'        (update-state-with-delta state delta)]
       [state' deltas' new-delta-uid])))

(declare add-edit-event-internal*)

(defmethod add-splice-edit-event :str/rplc
  [state editor-effects deltas file-uid branch-uid prev-delta-uid edit-event]
  (p :add-splice-edit-event/str-rplc
     (reduce
      (fn add-edit-event-rf-str-rplc-rf
        [[state deltas prev-delta-uid] edit-event]
        (add-edit-event-internal* state editor-effects deltas file-uid branch-uid prev-delta-uid edit-event))
      [state deltas prev-delta-uid] (splice-edit-event edit-event))))

(defmethod add-simple-edit-event :crsr/mv
  [state {:as editor-effects ::editor/keys [pad-fn uuid-fn timestamp-fn]} deltas file-uid branch-uid prev-delta-uid {:as edit-event ::edit-event/keys [idx]}]
  (p :add-simple-edit-event/crsr-mv
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
           state'        (update-state-with-delta state delta)]
       [state' deltas' new-delta-uid])))

(defmethod add-simple-edit-event :crsr/sel
  [state {:as editor-effects ::editor/keys [pad-fn uuid-fn timestamp-fn]} deltas file-uid branch-uid prev-delta-uid {:as edit-event [from-idx to-idx] ::edit-event/range}]
  (p :add-simple-edit-event/crsr-sel
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
           state'        (update-state-with-delta state delta)]
       [state' deltas' new-delta-uid])))

;;
;; * API
;;

;;
;; ** Updates
;;

;;
;; *** Adding deltas -- reading a vims
;;

(def ^:private add-delta-rf* (fnil update-state-with-delta empty-state))

(s/fdef add-delta
        :args (s/cat :state ::state-by-file-uid :delta ::delta/delta)
        :ret ::state-by-file-uid)

(defnp add-delta
  [state-by-file-uid {:keys [file-uid] :as delta}]
  (update state-by-file-uid file-uid add-delta-rf* delta))

(def ^:private add-delta* (fnil add-delta empty-state-by-file-uid))

(s/fdef add-deltas
        :args (s/cat :state-by-file-uid ::state-by-file-uid :deltas (s/nilable (s/every ::delta/delta)))
        :ret ::state-by-file-uid)

(defnp add-deltas
  [state-by-file-uid deltas]
  (reduce add-delta* state-by-file-uid deltas))

;;
;; *** Adding editing events -- editing a vims
;;

(def ^:private add-edit-event-internal* (fnil add-edit-event-internal empty-state))

;; Private version with internal deltas accumulator
(defn- add-edit-event*
  [state-by-file-uid editor-effects deltas file-uid branch-uid prev-delta-uid edit-event]
  (let [state                            (get state-by-file-uid file-uid)
        [state' deltas' prev-delta-uid'] (add-edit-event-internal* state editor-effects deltas file-uid branch-uid prev-delta-uid edit-event)
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

(defnp add-edit-event
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

(defnp add-edit-events
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
