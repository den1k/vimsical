(ns vimsical.frontend.util.preprocess.babel
  (:require [vimsical.frontend.util.preprocess.core :refer [preprocess]]))

(defn format-result [res]
  {:code       (.-code res)
   :ast        (.-ast res)
   :source-map (.-map res)})

(defmethod preprocess :babel
  ([_ string opts]
   (format-result (js/Babel.transform string (clj->js opts)))))
