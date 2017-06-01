(ns vimsical.frontend.vcr.views
  (:require
   [re-com.core :as re-com]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [vimsical.frontend.code-editor.views :refer [code-editor]]
   [vimsical.frontend.live-preview.views :refer [live-preview]]
   [vimsical.frontend.timeline.subs :as timeline.subs]
   [vimsical.frontend.timeline.views :refer [timeline]]
   [vimsical.frontend.util.dom :as util.dom :refer-macros [e>]]
   [vimsical.frontend.util.re-frame :as util.re-frame :refer [<sub]]
   [vimsical.frontend.vcr.handlers :as handlers]
   [vimsical.frontend.vcr.subs :as subs]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.frontend.views.shapes :as shapes]
   [vimsical.frontend.views.splits :as splits]
   [vimsical.vcs.file :as file]))

;;
;; * Temp
;;

(def files
  [{::file/sub-type :html
    ::file/hidden?  false}
   {::file/sub-type :css
    ::file/hidden?  false}
   {::file/sub-type :javascript
    ::file/hidden?  false}])

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

(defn- playback-control [{:keys [vims]}]
  (let [playing? (<sub [::timeline.subs/playing? vims])]
    [:div.control.play-pause
     (if-not playing?
       [:svg
        {:class    "icon play"          ;; rename to button
         :view-box "0 0 100 100"
         :on-click (e> (re-frame/dispatch [::handlers/play vims]))}
        (shapes/triangle {:origin          [50 50]
                          :height          100
                          :stroke-linejoin "round"
                          :stroke-width    15
                          :rotate          90})]
       [:svg {:class    "icon pause"    ;; rename to button
              :view-box "0 0 90 100"
              :on-click (e> (re-frame/dispatch [::handlers/pause vims]))}
        (let [attrs {:y 0 :width 30 :height 100 :rx 5 :ry 5}]
          [:g
           [:rect (merge {:x 0} attrs)]
           [:rect (merge {:x 60} attrs)]])])]))

(defn- playback [{:keys [vims]}]
  [:div.playback
   [playback-control {:vims vims}]
   [timeline {:vims vims}]
   [speed-control]])

(defn- editor-tabs [files]
  [:div.editor-tabs
   (doall
    (for [{:as file ::file/keys [sub-type]} files
          :let [hidden? (<sub [::subs/file-hidden? file])]]
      [:div.editor-tab
       {:class    (when hidden? "disabled")
        :key      sub-type
        :on-click (e> (re-frame/dispatch [::handlers/toggle-file-visibility file]))}
       [:div.tab-checkbox
        {:class (name sub-type)
         :style (when hidden? {:color :grey})}]]))])

(defn- editor-header [{::file/keys [sub-type] :as file}]
  (let [title (get {:html "HTML" :css "CSS" :javascript "JS"} sub-type)]
    [:div.editor-header {:key sub-type :class sub-type}
     [:div.title title]]))

(defn editor-headers [files]
  (mapv (fn [{:as file sub-type ::file/sub-type}]
          ^{:key sub-type} [editor-header file]) files))

(defn editors [vims files]
  (mapv (fn [{:as file sub-type ::file/sub-type}]
          ^{:key sub-type} [code-editor {:vims vims :file file}])
        files))

(defn vcr [{:keys [vims]}]
  (let [all-files      (<sub [::vcs.subs/files vims])
        visi-files     (<sub [::subs/visible-files vims])
        editor-headers (editor-headers visi-files)
        editors        (editors vims visi-files)]
    [re-com/v-box
     :class "vcr"
     :size "100%"
     :children
     [[playback {:vims vims}]
      [splits/n-h-split
       :class "live-preview-and-editors"
       :panels [[live-preview {:vims vims :error-catcher? false}]
                [splits/n-v-split
                 :height "100%"
                 :splitter-size "31px"
                 :panels editors
                 :splitter-children editor-headers
                 :margin "0"]]
       :splitter-child [editor-tabs all-files]
       :splitter-size "34px"
       :initial-split 60
       :margin "0"]]]))
