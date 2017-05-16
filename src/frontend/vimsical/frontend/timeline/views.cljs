(ns vimsical.frontend.timeline.views
  (:refer-clojure :exclude [chunk])
  (:require
   [re-frame.core :as re-frame]
   [vimsical.common.util.core :as util]
   [vimsical.frontend.styles.color :as color]
   [vimsical.frontend.timeline.handlers :as handlers]
   [vimsical.frontend.timeline.subs :as subs]
   [vimsical.frontend.util.re-frame :refer [<sub]]
   [vimsical.frontend.vcr.handlers :as vcr.handlers]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.state.chunk :as chunk]))

(def timeline-height 100)
(def timeline-vertical-center (/ timeline-height 2))
(def master-branch-height (* 0.75 timeline-height))
(def child-branch-height timeline-height)

;;
;; * Mouse events helpers
;;

(def x-scroll-factor 10)
(def y-scroll-factor (* 5 x-scroll-factor))

(defn e->mouse-coords [e] [(.-clientX e) (.-clientY e)])
(defn e->mouse-deltas [e] [(.-deltaX e) (.-deltaY e)])

(defn scroll-event->timeline-offset
  [e]
  (letfn [(abs [x] (max x (- x)))
          (max-delta [e]
            (let [[dx dy] (e->mouse-deltas e)]
              (if (< (abs dx) (abs dy))
                [nil dy]
                [dx nil])))]
    (let [[x y] (max-delta e)]
      (if x
        (* x-scroll-factor x)
        (* y-scroll-factor y)))))

(defn coords-and-svg-node->timeline-position
  [coords svg-node]
  (letfn [(scale-coords [svg-node [x y]]
            (let [pt (.createSVGPoint svg-node)
                  sc (.matrixTransform
                      (doto pt (aset "x" x) (aset "y" y))
                      (.inverse (.getScreenCTM svg-node)))]
              [(.-x sc) (.-y sc)]))]
    (first
     (scale-coords svg-node coords))))

;;
;; * Handlers
;;

(defn on-mouse-enter [e]
  (re-frame/dispatch
   [::handlers/on-mouse-enter
    (e->mouse-coords e)
    coords-and-svg-node->timeline-position]))

(defn on-mouse-wheel [e]
  (.preventDefault e)                   ; Prevent history navigation
  (re-frame/dispatch
   [::handlers/on-mouse-wheel
    (scroll-event->timeline-offset e)]))

(defn on-mouse-move [e]
  (re-frame/dispatch
   [::handlers/on-mouse-move
    (e->mouse-coords e)
    coords-and-svg-node->timeline-position]))

(defn on-click [e]
  (re-frame/dispatch
   [::handlers/on-click
    (e->mouse-coords e)
    coords-and-svg-node->timeline-position
    [::vcr.handlers/step]]))

(defn on-mouse-leave [_]
  (re-frame/dispatch [::handlers/on-mouse-leave]))

;;
;; * Components
;;

(def clip-path-id (gensym "clip-path-id"))

(defn- file-color
  [{::file/keys [sub-type]}]
  (get color/type-colors-timeline sub-type))

(defn- chunk
  [timeline-dur abs-time file {::chunk/keys [depth duration branch-start? branch-end?]}]
  (let [left               abs-time
        right              (+ left duration)
        height             (case depth 0 master-branch-height 1 child-branch-height)
        half-height        (/ height 2)
        top                (- timeline-vertical-center half-height)
        bottom             (+ timeline-vertical-center half-height)
        bezel              12.5
        top-bezel          (+ top bezel)
        bottom-bezel       (- bottom bezel)
        horizontal-padding (* 0.007 timeline-dur)
        left-padding       (+ left horizontal-padding)
        right-padding      (- right horizontal-padding)
        fill               (file-color file)]

    [:polygon
     {:fill fill
      :points
            (cond
              (and branch-start? branch-end?)
              (util/space-join
               left bottom-bezel
               left top-bezel
               left-padding top
               right-padding top
               right top-bezel
               right bottom-bezel
               right-padding bottom
               left-padding bottom)

              branch-start?
              (util/space-join
               left bottom-bezel
               left top-bezel
               left-padding top
               right top
               right bottom
               left-padding bottom)

              branch-end?
              (util/space-join
               left top
               right-padding top
               right top-bezel
               right bottom-bezel
               right-padding bottom
               left bottom)

              :else
              (util/space-join
               left top
               right top
               right bottom
               left bottom))}]))

(defn- skimhead-line
  [clip-path-id]
  (when-some [skimhead (<sub [::subs/skimhead])]
    (let [playhead     (<sub [::subs/playhead])
          at-playhead? (and playhead (= (int skimhead) (int playhead)))]
      [:line.skimhead
       {:x1            skimhead :x2 skimhead
        :y1            0 :y2 "100%"
        :stroke-width  3
        :vector-effect "non-scaling-stroke"
        :stroke        (if at-playhead? "red" "white")
        :clip-path     (str "url(#" clip-path-id ")")
        :style         {:pointer-events "none"}}])))

(defn- playhead-line
  [clip-path-id]
  (when-some [playhead (<sub [::subs/playhead])]
    [:line.playhead
     {:x1            playhead :x2 playhead
      :y1            0 :y2 "100%" :stroke-width 3
      :vector-effect "non-scaling-stroke"
      :stroke        "black"
      :clip-path     (str "url(#" clip-path-id ")")
      :style         {:pointer-events "none"}}]))

(defn chunks []
  (let [timeline-dur       (<sub [::subs/duration])
        chunks-by-abs-time (<sub [::subs/chunks-by-absolute-start-time])
        chunks-html        (doall
                            (for [[abs-time {::chunk/keys [id file-id] :as c}] chunks-by-abs-time
                                  :let [file (<sub [::vcs.subs/file file-id])]]
                              ^{:key id}
                              [chunk timeline-dur abs-time file c]))]
    [:g [:defs [:clipPath {:id clip-path-id} chunks-html]]
     chunks-html]))

(defn svg-ref-handler
  [node]
  (if (nil? node)
    (re-frame/dispatch [::handlers/dispose-svg])
    (re-frame/dispatch [::handlers/register-svg node])))

(defn timeline []
  (let [duration (<sub [::subs/duration])
        left     0
        right    0]
    [:div.timeline
     [:div.scaler
      [:svg.timeline-chunks
       {:ref                   svg-ref-handler
        :view-box              (util/space-join left right duration timeline-height)
        :preserve-aspect-ratio "none meet"
        :style                 {:width "100%" :height "100%"}
        :on-mouse-enter        on-mouse-enter
        :on-wheel              on-mouse-wheel
        :on-mouse-move         on-mouse-move
        :on-click              on-click
        :on-mouse-leave        on-mouse-leave}
       [chunks]
       [playhead-line clip-path-id]
       [skimhead-line clip-path-id]]]]))