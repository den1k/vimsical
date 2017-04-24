(ns vimsical.frontend.vcr.views
  (:require
   [re-com.core :as re-com]
   [vimsical.frontend.views.splits :as splits]
   [vimsical.frontend.timeline.views :refer [timeline]]
   [vimsical.frontend.live-preview.views :refer [live-preview]]
   [vimsical.frontend.code-editor.views :refer [code-editor]]
   [vimsical.frontend.views.shapes :as shapes]
   [reagent.core :as reagent]
   [vimsical.frontend.util.dom :as util.dom :refer-macros [e-> e>]]
   [vimsical.common.util.core :as util]
   [vimsical.frontend.vcr.handlers :as handlers]
   [vimsical.frontend.app.data :as app.data]
   [vimsical.frontend.vcr.data :as data]))

(defn visible-files [files]
  (remove :file/hidden? files))

(defn views-for-files [files editor-components-by-type]
  (->> (map :file/sub-type files)
       (map editor-components-by-type)))

(defn editor-components-by-file-type [editor-components]
  (util/project :file/sub-type editor-components))

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
        speed       (reagent/atom (first speed-range))]
    (fn []
      [:div.control.speed
       (triangle-left {:class    "icon speed-triangle decrease"
                       :on-click (e> :decrease)})
       (str @speed "x")
       (triangle-right {:class    "icon speed-triangle increase"
                        :on-click (e> :increase)})])))

(defn- playback-control []
  (let [playing? (reagent/atom false)]
    (fn []
      [:div.control.play-pause
       (if-not @playing?
         [:svg
          {:class    "icon play"        ;; rename to button
           :view-box "0 0 100 100"
           :on-click (e> (reset! playing? true))}
          (shapes/triangle {:origin          [50 50]
                            :height          100
                            :stroke-linejoin "round"
                            :stroke-width    15
                            :rotate          90})]
         [:svg {:class    "icon pause"  ;; rename to button
                :view-box "0 0 90 100"
                :on-click (e> (reset! playing? false))}
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
        :on-click (e>
                   (swap! files-subs
                          util/update-when
                          (fn [file] (= (:file/sub-type file) sub-type))
                          update :file/hidden? not))}]])])

(defn- editor-header [{:keys [file/sub-type] :as file}]
  (let [title (get {:html "HTML" :css "CSS" :javascript "JS"} sub-type)]
    [:div.editor-header {:key sub-type :class sub-type}
     [:div.title title]]))

(defn- editor-components [{:keys [file/sub-type] :as file}]
  {:file/sub-type
   sub-type
   :editor-header
   ^{:key sub-type} [editor-header file]
   :editor
   ^{:key sub-type} [code-editor
                     {:id             sub-type
                      :file-type      sub-type
                      :editor-reg-key ::data/editors}]})

(defn vcr []
  (let [files-subs   (reagent/atom files)
        editor-comps (->> files
                          (map editor-components)
                          editor-components-by-file-type)
        playing?     false]
    (fn []
      (let [visible-files          (visible-files @files-subs)
            visi-components        (views-for-files visible-files editor-comps)
            visible-editor-headers (mapv :editor-header visi-components)
            visible-editors        (mapv :editor visi-components)]
        [re-com/v-box
         :class "vcr"
         :size "100%"
         :children
         [[playback]
          [splits/n-h-split
           :class "live-preview-and-editors"
           :panels [[live-preview]
                    [splits/n-v-split
                     :height "100%"
                     :splitter-size "31px"
                     :panels visible-editors
                     :splitter-children visible-editor-headers
                     :margin "0"]]
           :splitter-child [editor-tabs files-subs]
           :splitter-size "34px"
           :initial-split 60
           :margin "0"]]]))))