(ns vimsical.frontend.styles.color
  (:require [thi.ng.color.core :refer [hsva as-css]]
            [vimsical.common.util.core :refer [map-vals]]))

(def type-colors-timeline
  {:html       "#F3997F"
   :css        "#9CDAF4"
   :javascript "#F5CC72"})

(def type->colors-editors
  {:html       "#F0805F"
   :css        "#32B2E9"
   :javascript "#F2BF4F"})

(defn type-style
  [type selector-prefix attr val]
  [(keyword (str (name selector-prefix) (name type)))
   {attr val}])

(defn type-child-style
  [type selector-prefix child-selector attr val]
  [(keyword (str (name selector-prefix) (name type)))
   [child-selector {attr val}]])

(defn type-class
  [type]
  (name type))

(defn- sketch-hsb->hsl [color-coll]
  (-> color-coll
      (update 0 / 360)
      (update 1 / 100)
      (update 2 / 100)
      hsva as-css deref))

(def colors
  (map-vals
   sketch-hsb->hsl
   {:darkgrey  [0 0 29]
    :grey      [0 0 60]
    :lightgrey [0 0 93]
    :darkblue  [226 15 25]
    :beatwhite [0 0 98]
    :facebook  [221 63 60]
    :shadow    [0 0 50]}))
