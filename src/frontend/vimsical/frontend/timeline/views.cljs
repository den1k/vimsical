(ns vimsical.frontend.timeline.views
  (:refer-clojure :exclude [chunk])
  (:require
   [re-frame.core :as re-frame]
   [vimsical.common.util.core :as util :include-macros true]
   [vimsical.frontend.styles.color :as color]
   [vimsical.frontend.timeline.handlers :as handlers]
   [vimsical.frontend.timeline.subs :as subs]
   [vimsical.frontend.util.re-frame :refer [<sub]]
   [vimsical.frontend.vcr.handlers :as vcr.handlers]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.state.chunk :as chunk]
   [vimsical.frontend.util.dom :as util.dom]))

(def timeline-height 100)
(def timeline-vertical-center (/ timeline-height 2))
(def master-branch-height (* 0.75 timeline-height))
(def child-branch-height timeline-height)

;;
;; * Mouse events helpers
;;

(def x-scroll-factor 10)
(def y-scroll-factor (* 5 x-scroll-factor))

(defn scroll-event->timeline-offset
  [e]
  (letfn [(abs [x] (max x (- x)))
          (max-delta [e]
            (let [[dx dy] (util.dom/e->mouse-deltas e)]
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

(defn on-mouse-enter [vims]
  (fn [e]
    (re-frame/dispatch
     [::handlers/on-mouse-enter
      vims
      (util.dom/e->mouse-coords e)
      coords-and-svg-node->timeline-position])))

(defn on-mouse-wheel [vims]
  (fn [e]
    (.preventDefault e)                 ; Prevent history navigation
    (re-frame/dispatch
     [::handlers/on-mouse-wheel
      vims
      (scroll-event->timeline-offset e)])))

(defn on-mouse-move [vims]
  (fn [e]
    (re-frame/dispatch
     [::handlers/on-mouse-move
      vims
      (util.dom/e->mouse-coords e)
      coords-and-svg-node->timeline-position])))

(defn on-click [vims]
  (fn [e]
    (re-frame/dispatch
     [::handlers/on-click
      vims
      (util.dom/e->mouse-coords e)
      coords-and-svg-node->timeline-position
      [::vcr.handlers/step]])))

(defn on-mouse-leave [vims]
  (fn [_]
    (re-frame/dispatch [::handlers/on-mouse-leave vims])))

(defn on-touch-move [vims]
  (fn [e]
    (on-click (util.dom/first-touch->e e))))

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

(defn- skimhead-line [{:keys [vims]}]
  (when-some [skimhead (<sub [::subs/skimhead vims])]
    (let [playhead     (<sub [::subs/playhead vims])
          at-playhead? (and playhead (= (int skimhead) (int playhead)))]
      [:line.skimhead
       {:x1            skimhead :x2 skimhead
        :y1            0 :y2 "100%"
        :stroke-width  3
        :vector-effect "non-scaling-stroke"
        :stroke        (if at-playhead? "red" "white")
        :clip-path     (str "url(#" clip-path-id ")")
        :style         {:pointer-events "none"}}])))

(defn- playhead-line [{:keys [vims]}]
  (when-some [playhead (<sub [::subs/playhead vims])]
    [:line.playhead
     {:x1            playhead :x2 playhead
      :y1            0 :y2 "100%" :stroke-width 3
      :vector-effect "non-scaling-stroke"
      :stroke        "black"
      :clip-path     (str "url(#" clip-path-id ")")
      :style         {:pointer-events "none"}}]))

(defn chunks [{:keys [vims]}]
  (let [timeline-dur       (<sub [::subs/duration vims])
        chunks-by-abs-time (<sub [::subs/chunks-by-absolute-start-time vims])
        chunks-html
        (doall
         (for [[abs-time {::chunk/keys [uid file-uid] :as c}] chunks-by-abs-time
               :let                                           [file (<sub [::vcs.subs/file file-uid])]]
           ^{:key uid}
           [chunk timeline-dur abs-time file c]))]
    [:g [:defs [:clipPath {:id clip-path-id} chunks-html]]
     chunks-html]))

(defn svg-ref-handler
  [node]
  (if (nil? node)
    (re-frame/dispatch [::handlers/dispose-svg])
    (re-frame/dispatch [::handlers/register-svg node])))

(defn timeline [{:keys [vims]}]
  (let [duration (<sub [::subs/duration vims])
        left     0
        right    0]
    [:div.timeline
     [:div.scaler
      [:svg.timeline-chunks
       {:ref                   svg-ref-handler
        :view-box              (util/space-join left right duration timeline-height)
        :preserve-aspect-ratio "none meet"
        :style                 {:width "100%" :height "100%"}
        :on-mouse-enter        (on-mouse-enter vims)
        :on-wheel              (on-mouse-wheel vims)
        :on-mouse-move         (on-mouse-move vims)
        :on-click              (on-click vims)
        :on-mouse-leave        (on-mouse-leave vims)
        :on-touch-move         (on-touch-move vims)}
       [chunks {:vims vims}]
       [playhead-line {:vims vims}]
       [skimhead-line {:vims vims}]]]]))
