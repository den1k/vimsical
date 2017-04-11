(ns vimsical.frontend.code-editor.views
  (:require [reagent.core :as r]))

(defn code-editor [file-type]
  (r/create-class
   {:component-did-mount
    (fn [c]
      (js/monaco.editor.create
       (r/dom-node c)
       #js {:value           (name file-type)
            :language        (name file-type)
            :automaticLayout true}))
    :reagent-render
    (fn [file-type]
      [:div {:style {:display    "flex"
                     :flex       "auto"
                     :background :tomato}}])}))


