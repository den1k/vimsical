(ns vimsical.vcs.delta
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.op :as op]))

;; * Consistency checks

(declare op-type op-id)

(defn ops-point-to-str-id?
  "Return true if every op-id in `deltas` points to a delta id whose op was of
  str/ins type."
  [deltas]
  (letfn [(str-op? [delta] (= :str/ins (op-type delta)))]
    (let [str-deltas-by-id (->> deltas (filter str-op?) (group-by :id))]
      (reduce
       (fn [_ delta]
         (if-some [op-id' (op-id delta)]
           (if (nil? (get str-deltas-by-id op-id'))
             (reduced false)
             true)
           true))
       {} deltas))))


;; * Spec

(def current-version 0.3)

(s/def ::id uuid?)
(s/def ::prev-id (s/nilable ::id))
(s/def ::op ::op/op)
(s/def ::pad nat-int?)
(s/def ::branch-id uuid?)
(s/def ::file-id uuid?)
(s/def ::version number?)
(s/def ::timestamp nat-int?)
(s/def ::meta (s/keys :req-un [::timestamp ::version]))

(s/def ::delta
  (s/keys :req-un [::branch-id ::file-id ::prev-id ::id ::op ::pad]
          :opt-un [::meta]))

(s/def ::new-delta
  (s/keys :req-un [::branch-id ::file-id ::prev-id ::id ::op ::pad ::timestamp]))

(s/fdef new-delta
        :args (s/cat :new-delta ::new-delta)
        :ret  ::delta)

(defn new-delta
  [{:keys [branch-id file-id prev-id id op pad timestamp]}]
  {:branch-id branch-id
   :file-id   file-id
   :prev-id   prev-id
   :id        id
   :op        op
   :pad       pad
   :meta      {:timestamp timestamp :version current-version}})


(defn op-id [{[_ id] :op}] id)
(defn op-type [{[op] :op}] op)
(defn op-diff [{[_ _ diff] :op}] (assert diff "Not a :str/ins") diff)
(defn op-amt  [{[_ _ amt] :op}] (assert amt "Not :str/rem") amt)
