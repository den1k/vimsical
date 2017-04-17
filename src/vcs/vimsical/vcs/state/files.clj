(ns vimsical.vcs.state.files
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.data.splittable :as splittable]
   [vimsical.vcs.data.indexed.vector :as indexed]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.file :as file]))


;; * Spec
;; ** Singe file state

(s/def ::idx nat-int?)

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
        :ret  ::delta/prev-id)

(defn op-idx->op-id
  [{::keys [deltas id->idx] :as state} op-idx]
  (or (try
        (nth deltas op-idx)
        (catch Throwable _))
      (reduce-kv
       (fn [_ id idx]
         (when (= op-idx idx) (reduced id)))
       nil id->idx)))

(s/fdef op-id->op-idx
        :args (s/cat :state ::state :op-id ::delta/prev-id)
        :ret  ::idx)

(defn op-id->op-idx
  [{::keys [deltas id->idx] :as state} op-id]
  (cond
    ;; First delta
    (nil? op-id)    0
    ;; Moved to a new file
    (empty? deltas) 0
    :else
    (or (try (indexed/index-of deltas op-id) (catch Throwable _))
        (get id->idx op-id)
        (throw (ex-info "Op idx not found" {:op-id op-id :id->idx id->idx :deltas deltas})))))


;; ** State update

(s/fdef update-state
        :args (s/cat :state ::state :delta ::delta/delta)
        :ret  ::state)

(defmulti ^:private update-state
  (fn [state delta]
    (-> delta :op first)))

(defmethod update-state :default [state delta]
  (assert false delta)
  state)

(defmethod update-state :crsr/mv
  [{::keys [id->idx deltas string] :as state} {:keys [id] :as delta}]
  (s/assert ::state state)
  (let [op-id  (-> delta :op second)    ; use conform?
        op-idx (op-id->op-idx state op-id)]
    (s/assert
     ::state
     ;; If we think of the index as a caret, after say an insert, it would still
     ;; be position before the character, so when we retrieve
     {::id->idx (assoc id->idx id (inc op-idx))
      ::deltas  deltas
      ::string  string})))

(defmethod update-state :str/ins
  [{::keys [id->idx deltas string] :as state} {:keys [id] :as delta}]
  (s/assert ::state state)
  (let [op-id   (-> delta :op second) ; use conform?
        op-diff (-> delta :op (nth 2))
        op-idx  (op-id->op-idx state op-id)]
    (s/assert
     ::state
     {::id->idx (assoc id->idx id op-idx)
      ::deltas  (splittable/splice deltas op-idx (indexed/vec-by :id [delta]))
      ::string  (splittable/splice string op-idx op-diff)})))

(defmethod update-state :str/rem
  [{::keys [id->idx deltas string] :as state} {:keys [id] [_ op-id] :op :as delta}]
  (s/assert ::state state)
  (let [op-id  (-> delta :op second) ; use conform?
        op-idx (op-id->op-idx state op-id)]
    (try
      {::id->idx (assoc id->idx id op-idx)
       ::deltas  (splittable/omit deltas op-idx 1)
       ::string  (splittable/omit string op-idx 1)}
      (catch Throwable t
        (throw
         (ex-info "?" {[::update-cache :str/rem]
                       {:op-id  op-id
                        :op-idx op-idx
                        :delta  delta
                        :cache  state}}))))))

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
        state      (update file-cache file-id (fnil update-state empty-state) delta)]
    (assoc states id state)))

(s/fdef add-deltas
        :args (s/cat :states ::states :delta (s/every ::delta/delta))
        :ret ::states)

(defn add-deltas
  [states deltas]
  (reduce add-delta states deltas))

;; ** Adding deltas (when writing a vims)

;; Not handled here, see `vimsical.vcs.state.editor`
