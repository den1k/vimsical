(ns vimsical.frontend.ui.views
  (:require [reagent.core :as reagent]
            [re-frame.interop :as interop]
            [vimsical.frontend.util.dom :as util.dom]))

(defn visibility
  [{:keys [child once?]
    :or   {once? false}}]
  (let [node           (atom)
        visible?       (interop/ratom false)
        stop?          (interop/ratom false)
        scroll-handler (fn [_]
                         (let [vis? (util.dom/visible-in-viewport? @node)]
                           (when (and once? vis?) (reset! stop? true))
                           (reset! visible? vis?)))]
    (reagent/create-class
     {:component-did-mount
      (fn [c]
        (reset! node (reagent/dom-node c))
        (scroll-handler nil)            ; init
        (.addEventListener js/window "scroll" scroll-handler))
      :component-will-unmount
      (fn [_] (.removeEventListener js/window "scroll" scroll-handler))
      :render
      (fn [_]
        [:span.visibility
         {:class (if (or @visible? @stop?) "visible" "none")}
         child])})))

(defn viewport-ratio [pct-ratom child]
  (let [node           (atom)
        scroll-handler (fn [_]
                         (reset! pct-ratom (util.dom/viewport-ratio @node)))]
    (reagent/create-class
     {:component-did-mount
      (fn [c]
        (reset! node (reagent/dom-node c))
        (scroll-handler nil)            ; init
        (.addEventListener js/window "scroll" scroll-handler))
      :component-will-unmount
      (fn [_] (.removeEventListener js/window "scroll" scroll-handler))
      :render
      (fn [_] [:span child])})))
