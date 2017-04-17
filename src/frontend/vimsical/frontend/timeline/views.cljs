(ns vimsical.frontend.timeline.views
  (:require [vimsical.common.util.core :as util])
  (:refer-clojure :exclude [chunk]))

(defonce tl-height 100)
(defonce tl-middle (/ tl-height 2))
(defonce master-branch-height (* 0.75 tl-height))
(defonce child-branch-height tl-height)

;;
;; * Components
;;

(def clip-path-id (gensym "clip-path-id"))

(defn linear-gradient [id & stops-attrs]
  [:linearGradient
   {:id id}
   (for [[offset col op] stops-attrs]
     [:stop
      {:key    (str id "__" offset)
       :offset offset
       :style  {:stop-color   col
                :stop-opacity (or op 1)}}])])

(defn infinity-gradients [{:keys [at-start? at-end? window-dur start-offset]}]
  (letfn [(style [on-off]
            {:opacity        (if on-off 0 1)
             :transition     "opacity 0.5s ease"
             :width          "5%"
             :height         "100%"
             :pointer-events "none"})]
    [:g
     [:defs
      (linear-gradient "start-grad" ["0%" "white" 1] ["100%" "white" 0])
      (linear-gradient "end-grad" ["0%" "white" 0] ["100%" "white" 1])]
     [:rect.grad.infinity-start
      {:x    start-offset :y 0
       :fill "url(#start-grad)" :style (style at-start?)}]
     [:rect.grad.infinity-end
      {:x    (+ start-offset (* 0.95 window-dur)) :y 0
       :fill "url(#end-grad)" :style (style at-end?)}]]))

(defn chunk [{:keys [chunk opts]}]
  (let [{:keys [window-dur handlers]} opts
        {:keys [lx rx fill
                chunk/branch-depth chunk/branch-start? chunk/branch-end?]} chunk
        height       (case branch-depth
                       0 master-branch-height
                       1 child-branch-height)
        half-height  (/ height 2)
        t-edge       (- tl-middle half-height)
        b-edge       (+ tl-middle half-height)
        edge-h       12.5               ; (max branch-1) / 2
        t-chip       (+ t-edge edge-h)
        b-chip       (- b-edge edge-h)
        edge-offset  (* 0.007 window-dur)
        left-offset  (+ lx edge-offset)
        right-offset (- rx edge-offset)]

    [:polygon
     ;; TODO event handlers
     {:fill   fill
      :points (cond
                (and branch-start? branch-end?)
                (util/space-join
                 lx b-chip
                 lx t-chip
                 left-offset t-edge
                 right-offset t-edge
                 rx t-chip
                 rx b-chip
                 right-offset b-edge
                 left-offset b-edge)

                branch-start?
                (util/space-join
                 lx b-chip
                 lx t-chip
                 left-offset t-edge
                 rx t-edge
                 rx b-edge
                 left-offset b-edge)

                branch-end?
                (util/space-join
                 lx t-edge
                 right-offset t-edge
                 rx t-chip
                 rx b-chip
                 right-offset b-edge
                 lx b-edge)

                :else
                (util/space-join
                 lx t-edge
                 rx t-edge
                 rx b-edge
                 lx b-edge))}]))

(defn timeline []
  (let [chunks           []

        ;; timeline attrs
        start-offset     0
        window-dur       2000

        ;; playhead & skimhead
        skimhead         1000
        playhead         2000
        playhead-locked? false
        show-playhead?   true]
    (fn []
      (let [chunks-html (for [c chunks]
                          ^{:key (:db/uuid c)}
                          [chunk
                           {:chunk c
                            :opts  {:window-dur window-dur}
                            ;:handlers {:on-click        click-handler
                            ;           :on-double-click double-click-handler
                            ;           :on-mouse-move   mouse-move-handler}
                            }])]
        [:div.timeline
         [:div.scaler
          [:svg.timeline-chunks
           {:ref                   "svg"
            ;:view-box              (vimsical.common.util.core/space-join
            ;                        start-offset 0 window-dur tl-height)
            :preserve-aspect-ratio "none meet"
            :style                 {:width "100%" :height "100%"}
            ;:on-wheel              mouse-wheel-handler
            ;:on-mouse-enter        mouse-enter-handler
            ;:on-mouse-leave        mouse-leave-handler
            }
           [:defs [:clipPath {:id clip-path-id} chunks-html]]
           chunks-html
           #_(infinity-gradients
              {:at-start?    at-start?
               :at-end?      at-end?
               :start-offset start-offset
               :window-dur   window-dur}
              )
           [:line.skimhead
            {:x1            skimhead :x2 skimhead
             :y1            0 :y2 "100%"
             :stroke-width  3
             :vector-effect "non-scaling-stroke"
             :stroke        "white"
             :clip-path     (str "url(#" clip-path-id ")")
             :style         {:visibility     (when-not skimhead "hidden")
                             :pointer-events "none"}}]
           [:line.playhead
            {:x1            playhead :x2 playhead
             :y1            0 :y2 "100%" :stroke-width 3
             :vector-effect "non-scaling-stroke"
             :stroke        (if (= skimhead playhead) "red" "black")
             :clip-path     (when-not playhead-locked?
                              (str "url(#" clip-path-id ")"))
             :style         {:visibility     (when-not show-playhead? "hidden")
                             :pointer-events "none"}}]]]]))))