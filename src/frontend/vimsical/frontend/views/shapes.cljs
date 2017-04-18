(ns vimsical.frontend.views.shapes
  (:require [clojure.string :as str]))

(defmulti polygon (fn [shape _] shape))

(defmethod polygon :triangle
  [_ {:keys [type origin height]
      :or   {origin [0 0] type :equilateral}}]
  {:pre [height]}
  (case type
    :equilateral
    (let [[origin-x origin-y] origin
          side-len    (->> (js/Math.sqrt 3) (/ 2) (* height))
          half-height (/ height 2)
          half-side   (/ side-len 2)]
      [[(- origin-x half-side) (+ origin-y half-height)]
       [origin-x (- origin-y half-height)]
       [(+ origin-x half-side) (+ origin-y half-height)]])))

;;
;; * SVG Components
;;

(defn triangle [{:keys [origin height rotate style stroke-width]
                 :or   {rotate 0 stroke-width 0}
                 :as   opts}]
  ;; unclear why x 1.5. Think it should be x 2. But this works better.
  (let [height     (- height (* stroke-width 1.5))
        points     (polygon :triangle
                            {:origin origin
                             :height height})
        points-str (->> (map (partial str/join ",") points)
                        (str/join " "))]
    [:polygon.triangle
     (merge
      {:points points-str
       :style  {:transform        (str "rotate(" rotate "deg)")
                :transform-origin "center"}}
      (dissoc opts :origin :height :rotate))]))
