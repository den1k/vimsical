(ns vimsical.vcs.state.vims.files.delta
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.data.indexed.vector :as indexed.vector]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.file :as file]))

;; * Notes

;; given the current-delta-id, branch? and file
;; - file string
{:<delta-id> {:<file> "string"}}

;; given the current-dela-id, file, branch?
;; - delta id of idx position in string
{:<delta-id>
 {:<file>
  ;; imagine deltas...
  [#:vimsical.vcs.edit-event{:op :str/ins, :idx 0, :diff "s"}
   #:vimsical.vcs.edit-event{:op :str/ins, :idx 1, :diff "t"}
   #:vimsical.vcs.edit-event{:op :str/ins, :idx 2, :diff "r"}
   #:vimsical.vcs.edit-event{:op :str/ins, :idx 3, :diff "i"}
   #:vimsical.vcs.edit-event{:op :str/ins, :idx 4, :diff "n"}
   #:vimsical.vcs.edit-event{:op :str/ins, :idx 5, :diff "g"}]}}


(s/def ::string string?)
(s/def ::deltas (s/and ::indexed.vector/indexed-vector (s/every ::delta/delta)))
(s/def ::cache (s/keys :req [::string ::deltas]))

;; TODO rename to :vimsical.vcs.state.file/by-delta-id

(s/def ::delta-index
  (s/map-of ::delta/id (s/map-of ::file/id ::cache)))

(defmulti update-cache
  (fn [cache delta]
    (-> delta :op first)))

(defn new-cache []
  {::string ""
   ::deltas (indexed.vector/indexed-vector-by :id)})

(def empty-cache (new-cache))

(defn op-idx->op-id
  [{::keys [deltas id->idx] :as cache} op-idx]
  (or (try
        (nth deltas op-idx)
        (catch Throwable _))
      (reduce-kv
       (fn [_ id idx]
         (when (= op-idx idx) (reduced id)))
       nil id->idx)))

(defn op-id->op-idx
  [{::keys [deltas id->idx] :as cache} op-id]
  (cond
    ;; First delta
    (nil? op-id)    0
    ;; Moved to a new file
    (empty? deltas) 0
    :else
    (or (try (indexed.vector/index-of deltas op-id) (catch Throwable _))
        (get id->idx op-id)
        (throw (ex-info "Op idx not found" {:op-id op-id :id->idx id->idx :deltas deltas})))))

(s/def ::id->idx (s/map-of (s/nilable ::delta/id) nat-int?))
(s/def ::deltas (s/and indexed.vector/indexed-vector? (s/every ::delta/delta)))
(s/def ::string string?)
(s/def ::cache (s/keys :req [::id->idx ::deltas ::string]))

(defn splice-string
  [n s diff]
  (str (subs s 0 n) diff (subs s n)))

(defmethod update-cache :default [cache delta]
  (assert false delta)
  cache)

(defmethod update-cache :crsr/mv
  [{::keys [id->idx deltas string] :as cache} {:keys [id] :as delta}]
  (s/assert ::cache cache)
  (let [op-id  (-> delta :op second)    ; use conform?
        op-idx (op-id->op-idx cache op-id)]
    (s/assert
     ::cache
     ;; If we think of the index as a caret, after say an insert, it would still
     ;; be position before the character, so when we retrieve
     {::id->idx (assoc id->idx id (inc op-idx))
      ::deltas  deltas
      ::string  string})))

(defmethod update-cache :str/ins
  [{::keys [id->idx deltas string] :as cache} {:keys [id] :as delta}]
  (s/assert ::cache cache)
  (let [op-id   (-> delta :op second) ; use conform?
        op-diff (-> delta :op (nth 2))
        op-idx  (op-id->op-idx cache op-id)]
    (s/assert
     ::cache
     {::id->idx (assoc id->idx id op-idx)
      ::deltas  (indexed.vector/splice-at op-idx deltas (indexed.vector/indexed-vector-by :id [delta]))
      ::string  (splice-string op-idx string op-diff)})))

(defmethod update-cache :str/rem
  [{::keys [id->idx deltas string] :as cache} {:keys [id] [_ op-id] :op :as delta}]
  (s/assert ::cache cache)
  (let [op-id  (-> delta :op second) ; use conform?
        op-idx (op-id->op-idx cache op-id)]
    (try
      {::id->idx (assoc id->idx id op-idx)
       ::deltas  (into
                  (first (indexed.vector/split-at op-idx deltas))
                  (second (indexed.vector/split-at (inc op-idx) deltas)))
       ::string  (str
                  (subs string 0 op-idx)
                  (subs string op-idx))}
      (catch Throwable t
        (throw
         (ex-info "?" {[::update-cache :str/rem]
                       {:op-id  op-id
                        :op-idx op-idx
                        :delta  delta
                        :cache  cache}}))))))

(defn new-delta-index ([] {}))

(defn add-delta
  [delta-index {:keys [file-id id prev-id] :as delta}]
  (let [file-cache (or (get delta-index prev-id) {file-id empty-cache})
        ;; _ (println prev-id file-id file-cache)
        ;; _ (assert (get file-cache file-id)  file-id)
        cache      (update file-cache file-id (fnil update-cache empty-cache) delta)]
    (assoc delta-index id cache)))

(s/def ::delta-index
  (s/map-of ::delta/id (s/map-of ::file/id ::cache)))

(s/fdef add-deltas
        :args (s/cat :delta-index ::delta-index :delta (s/every ::delta/delta)))

(defn add-deltas
  [delta-index deltas]
  (reduce add-delta delta-index deltas))

(defn get-deltas
  ([delta-index delta-id]
   (get delta-index delta-id))
  ([delta-index delta-id file-id]
   (get-in delta-index [delta-id file-id])))
