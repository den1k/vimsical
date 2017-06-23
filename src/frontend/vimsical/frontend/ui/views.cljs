(ns vimsical.frontend.ui.views
  (:require [reagent.core :as reagent]
            [re-frame.interop :as interop]
            [vimsical.frontend.util.dom :as util.dom]))

(defn visibility
  [{:keys [once? visible? on-visibility-change]
    :or   {visible? (interop/ratom false) once? false}}
   child]
  (let [node           (atom)
        stop?          (interop/ratom false)
        scroll-handler (fn [_]
                         (let [vis? (util.dom/visible-in-viewport? @node)]
                           (when (and once? vis?) (reset! stop? true))
                           (when (and on-visibility-change (not= @visible? vis?))
                             (on-visibility-change vis?))
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

(defn viewport-ratio
  ([dispatch-fn child] [viewport-ratio false child])
  ([dispatch-fn mirror? child]
   (fn []
     (let [node           (atom)
           ratio-fn       #(cond->> (util.dom/viewport-ratio @node)
                             mirror? (- 1))
           scroll-handler (fn [_] (dispatch-fn (ratio-fn)))]
       (reagent/create-class
        {:component-did-mount
         (fn [c]
           (reset! node (reagent/dom-node c))
           (scroll-handler nil)         ; init
           (.addEventListener js/window "scroll" scroll-handler))
         :component-will-unmount
         (fn [_] (.removeEventListener js/window "scroll" scroll-handler))
         :render
         (fn [_] [:span child])})))))