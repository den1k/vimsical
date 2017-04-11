(ns vimsical.common.coll)

(defn index-by
  "Like `group-by` with single values."
  [f coll]
  (persistent!
   (reduce
    (fn [m v]
      (assoc! m (f v) v))
    (transient {}) coll)))

(defn unique-index-by
  "Like `group-by` with single values."
  [f coll]
  (persistent!
   (reduce
    (fn [m v]
      (let [k (f v)]
        (if (get m k)
          (throw
           (ex-info
            "Uniqueness constraint violation."
            {:k k :existing-value (get m k) :new-value v}))
          (assoc! m k v))))
    (transient {}) coll)))
