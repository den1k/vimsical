(ns vimsical.frontend.player.views.player
  (:require [vimsical.frontend.views.splits :as splits]
            [vimsical.frontend.player.views.elems :as elems]
            [reagent.core :as reagent]
            [re-com.core :as re-com]
            [vimsical.frontend.util.dom :as util.dom :refer-macros [e> e->]]
            [vimsical.frontend.util.re-frame :refer [<sub]]
            [vimsical.frontend.live-preview.views :as live-preview]
            [vimsical.frontend.code-editor.views :as code-editor]
            [vimsical.frontend.util.content :as util.content]
            [vimsical.frontend.views.icons :as icons]
            [vimsical.frontend.user.views :as user]
            [vimsical.frontend.app.subs :as app.subs]
            [vimsical.frontend.vcs.subs :as vcs.subs]
            [vimsical.frontend.timeline.subs :as timeline.subs]
            [vimsical.frontend.player.subs :as subs]
            [vimsical.frontend.player.views.timeline :refer [timeline]]
            [vimsical.frontend.player.handlers :as handlers]
            [vimsical.vcs.branch :as branch]
            [clojure.string :as string]
            [re-frame.core :as re-frame]))

(defn play-pause []
  (let [playing? (<sub [::timeline.subs/playing?])]
    [:svg.play-pause
     {:view-box "0 0 100 100"
      :on-click (e> (re-frame/dispatch [(if playing? ::handlers/pause ::handlers/play)]))}
     (if-not playing?
       [elems/play-symbol
        {:origin       [50 50]
         :height       100
         :stroke-width 20}]
       [elems/pause-symbol
        {:origin        [50 50]
         :height        100
         :bar-width     30
         :gap-width     60
         :border-radius 10}])]))

(defn central-play-button []
  (let [unset? (<sub [::subs/playback-unset?])]
    (when unset?
      [:div.play-button-overlay.jc.ac
       {:on-click (e> (re-frame/dispatch [::handlers/play]))}
       [elems/play-button]])))

(defn preview-container []
  (let [liked    (reagent/atom false)
        playing? (reagent/atom false)]
    (fn []
      (let [branch (<sub [::vcs.subs/branch])]
        [:div.preview-panel.jsb.dc
         [:div.bar.social

          [re-com/h-box
           :gap "40px"
           :children [[re-com/md-icon-button
                       :md-icon-name (if-not @liked "zmdi-favorite-outline" "zmdi-favorite")
                       :on-click (e> (swap! liked not)) :class "favorite"]
                      [re-com/md-icon-button
                       :md-icon-name "zmdi-share" :tooltip "share" :class "share"]
                      [re-com/md-icon-button
                       :md-icon-name "zmdi-time" :tooltip "watch later" :class "watch-later"]]]
          [:div.edit
           "Edit on Vimsical"]]
         [:div.preview-container.f1
          [central-play-button]
          [live-preview/live-preview {:branch branch}]]
         [re-com/h-box
          :class "bar timeline-container"
          :justify :center
          :align :center
          :gap "18px"
          :children [[play-pause]
                     [timeline]
                     [:div.speed-control
                      "1.5x"]]]]))))

(defn info-and-editor-container []
  (let [show-info? (reagent/atom true)
        desc       (util.content/lorem-ipsum 2)]
    (reagent/create-class
     {:render
      (fn [c]
        (let [{:as        vims
               :vims/keys [title
                           author]} (<sub [::app.subs/vims
                                           [:vims/title
                                            {:vims/author [:user/first-name
                                                           :user/last-name
                                                           :user/email]}]])
              author-full-name (->> author
                                    ((juxt :user/first-name :user/last-name))
                                    (string/join " "))
              branch           (<sub [::vcs.subs/branch])
              temp-first-file  (-> branch ::branch/files first)
              active-file-id   (<sub [::subs/active-file-id])]
          [:div.info-and-editor-panel.dc
           {:on-mouse-enter (e>
                             (reset! show-info? true))
            :on-mouse-out   (e>
                             (when-not (util.dom/view-contains-related-target? c e)
                               (reset! show-info? false)))}
           [:div.info
            {:class (when-not @show-info? "pan-out")}
            [:div.header.ac
             [user/avatar
              {:user author}]
             [:div.title-and-creator
              [:div.title.truncate title]
              [:div.creator.truncate author-full-name]]]
            (when desc
              [:div.desc desc])]
           [code-editor/code-editor {:file     temp-first-file
                                     :compact? true
                                     ;:read-only? true
                                     }]
           [:div.logo-and-file-type.bar
            [icons/logo-and-type]
            ;; todo dynamic
            [:div.active-file-type.css
             ;; todo dynamic
             "HTML"]]]))})))

(defn player []
  [:div.vimsical-frontend-player
   [splits/n-h-split
    :panels
    [[preview-container]
     [info-and-editor-container]]
    :splitter-size "1px"
    :splitter-child [elems/resizer]
    :initial-split 70
    :margin "0"]])
