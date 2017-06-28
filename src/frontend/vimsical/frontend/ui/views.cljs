(ns vimsical.frontend.ui.views
  (:require [reagent.core :as reagent]
            [re-frame.interop :as interop]
            [vimsical.frontend.util.dom :as util.dom]
            [vimsical.common.util.core :as util]))

(defn visibility
  [{:keys [once? visible? ratio range range-pred in-range? on-visibility-change]
    :or   {visible?  (interop/ratom false)
           ratio     (interop/ratom nil)
           in-range? (interop/ratom false)
           once?     false}}
   child]
  (let [node           (atom)
        stop?          (interop/ratom false)

        check-vis      (fn []
                         (let [vis? (util.dom/visible-in-viewport? @node)]
                           (when (and once? vis?) (reset! stop? true))
                           (when (and on-visibility-change (not= @visible? vis?))
                             (on-visibility-change vis?))
                           (reset! visible? vis?)))

        check-range    (fn []
                         (when (or range range-pred ratio)
                           (let [vp-ratio (util.dom/viewport-ratio @node)]
                             (reset! ratio vp-ratio)
                             (when (or range range-pred)
                               (reset! in-range?
                                       (if range-pred
                                         (range-pred vp-ratio)
                                         (util/between? range vp-ratio)))))))
        scroll-handler (fn [_]
                         (check-range)
                         (check-vis))]
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
         {:class (util/space-join
                  (if @visible? "visible" "none")
                  (when @in-range? "in-range"))}
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