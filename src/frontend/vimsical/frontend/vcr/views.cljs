(ns vimsical.frontend.vcr.views
  (:require
   [re-com.core :refer [v-box h-box box]]
   [vimsical.frontend.views.splits :refer [n-h-split n-v-split]]
   [vimsical.frontend.timeline.views :refer [timeline]]
   [vimsical.frontend.live-preview.views :refer [live-preview]]
   [vimsical.frontend.code-editor.views :refer [code-editor]]
   [vimsical.frontend.views.shapes :as shapes]
   [reagent.core :as r]
   [vimsical.frontend.util.dom :as util.dom :refer-macros [e-> e-handler]]
   [vimsical.common.util.util :as util]))

(defn wrap-editor [{:keys [file/sub-type]}]
  {:file/sub-type sub-type
   :editor        ^{:key sub-type} [code-editor sub-type]
   :hidden?       false})

(defn visible-editors [files editors-by-type]
  (->> files
       (remove :file/hidden?)
       (map :file/sub-type)
       (select-keys editors-by-type)
       vals
       (mapv :editor)))

(defn editors-by-type [files]
  (->> files
       (map wrap-editor)
       (util/project :file/sub-type)))

;;
;; * Temp
;;

(def files
  [{:file/sub-type :html
    :file/hidden?  false}
   {:file/sub-type :css
    :file/hidden?  false}
   {:file/sub-type :javascript
    :file/hidden?  false}])

;;
;; * Components
;;

(defn- speed-triangle [dir]
  (fn [opts]
    [:svg
     (merge {:view-box "0 0 100 100"} opts)
     (shapes/triangle {:origin          [50 50]
                       :height          100
                       :stroke-linejoin "round"
                       :stroke-width    15
                       :rotate          (case dir
                                          :left -90
                                          :right 90)})]))

(def triangle-left (speed-triangle :left))
(def triangle-right (speed-triangle :right))

(defn- speed-control []
  (let [speed-range [1.0 1.5 1.75 2 2.25 2.5]
        speed       (r/atom (first speed-range))]
    (fn []
      [:div.control.speed
       (triangle-left {:class    "icon speed-triangle decrease"
                       :on-click (e-handler :decrease)})
       (str @speed "x")
       (triangle-right {:class    "icon speed-triangle increase"
                        :on-click (e-handler :increase)})])))

(defn- playback-control []
  (let [playing? (r/atom false)]
    (fn []
      [:div.control.play-pause
       (if-not @playing?
         [:svg
          {:class    "icon play"        ;; rename to button
           :view-box "0 0 100 100"
           :on-click (e-handler (reset! playing? true))}
          (shapes/triangle {:origin          [50 50]
                            :height          100
                            :stroke-linejoin "round"
                            :stroke-width    15
                            :rotate          90})]
         [:svg {:class    "icon pause"  ;; rename to button
                :view-box "0 0 90 100"
                :on-click (e-handler (reset! playing? false))}
          (let [attrs {:y 0 :width 30 :height 100 :rx 5 :ry 5}]
            [:g
             [:rect (merge {:x 0} attrs)]
             [:rect (merge {:x 60} attrs)]])])])))

(defn- playback []
  [:div.playback [playback-control] [timeline] [speed-control]])

(defn- editor-tabs [files-subs]
  [:div.editor-tabs
   (for [{:file/keys [sub-type hidden?]} @files-subs]
     [:div.editor-tab
      {:class (when hidden? "disabled")
       :key   sub-type}
      [:div.tab-checkbox
       {:class    (name sub-type)
        :style    (cond-> {:cursor :pointer}
                    hidden? (assoc :color :grey))
        :on-click (e-handler
                   (swap! files-subs
                          util/update-when
                          (fn [file] (= (:file/sub-type file) sub-type))
                          update :file/hidden? not))}]])])

(defn vcr []
  (let [files-subs      (r/atom files)
        editors-by-type (editors-by-type files)
        playing?        false]
    (fn []
      (let [visible-editors (visible-editors @files-subs editors-by-type)]
        [v-box
         :class "vcr"
         :size "100%"
         :children
         [[playback]
          [n-h-split
           :panels [[live-preview]
                    [n-v-split
                     :height "100%"
                     :splitter-size "30px"
                     :panels visible-editors
                     :margin "0"]]
           :splitter-child [editor-tabs files-subs]
           :splitter-size "34px"
           :initial-split 60
           :margin "0"]]]))))