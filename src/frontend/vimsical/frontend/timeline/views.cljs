(ns vimsical.frontend.timeline.views
  (:refer-clojure :exclude [chunk])
  (:require
   [re-frame.core :as re-frame]
   [vimsical.common.util.core :as util]
   [vimsical.frontend.styles.color :as color]
   [vimsical.frontend.timeline.handlers :as handlers]
   [vimsical.frontend.util.re-frame :refer [<sub]]
   [vimsical.frontend.vcr.subs :as vcr.subs]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.state.chunk :as chunk]))

(defonce timeline-height 100)
(defonce timeline-vertical-center (/ timeline-height 2))
(defonce master-branch-height (* 0.75 timeline-height))
(defonce child-branch-height timeline-height)


;;
;; * Handlers
;;

(defn xy-coords [e] [(.-clientX e) (.-clientY e)])

(defn scale-coords [svg-node [x y]]
  (let [pt (.createSVGPoint svg-node)
        _  (doto pt (aset "x" x) (aset "y" y))
        sc (.matrixTransform pt (.inverse (.getScreenCTM svg-node)))]
    [(.-x sc) (.-y sc)]))

(defn event->time [node e]
  (first (scale-coords node (xy-coords e))))

;; precise skimhead
(defn on-chunks-mouse-wheel [e])
;; set skimhead
(defn on-chunks-mouse-enter [e])
;; skimhead nil
(defn on-chunks-mouse-leave [e])


;; set skimhead
(defn on-chunk-mouse-move [e]
  (re-frame/dispatch [::handlers/chunk-mouse-move (xy-coords e) scale-coords]))

;;
;; * Components
;;

(def clip-path-id (gensym "clip-path-id"))

(defn- file-color
  [{::file/keys [sub-type] :as file}]
  (get color/type-colors-timeline sub-type))

(defn- chunk
  [timeline-duration abs-time file {::chunk/keys [depth duration branch-start? branch-end?]}]
  (let [left               abs-time
        right              (+ left duration)
        height             (case depth 0 master-branch-height 1 child-branch-height)
        half-height        (/ height 2)
        top                (- timeline-vertical-center half-height)
        bottom             (+ timeline-vertical-center half-height)
        bezel              12.5
        top-bezel          (+ top bezel)
        bottom-bezel       (- bottom bezel)
        horizontal-padding 0
        left-padding       (+ left horizontal-padding)
        right-padding      (- right horizontal-padding)
        fill               (file-color file)]
    [:polygon
     {:on-mouse-move on-chunk-mouse-move
      :fill          fill
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
  (let [skimhead (<sub [::vcr.subs/skimhead])]
    [:line.skimhead
     {:x1            skimhead :x2 skimhead
      :y1            0        :y2 "100%"
      :stroke-width  3
      :vector-effect "non-scaling-stroke"
      :stroke        "white"
      :clip-path     (str "url(#" clip-path-id ")")
      :style         {:visibility     (when-not skimhead "hidden")
                      :pointer-events "none"}}]))
(defn- playhead-line
  [clip-path-id]
  (let [playhead (<sub [::vcr.subs/playhead])
        skimhead (<sub [::vcr.subs/skimhead])]
    [:line.playhead
     {:x1            playhead :x2 playhead
      :y1            0        :y2 "100%" :stroke-width 3
      :vector-effect "non-scaling-stroke"
      :stroke        (if (= skimhead playhead) "red" "black")
      :clip-path     (str "url(#" clip-path-id ")")
      :style         {:pointer-events "none"}}]))

(defn chunks []
  (let [timeline-duration  (<sub [::vcs.subs/timeline-duration])
        chunks-by-abs-time (<sub [::vcs.subs/timeline-chunks-by-absolute-start-time])
        chunks-html
        (doall
         (for [[abs-time {::chunk/keys [id file-id] :as c}] chunks-by-abs-time
               :let                                         [file (<sub [::vcs.subs/file file-id])]]
           ^{:key id}
           [chunk timeline-duration abs-time file c]))]
    [:g [:defs [:clipPath {:id clip-path-id} chunks-html]]
     chunks-html]))

(defn svg-ref-handler
  [node]
  (if (nil? node)
    (re-frame/dispatch [::handlers/dispose-svg])
    (re-frame/dispatch [::handlers/register-svg node])))

(defn timeline []
  (let [timeline-duration (<sub [::vcs.subs/timeline-duration])
        left              0
        right             0]
    [:div.timeline
     [:div.scaler
      [:svg.timeline-chunks
       {:ref                   svg-ref-handler
        :view-box              (util/space-join left right timeline-duration timeline-height)
        :preserve-aspect-ratio "none meet"
        :style                 {:width "100%" :height "100%"}
        :on-wheel              on-chunks-mouse-wheel
        :on-mouse-enter        on-chunks-mouse-enter
        :on-mouse-leave        on-chunks-mouse-leave}
       [chunks]
       [playhead-line clip-path-id]
       [skimhead-line clip-path-id]]]]))
