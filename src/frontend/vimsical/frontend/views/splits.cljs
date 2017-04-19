(ns vimsical.frontend.views.splits
  (:require [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [re-com.util :refer [get-element-by-id sum-scroll-offsets]]
            [re-com.box :refer [flex-child-style flex-flow-style]]
            [re-com.validate :refer [string-or-hiccup? number-or-string? html-attr? css-style?] :refer-macros [validate-args-macro]]
            [re-com.splits :as splits]
            [reagent.core :as reagent]
            [vimsical.common.util.core :as util]
            [vimsical.frontend.util.dom :as util.dom :refer-macros [e-> e>]]))

(def n-v-split-args-desc
  [{:name :panels :required true :type "vector" :validate-fn vector? :description "coll of markup to go into each panel"}
   {:name :splitter-child :required false :type "hiccup" :validate-fn string-or-hiccup? :description "markup to go into each splitter instead of drag-handle"}
   {:name :splitter-children :required false :type "vector" :validate-fn vector? :description "coll of markup to go into each splitter instead of drag-handle"}
   {:name :size :required false :default "auto" :type "string" :validate-fn string? :description [:span "applied to the outer container of the two panels. Equivalent to CSS style " [:span.bold "flex"] "." [:br] "Examples: " [:code "initial"] ", " [:code "auto"] ", " [:code "none"] ", " [:code "100px"] ", " [:code "2"] " or a generic triple of " [:code "grow shrink basis"]]}
   {:name :width :required false :type "string" :validate-fn string? :description "width of the outer container of the two panels. A CSS width style"}
   {:name :height :required false :type "string" :validate-fn string? :description "height of the outer container of the two panels. A CSS height style"}
   {:name :on-split-change :required false :type "double -> nil" :validate-fn fn? :description [:span "called when the user moves the splitter bar (on mouse up, not on each mouse move). Given the new " [:code ":panel-1"] " percentage split"]}
   {:name :initial-split :required false :default 50 :type "double | string" :validate-fn number-or-string? :description [:span "initial split percentage for " [:code ":panel-1"] ". Can be double value or string (with/without percentage sign)"]}
   {:name :splitter-size :required false :default "8px" :type "string" :validate-fn string? :description "thickness of the splitter"}
   {:name :margin :required false :default "8px" :type "string" :validate-fn string? :description "thickness of the margin around the panels"}
   {:name :class :required false :type "string" :validate-fn string? :description "CSS class names, space separated, applied to outer container"}
   {:name :style :required false :type "CSS style map" :validate-fn css-style? :description "CSS styles to add or override, applied to outer container"}
   {:name :attr :required false :type "HTML attr map" :validate-fn html-attr? :description [:span "HTML attributes, like " [:code ":on-mouse-move"] [:br] "No " [:code ":class"] " or " [:code ":style"] "allowed, applied to outer container"]}])

(defn n-v-split
  "Returns markup for a vertical layout component for an arbitrary number of
  panels. "
  [& {:keys [panels size width height on-split-change initial-split splitter-size
             splitter-child splitter-children margin]
      :or   {size "auto" initial-split 50 splitter-size "8px" margin "8px"}
      :as   args}]
  {:pre [(validate-args-macro n-v-split-args-desc args "n-v-split")]}
  (let [container-id           (gensym "n-v-split-")
        dragging?              (reagent/atom false) ;; is the user dragging the splitter (mouse is down)?
        over?                  (reagent/atom false) ;; is the mouse over the splitter, if so, highlight it
        panel-count            (count panels)
        percs                  (reagent/atom (vec (repeat panel-count (/ 100 panel-count))))
        prev-mouse-y           (reagent/atom 0)
        splitter-idx           (reagent/atom nil)
        splitter-size-int      (js/parseInt splitter-size)
        splitters-count        (cond-> panel-count (nil? splitter-child) dec)
        splitters-height       (* splitters-count splitter-size-int)

        make-splitter-children (fn [{:keys [panels splitter-child splitter-children]}]
                                 (let [panel-count (count panels)]
                                   (if splitter-children
                                     (do (assert (= (count panels)
                                                    (count splitter-children)))
                                         splitter-children)
                                     (repeat panel-count splitter-child))))

        stop-drag              (fn []
                                 (when on-split-change (on-split-change @percs))
                                 (when-not @over?
                                   (reset! over? false))
                                 (reset! dragging? false))

        calc-percs             (fn [delta]
                                 (let [container (get-element-by-id container-id) ;; the outside container
                                       c-height  (.-clientHeight container) ;; the container's height
                                       perc-amt  (* 100 (/ delta c-height))
                                       idx       @splitter-idx]
                                   (-> @percs
                                       (update idx - perc-amt)
                                       (update (dec idx) + perc-amt))))

        mouse-y-delta          (fn [mouse-y]
                                 (let [prev @prev-mouse-y]
                                   (reset! prev-mouse-y mouse-y)
                                   (- mouse-y prev)))

        mouseleave             (fn [event] (stop-drag))

        mousemove              (fn [event]
                                 (let [delta (mouse-y-delta (.-clientY event))]
                                   (when (not (zero? delta))
                                     (reset! percs (calc-percs delta)))))

        mousedown              (fn [event idx]
                                 (.preventDefault event) ;; stop selection of text during drag
                                 (reset! dragging? true)
                                 (reset! prev-mouse-y (.-clientY event)))

        mouseover-split        (fn [idx]
                                 (reset! over? true)
                                 (reset! splitter-idx idx))

        mouseout-split         #(when-not @dragging?
                                  (reset! over? false))

        current-over?          (fn [idx]
                                 (and @over? (= @splitter-idx idx)))

        make-container-attrs   (fn [class style attr in-drag?]
                                 (merge {:class (str "rc-n-v-split display-flex " class)
                                         :id    container-id
                                         :style (merge (flex-child-style size)
                                                       (flex-flow-style "column nowrap")
                                                       {:margin margin
                                                        :width  width}
                                                       style)}
                                        (when in-drag? ;; only listen when we are dragging
                                          {:on-mouse-up    (e> (stop-drag))
                                           :on-mouse-move  (e-> mousemove)
                                           :on-mouse-leave (e-> mouseleave)})
                                        attr))

        make-split-panel-attrs (fn [idx perc]
                                 {:key   (str "split-panel-" idx)
                                  :class "display-flex"
                                  :style (merge
                                          (flex-flow-style "column nowrap")
                                          (flex-child-style "auto")
                                          {:height (str "calc(" perc "% - "
                                                        splitter-size-int "px)")})})

        make-panel-attrs       (fn [idx class in-drag?]
                                 {:class (str "display-flex " class)
                                  :style (merge
                                          (flex-child-style size)
                                          (when in-drag? {:pointer-events "none"}))})

        make-splitter-attrs    (fn [idx resizable? class]
                                 (cond-> {:class (str "display-flex " class)
                                          :style {:flex-direction :column
                                                  :height         splitter-size}}
                                   resizable? (util/merge-1
                                               {:on-mouse-down (e-> (mousedown idx))
                                                :on-mouse-over (e> (mouseover-split idx))
                                                :on-mouse-out  (e> (mouseout-split))
                                                :style         (merge
                                                                {:cursor "row-resize"}
                                                                (when (current-over? idx)
                                                                  {:background "#f8f8f8"}))})))]

    (fn
      [& {:keys [panels class style attr splitter-children] :as opts}]
      (let [splitter-children (make-splitter-children opts)]
        [:div (make-container-attrs class style attr @dragging?)
         (doall
          (map
           (fn [idx perc panel splitter-child]
             (let [resizable? (not (zero? idx))]
               [:div (make-split-panel-attrs idx perc)
                [:div (make-splitter-attrs idx resizable? "rc-n-v-split-splitter")
                 (cond
                   splitter-child splitter-child
                   resizable? [splits/drag-handle :horizontal @over?])]
                [:div (make-panel-attrs idx "rc-n-v-split-panel" @dragging?)
                 panel]]))
           (range panel-count) @percs panels splitter-children))]))))

(defn n-h-split
  "Returns markup for a horizontal layout component"
  [& {:keys [panels size width height on-split-change initial-split class
             splitter-size splitter-child splitter-children margin]
      :or   {size "auto" initial-split 50 splitter-size "8px" margin "8px"}
      :as   args}]
  {:pre [(validate-args-macro n-v-split-args-desc args "h-split")]}
  (let [container-id         (gensym "n-h-split-")
        dragging?            (reagent/atom false) ;; is the user dragging the splitter (mouse is down)?
        over?                (reagent/atom false) ;; is the mouse over the splitter, if so, highlight it
        panel-count          (count panels)
        percs                (reagent/atom (vec (repeat panel-count (/ 100 panel-count))))
        prev-mouse-x         (reagent/atom 0)
        splitter-idx         (reagent/atom nil)
        splitter-count       (dec panel-count)
        default-splitter?    (and (nil? splitter-child) (nil? splitter-children))
        splitter-children    (cond
                               splitter-children (do (assert (= splitter-count (count splitter-children)))
                                                     splitter-children)
                               splitter-child (repeat splitter-count splitter-child)

                               default-splitter? (repeat splitter-count (fn [] [splits/drag-handle :vertical @over?])))
        splitter-size-int    (js/parseInt splitter-size)
        splitters-width      (* splitter-count splitter-size-int)

        stop-drag            (fn []
                               (when on-split-change (on-split-change @percs))
                               (reset! dragging? false))

        calc-percs           (fn [delta]
                               (let [container (get-element-by-id container-id) ;; the outside container
                                     c-width   (.-clientWidth container) ;; the container's width
                                     perc-amt  (* 100 (/ delta c-width))
                                     idx       @splitter-idx]
                                 (-> @percs
                                     (update idx - perc-amt)
                                     (update (dec idx) + perc-amt))))

        mouse-x-delta        (fn [mouse-x]
                               (let [prev @prev-mouse-x]
                                 (reset! prev-mouse-x mouse-x)
                                 (- mouse-x prev)))

        mousemove            (fn [event]
                               (let [delta (mouse-x-delta (.-clientX event))]
                                 (when (not (zero? delta))
                                   (reset! percs (calc-percs delta)))))

        mousedown            (fn [event idx]
                               (.preventDefault event) ;; stop selection of text during drag
                               (reset! dragging? true)
                               (reset! prev-mouse-x (.-clientX event)))

        mouseleave           (fn [event]
                               (stop-drag))

        mouseover-split      (fn [idx]
                               (when default-splitter?
                                 (reset! over? true))
                               (reset! splitter-idx idx))

        mouseout-split       #(reset! over? false)

        current-over?        (fn [idx]
                               (and @over? (= @splitter-idx idx)))

        make-container-attrs (fn [class style attr in-drag?]
                               (merge {:class (str "rc-n-h-split display-flex " class)
                                       :id    container-id
                                       :style (merge (flex-child-style size)
                                                     (flex-flow-style "row nowrap")
                                                     {:margin margin
                                                      :width  width
                                                      :height height}
                                                     style)}
                                      (when in-drag? ;; only listen when we are dragging
                                        {:on-mouse-up    (e> (stop-drag))
                                         :on-mouse-move  (e-> mousemove)
                                         :on-mouse-leave (e-> mouseleave)})
                                      attr))

        make-panel-attrs     (fn [idx class in-drag? perc]
                               {:class (str "display-flex " class)
                                :key   (str "panel-" idx)
                                :style (merge
                                        {:min-height "100%"
                                         :width      (str "calc(" perc "% - "
                                                          (/ splitter-size-int
                                                             (/ panel-count splitter-count)) "px)")}
                                        (when in-drag? {:pointer-events "none"}))})

        make-splitter-attrs  (fn [idx class]
                               {:class         (str "display-flex " class)
                                :key           (str "splitter-" idx)
                                :on-mouse-down (e-> (mousedown idx))
                                :on-mouse-over (e> (mouseover-split idx))
                                :on-mouse-out  (e> (mouseout-split))
                                :style         (merge {:width splitter-size}
                                                      {:cursor "col-resize"}
                                                      (when (current-over? idx)
                                                        {:background "#f8f8f8"}))})]

    (fn
      [& {:keys [panels class style attr]}]
      (let [tagged-panels    (map (fn [idx panel perc]
                                    [:panel (inc idx) panel perc])
                                  (range panel-count) panels @percs)
            tagged-splitters (map (fn [idx spl-ch]
                                    [:splitter (inc idx) spl-ch])
                                  (range splitter-count) splitter-children)
            interleaved      (util/interleave-all tagged-panels tagged-splitters)]
        [:div (make-container-attrs class style attr @dragging?)
         (doall
          (map
           (fn [[kind idx panel-or-splitter perc]]
             (case kind
               :panel
               [:div (make-panel-attrs idx "rc-n-h-split-panel" @dragging? perc)
                panel-or-splitter]
               :splitter
               [:div (make-splitter-attrs idx "rc-n-h-split-splitter")
                splitter-child]))
           interleaved))]))))