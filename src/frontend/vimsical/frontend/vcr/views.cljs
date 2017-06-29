(ns vimsical.frontend.vcr.views
  (:require
   [re-com.core :as re-com]
   [re-frame.core :as re-frame]
   [vimsical.frontend.code-editor.views :refer [code-editor]]
   [vimsical.frontend.live-preview.views :refer [live-preview]]
   [vimsical.frontend.timeline.subs :as timeline.subs]
   [vimsical.frontend.timeline.views :refer [timeline]]
   [vimsical.frontend.util.dom :as util.dom :refer-macros [e>]]
   [vimsical.frontend.util.re-frame :as util.re-frame :refer [<sub]]
   [vimsical.frontend.app.subs :as app.subs]
   [vimsical.frontend.vcr.handlers :as handlers]
   [vimsical.frontend.vcr.subs :as subs]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.frontend.user.subs :as user.subs]
   [vimsical.frontend.views.shapes :as shapes]
   [vimsical.frontend.views.splits :as splits]
   [vimsical.frontend.user.handlers :as user.handlers]
   [vimsical.vcs.file :as file]
   [vimsical.frontend.remotes.fx :as frontend.remotes.fx]
   [re-frame.interop :as interop]
   [vimsical.frontend.vcs.sync.handlers :as vcs.sync.handlers]
   [vimsical.frontend.vcs.sync.subs :as vcs.sync.subs]
   [vimsical.frontend.app.handlers :as app.handlers]
   [vimsical.common.util.core :as util]
   [vimsical.user :as user]
   [vimsical.vims :as vims]))

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
  (let [{:as            settings
         :settings/keys [playback-speed]
         :or            {playback-speed 1}} (<sub [::user.subs/settings])]
    [:div.control.speed
     (triangle-left
      {:class    "icon speed-triangle decrease"
       :on-click (e> (re-frame/dispatch
                      [::user.handlers/playback-speed settings :dec]))})
     (str playback-speed "x")
     (triangle-right
      {:class    "icon speed-triangle increase"
       :on-click (e> (re-frame/dispatch
                      [::user.handlers/playback-speed settings :inc]))})]))

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
          ^{:key sub-type} [code-editor {:ui-key :vcr :vims vims :file file}])
        files))

(defn preview-and-editors [{:keys [vims]}]
  (let [all-files      (<sub [::vcs.subs/files vims])
        visi-files     (<sub [::subs/visible-files vims])
        editor-headers (editor-headers visi-files)
        editors        (editors vims visi-files)]
    [splits/n-h-split
     :class "live-preview-and-editors"
     ;; XXX files are required to prevent a race condition, c.f. live-preview
     ;; component for a description of the problem
     :panels [[live-preview {:ui-key :vcr :vims vims :files all-files :error-catcher? false}]
              [splits/n-v-split
               :height "100%"
               :splitter-size "31px"
               :panels editors
               :splitter-children editor-headers
               :margin "0"]]
     :splitter-child [editor-tabs all-files]
     :splitter-size "34px"
     :initial-split 60
     :margin "0"]))

(defn- vims-sync-status []
  (let [vims-uid (:db/uid (<sub [::app.subs/vims [:db/uid]]))
        status   (<sub [::vcs.sync.subs/vims-sync-status vims-uid])]
    [:div.sync-status-wrapper.section
     (when-not (= :init status)
       [:div.sync-status.ac
        {:class (when (= status :success) "saved")}
        [:div.status-circle]
        (re-com/gap :size "8px")
        [:div.status-message
         (case status
           :waiting "unsaved changes"
           :saving "saving..."
           :success "saved")]])]))

(defn- author-credit []
  (let [{::vims/keys [owner]} (<sub [::app.subs/vims
                                     [{::vims/owner [::user/first-name
                                                     ::user/last-name]}]])]
    [:div.author-credit
     (util/space-join "by" (user/full-name owner))]))

(defn- options []
  [re-com/h-box
   :class "options section"
   :gap "5px"
   :justify :end
   :children
   [[:div.option
     {:on-click (e> (.stopPropagation e)
                    (re-frame/dispatch [::app.handlers/modal :modal/libs]))}
     "libs"]
    [:div.license.option
     {:on-click (e> (.stopPropagation e)
                    (re-frame/dispatch [::app.handlers/modal :modal/license]))}
     [:div.license-title "license"]]]])

(defn vcr-footer []
  [:div.vcr-footer.jsb.ac
   [vims-sync-status]
   [author-credit]
   [options]])

(defn vcr [{:keys [vims]}]
  [re-com/v-box
   :class "vcr"
   :size "100%"
   :children
   [[playback {:vims vims}]
    [preview-and-editors {:vims vims}]
    [vcr-footer]]])
