(ns vimsical.frontend.views.splits
  (:require [re-frame.core :as rf]
            [re-com.core :refer [h-box box border] :as rc]
            [re-com.util :refer [get-element-by-id sum-scroll-offsets]]
            [re-com.box :refer [flex-child-style flex-flow-style]]
            [re-com.validate :refer [string-or-hiccup? number-or-string? html-attr? css-style?] :refer-macros [validate-args-macro]]
            [re-com.splits :as splits]
            [reagent.core :as reagent]
            [vimsical.frontend.util.dom :as util.dom :refer-macros [e-> e-handler]]))

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

;; TODO call update-percs on resize

(defn n-v-split
  "Returns markup for a vertical layout component for an arbitrary number of
  panels. "
  [& {:keys [panels size width height on-split-change initial-split splitter-size
             splitter-child splitter-children margin]
      :or   {size "auto" initial-split 50 splitter-size "8px" margin "8px"}
      :as   args}]
  {:pre [(validate-args-macro n-v-split-args-desc args "n-v-split")]}
  (let [container-id           (gensym "n-v-split-")
        dragging?              (reagent/atom false)         ;; is the user dragging the splitter (mouse is down)?
        over?                  (reagent/atom false)         ;; is the mouse over the splitter, if so, highlight it
        panel-count            (count panels)
        percs                  (reagent/atom (vec (repeat panel-count (/ 100 panel-count))))
        prev-mouse-y           (reagent/atom 0)
        splitter-idx           (reagent/atom nil)
        splitter-children      (if splitter-children
                                 (do (assert (= panel-count (count splitter-children)))
                                     splitter-children)
                                 (repeat panel-count splitter-child))
        splitter-size-int      (js/parseInt splitter-size)
        splitters-count        (cond-> panel-count (nil? splitter-child) dec)
        splitters-height       (* splitters-count splitter-size-int)

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

        <html>?                #(= % (.-documentElement js/document)) ;; test for the <html> element

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
                                 (.preventDefault event)    ;; stop selection of text during drag
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
                                 (merge {:class (str "rc-v-split display-flex " class)
                                         :id    container-id
                                         :style (merge (flex-child-style size)
                                                       (flex-flow-style "column nowrap")
                                                       {:margin margin
                                                        :width  width}
                                                       style)}
                                        (when in-drag?      ;; only listen when we are dragging
                                          {:on-mouse-up    (e-handler (stop-drag))
                                           :on-mouse-move  (e-> mousemove)
                                           :on-mouse-leave (e-> mouseleave)})
                                        attr))

        make-split-panel-attrs (fn [idx perc]
                                 {:key   (str "split-panel-" idx)
                                  :style {:height (str "calc(" perc "% + " splitter-size ")")}})

        make-panel-attrs       (fn [idx class in-drag? perc]
                                 {:class (str "display-flex " class)
                                  :style (merge
                                          {
                                           :height     "100%"
                                           :background :cadetblue}
                                          (when in-drag? {:pointer-events "none"}))})

        make-splitter-attrs    (fn [idx class]
                                 {:class         (str "display-flex " class)
                                  :on-mouse-down (e-> (mousedown idx))
                                  :on-mouse-over (e-handler (mouseover-split idx))
                                  :on-mouse-out  (e-handler (mouseout-split))
                                  :style         (merge {:height splitter-size}
                                                        {:cursor "row-resize"}
                                                        (when (current-over? idx)
                                                          {:background-color "#f8f8f8"}))})

        update-percs           (fn [c]
                                 ;; get exact height after render to compute
                                 ;; accurate percentages for panels including
                                 ;; splitters
                                 (let [el                  (reagent/dom-node c)
                                       h                   (.-clientHeight el)
                                       h-splits            (- h splitters-height)
                                       accurate-panel-perc (/ (* 100 (/ h-splits h)) panel-count)]
                                   (reset! percs (vec (repeat panel-count accurate-panel-perc)))))]

    (reagent/create-class
     {:component-did-mount
      update-percs
      :reagent-render
      (fn
        [& {:keys [panels class style attr]}]
        [:div (make-container-attrs class style attr @dragging?)
         (doall
          (map
           (fn [idx perc panel splitter-child]
             [:div (make-split-panel-attrs idx perc)
              [:div (make-splitter-attrs idx "re-v-split-splitter")
               (cond
                 splitter-child splitter-child
                 (not (zero? idx)) [splits/drag-handle :horizontal @over?])]
              [:div (make-panel-attrs idx "re-v-split-top" @dragging? perc)
               panel]])
           (range (count panels)) @percs panels splitter-children))])})))

(def custom-hv-split-args-desc
  (conj splits/hv-split-args-desc
        {:name :splitter-child :required false :type "hiccup" :validate-fn string-or-hiccup? :description "markup to go into each splitter instead of drag-handle"}))

(defn h-split
  "Returns markup for a horizontal layout component"
  [& {:keys [size width height on-split-change initial-split splitter-size splitter-child margin]
      :or   {size "auto" initial-split 50 splitter-size "8px" margin "8px"}
      :as   args}]
  {:pre [(validate-args-macro custom-hv-split-args-desc args "h-split")]}
  (let [container-id         (gensym "h-split-")
        split-perc           (reagent/atom (js/parseInt initial-split)) ;; splitter position as a percentage of width
        dragging?            (reagent/atom false)           ;; is the user dragging the splitter (mouse is down)?
        over?                (reagent/atom false)           ;; is the mouse over the splitter, if so, highlight it

        stop-drag            (fn []
                               (when on-split-change (on-split-change @split-perc))
                               (reset! dragging? false))

        calc-perc            (fn [mouse-x]                  ;; turn a mouse y coordinate into a percentage position
                               (let [container  (get-element-by-id container-id) ;; the outside container
                                     offsets    (sum-scroll-offsets container) ;; take any scrolling into account
                                     c-width    (.-clientWidth container) ;; the container's width
                                     c-left-x   (.-offsetLeft container) ;; the container's left X
                                     relative-x (+ (- mouse-x c-left-x) (:left offsets))] ;; the X of the mouse, relative to container
                                 (* 100.0 (/ relative-x c-width)))) ;; do the percentage calculation

        mousemove            (fn [event]
                               (reset! split-perc (calc-perc (.-clientX event))))

        mousedown            (fn [event]
                               (.preventDefault event)      ;; stop selection of text during drag
                               (reset! dragging? true))

        mouseleave           (fn [event]
                               (stop-drag))

        mouseover-split      #(reset! over? true)           ;; true CANCELs mouse-over (false cancels all others)
        mouseout-split       #(reset! over? false)

        make-container-attrs (fn [class style attr in-drag?]
                               (merge {:class (str "rc-h-split display-flex " class)
                                       :id    container-id
                                       :style (merge (flex-child-style size)
                                                     (flex-flow-style "row nowrap")
                                                     {:margin margin
                                                      :width  width
                                                      :height height}
                                                     style)}
                                      (when in-drag?        ;; only listen when we are dragging
                                        {:on-mouse-up    (e-handler (stop-drag))
                                         :on-mouse-move  (e-> mousemove)
                                         :on-mouse-leave (e-> mouseleave)})
                                      attr))

        make-panel-attrs     (fn [class in-drag? percentage]
                               {:class (str "display-flex " class)
                                :style (merge (flex-child-style (str percentage " 1 0px"))
                                              {:overflow "hidden"} ;; TODO: Shouldn't have this...test removing it
                                              (when in-drag? {:pointer-events "none"}))})

        make-splitter-attrs  (fn [class]
                               {:class         (str "display-flex " class)
                                :on-mouse-down (e-> mousedown)
                                :on-mouse-over (e-handler (mouseover-split))
                                :on-mouse-out  (e-handler (mouseout-split))
                                :style         (merge (when-not splitter-child
                                                        (flex-child-style (str "0 0 " splitter-size)))
                                                      {:cursor "col-resize"}
                                                      (when @over? {:background-color "#f8f8f8"}))})]

    (fn
      [& {:keys [panel-1 panel-2 class style attr]}]
      [:div (make-container-attrs class style attr @dragging?)
       [:div (make-panel-attrs "rc-h-split-top" @dragging? @split-perc)
        panel-1]
       [:div (make-splitter-attrs "rc-h-split-splitter")
        (or
         splitter-child
         [splits/drag-handle :vertical @over?])]
       [:div (make-panel-attrs "rc-h-split-bottom" @dragging? (- 100 @split-perc))
        panel-2]])))
