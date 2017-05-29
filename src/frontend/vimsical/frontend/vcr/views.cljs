(ns vimsical.frontend.vcr.views
  (:require
   [re-com.core :as re-com]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [vimsical.common.util.core :as util :include-macros true]
   [vimsical.frontend.code-editor.views :refer [code-editor]]
   [vimsical.frontend.live-preview.views :refer [live-preview]]
   [vimsical.frontend.timeline.subs :as timeline.subs]
   [vimsical.frontend.timeline.views :refer [timeline]]
   [vimsical.frontend.util.dom :as util.dom :refer-macros [e>]]
   [vimsical.frontend.util.re-frame :as util.re-frame :refer [<sub]]
   [vimsical.frontend.vcr.handlers :as handlers]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.frontend.views.shapes :as shapes]
   [vimsical.frontend.views.splits :as splits]
   [vimsical.frontend.app.subs :as app.subs]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.file :as file]))

(defn visible-files [files]
  (remove ::file/hidden? files))

(defn views-for-files [files editor-components-by-type]
  (->> (map ::file/sub-type files)
       (map editor-components-by-type)))

(defn editor-components-by-file-type [editor-components]
  (util/project ::file/sub-type editor-components))

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
   ;; TODO move hidden state to ui db
   (for [{::file/keys [sub-type hidden?]} files]
     [:div.editor-tab
      {:class (when hidden? "disabled")
       :key   sub-type}
      [:div.tab-checkbox
       {:class (name sub-type)
        :style (cond-> {:cursor :pointer}
                 hidden? (assoc :color :grey))
        ;; :on-click (e>
        ;;            (swap! files
        ;;                   util/update-when
        ;;                   (fn [file] (= (::file/sub-type file) sub-type))
        ;;                   update ::file/hidden? not))
        }]])])

(defn- editor-header [{::file/keys [sub-type] :as file}]
  (let [title (get {:html "HTML" :css "CSS" :javascript "JS"} sub-type)]
    [:div.editor-header {:key sub-type :class sub-type}
     [:div.title title]]))

(defn- editor-components [{::file/keys [sub-type] :as file}]
  {::file/sub-type sub-type
   :editor-header  ^{:key sub-type} [editor-header file]
   :editor         ^{:key sub-type} [code-editor
                                     {:vims           (<sub [::app.subs/vims])
                                      :file           file
                                      :editor-reg-key :vcr/editors}]})

(defn vcr []
  (let [vims                   (<sub [::app.subs/vims])
        branch                 (<sub [::vcs.subs/branch vims])
        files                  (<sub [::vcs.subs/files vims])
        editor-comps           (->> files (map editor-components) editor-components-by-file-type)
        visible-files          (visible-files files)
        visi-components        (views-for-files visible-files editor-comps)
        visible-editor-headers (mapv :editor-header visi-components)
        visible-editors        (mapv :editor visi-components)]
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
                 :panels visible-editors
                 :splitter-children visible-editor-headers
                 :margin "0"]]
       :splitter-child [editor-tabs files]
       :splitter-size "34px"
       :initial-split 60
       :margin "0"]]]))
