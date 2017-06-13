(ns vimsical.vcs.delta
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.vcs.op :as op]))

;;
;; * Consistency checks
;;

(declare op-type op-uid)

(defn ops-point-to-str-id?
  "Return true if every op-id in `deltas` points to a delta uid whose op was of
  str/ins type."
  [deltas]
  (letfn [(str-op? [delta] (= :str/ins (op-type delta)))]
    (let [str-deltas-by-id (->> deltas (filter str-op?) (group-by :uid))]
      (letfn [(valid-op-uid? [op-uid]
                (cond
                  (vector? op-uid) (every? valid-op-uid? op-uid)
                  (nil? op-uid)    true
                  :else            (some? (get str-deltas-by-id op-uid))))]
        (reduce
         (fn [_ delta]
           (or (valid-op-uid? (op-uid delta)) (reduced false)))
         {} deltas)))))

;;
;; * Spec
;;

(def current-version 0.3)

(s/def ::uid uuid?)
(s/def ::prev-uid (s/nilable ::uid))
(s/def ::op ::op/op)
(s/def ::pad nat-int?)
(s/def ::branch-uid uuid?)
(s/def ::file-uid uuid?)
(s/def ::version number?)
(s/def ::timestamp nat-int?)
(s/def ::meta (s/keys :req-un [::timestamp ::version]))

(s/def ::delta
  (s/keys :req-un [::branch-uid ::file-uid ::prev-uid ::uid ::op ::pad]
          :opt-un [::meta]))

;;
;; * Constructor
;;

(s/def ::new-delta
  (s/keys :req-un [::branch-uid ::file-uid ::prev-uid ::uid ::op ::pad ::timestamp]))

(s/fdef new-delta
        :args (s/cat :new-delta ::new-delta)
        :ret  ::delta)

(defn new-delta
  [{:keys [branch-uid file-uid prev-uid uid op pad timestamp]}]
  {:branch-uid branch-uid
   :file-uid   file-uid
   :prev-uid   prev-uid
   :uid        uid
   :op         op
   :pad        pad
   :meta       {:timestamp timestamp :version current-version}})

;;
;; * Accessors
;;

(defn op-uid  [{[_ uid] :op}] uid)
(defn op-type [{[op] :op}] op)
(defn op-diff [{[_ _ diff] :op}] (assert diff "Not a :str/ins") diff)
(defn op-amt  [{[_ _ amt] :op}] (assert amt "Not :str/rem") amt)
