(ns vimsical.common.core )

(defn =by
  ([f a]
   (fn [b]
     (= (f a) (f b))))
  ([f x y]   (= (f x) (f y)))
  ([f g x y] (= (f x) (g y))))

(defn some-val
  [f coll]
  (some (fn [x] (when (f x) x)) coll))
