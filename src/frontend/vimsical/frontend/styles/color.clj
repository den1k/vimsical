(ns vimsical.frontend.styles.color
  (:require [thi.ng.color.core :refer [hsva as-css]]
            [vimsical.common.util.core :refer [map-vals]]))

(def type-display-names
  {:text/html       "HTML"
   :text/css        "CSS"
   :text/javascript "JS"})

(def type-colors-timeline
  {:text/html       "#F3997F"
   :text/css        "#9CDAF4"
   :text/javascript "#F5CC72"
   :ui/pointer      "lightgrey"})

(def type-colors-editors
  {:text/html       "#F0805F"
   :text/css        "#32B2E9"
   :text/javascript "#F2BF4F"})

(defn type-display-name
  [type]
  (type-display-names type))

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
