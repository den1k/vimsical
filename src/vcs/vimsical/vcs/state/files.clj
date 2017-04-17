(ns vimsical.vcs.state.files
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.data.splittable :as splittable]
   [vimsical.vcs.data.indexed.vector :as indexed]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.file :as file]))


;; * Spec
;; ** Singe file state

(s/def ::idx (s/or :nil-delta #{-1} :delta nat-int?))
(s/def ::id->idx (s/map-of (s/nilable ::delta/id) ::idx))
(s/def ::deltas (s/and ::indexed/vector (s/every ::delta/delta)))
(s/def ::string string?)
(s/def ::state (s/keys :req [::id->idx ::deltas ::string]))

(defn new-state [] {::string "" ::deltas (indexed/vector-by :id) ::id->idx {}})

(def empty-state (new-state))


;; ** All files' states by delta id

(s/def ::states (s/map-of ::delta/id (s/map-of ::file/id ::state)))


;; * Internal
;; ** Using the state to map between string index positions and delta id

(s/fdef op-idx->op-id
        :args (s/cat :state ::state :op-idx ::idx)
        :ret ::delta/id)

(defn op-idx->op-id
  [{::keys [deltas id->idx] :as state} op-idx]
  (:id (if (== (count deltas) op-idx)
         (last (.v deltas))
         (nth deltas op-idx))))

(s/fdef op-id->op-idx
        :args (s/cat :state ::state :op-id ::delta/prev-id)
        :ret  ::idx)

(defn op-id->op-idx
  [{::keys [deltas id->idx] :as state} op-id]
  (cond
    ;; First delta
    (nil? op-id)    -1
    ;; Moved to a new file
    (empty? deltas) 0

    :else (indexed/index-of deltas op-id)
    ;; (get id->idx op-id)
    ))


;; ** State update

(s/fdef state-add-delta
        :args (s/cat :state ::state :delta ::delta/delta)
        :ret  ::state)

(defmulti ^:private state-add-delta
  (fn [state delta]
    (-> delta :op first)))

(defmethod state-add-delta :default [state delta]
  (assert false delta)
  state)

(defmethod state-add-delta :crsr/mv
  [{::keys [id->idx deltas string] :as state} {:keys [id] :as delta}]
  (let [op-id  (delta/op-id delta)
        op-idx (op-id->op-idx state op-id)]
    (update state ::id->idx assoc id (inc op-idx))))

(defmethod state-add-delta :str/ins
  [{::keys [id->idx deltas string] :as state} {:keys [id prev-id] :as delta}]
  (if (nil? prev-id)
    {::id->idx (assoc id->idx id (count (delta/op-diff delta)))
     ::deltas  (indexed/vec-by :id [delta])
     ::string  (delta/op-diff delta)}
    (let [op-id      (delta/op-id delta)
          op-diff    (delta/op-diff delta)
          op-idx     (op-id->op-idx state op-id)
          op-idx-ins (+ op-idx (count op-diff))]
      (-> state
          (update ::id->idx assoc id op-idx-ins)
          (update ::deltas splittable/splice op-idx-ins (indexed/vec-by :id [delta]))
          (update ::string splittable/splice op-idx-ins op-diff)))))

(defmethod state-add-delta :str/rem
  [{::keys [id->idx deltas string] :as state} {:keys [id] [_ op-id] :op :as delta}]
  (let [op-id      (delta/op-id delta)
        op-amt     (delta/op-amt delta)
        op-idx     (op-id->op-idx state op-id)
        op-idx-rem (max 0 (- op-idx op-amt))]
    (-> state
        (update ::id->idx assoc id op-idx-rem)
        (update ::deltas splittable/omit op-idx-rem op-amt)
        (update ::string splittable/omit op-idx-rem op-amt))))

;; * API

(defn new-states ([] {}))

(def empty-states (new-states))

;; ** File(s) state accessors

(s/fdef get-file-state
        :args (s/cat :states ::states :delta-id ::delta/id :file-id ::file/id)
        :ret ::state)

(defn get-file-state
  [states delta-id file-id]
  (get-in states [delta-id file-id]))

(s/fdef get-files-states
        :args (s/cat :states ::states :delta-id ::delta/id)
        :ret  (s/map-of ::file/id ::state))

(defn get-files-states
  [states delta-id]
  (get states delta-id))


;; ** Adding deltas (when reading a vims)

(s/fdef add-delta
        :args (s/cat :states ::states :delta ::delta/delta)
        :ret ::states)

(defn add-delta
  [states {:keys [file-id id prev-id] :as delta}]
  (let [file-cache (or (get states prev-id) {file-id empty-state})
        ;; _ (println prev-id file-id file-cache)
        ;; _ (assert (get file-cache file-id)  file-id)
        state      (update file-cache file-id (fnil state-add-delta empty-state) delta)]
    (assoc states id state)))

(s/fdef add-deltas
        :args (s/cat :states ::states :delta (s/every ::delta/delta))
        :ret ::states)

(defn add-deltas
  [states deltas]
  (reduce add-delta states deltas))

;; ** Adding deltas (when writing a vims)

;; Not handled here, see `vimsical.vcs.state.editor`
