(ns vimsical.frontend.player.views.preview
  (:require [vimsical.frontend.views.splits :as splits]
            [vimsical.frontend.player.views.elems :as elems]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [vimsical.frontend.util.dom :as util.dom :refer-macros [e> e->]]
            [vimsical.frontend.util.re-frame :refer [<sub]]
            [vimsical.frontend.live-preview.views :refer [live-preview]]
            [vimsical.frontend.vcs.subs :as vcs.subs]
            [vimsical.frontend.timeline.subs :as timeline.subs]
            [vimsical.frontend.player.subs :as subs]
            [vimsical.frontend.player.views.timeline :refer [timeline-bar]]
            [vimsical.frontend.player.handlers :as handlers]
            [vimsical.common.util.core :as util]
            [vimsical.frontend.views.icons :as icons]))

(defn central-play-button []
  (let [unset? (<sub [::subs/playback-unset?])]
    (when unset?
      [:div.play-button-overlay.jc.ac
       {:on-click (e> (re-frame/dispatch [::handlers/play]))}
       [elems/play-button]])))

(defn preview-container []
  [:div.preview-container.f1
   [central-play-button]
   [live-preview]])

(defn social-bar [{:keys [show-edit? show-logo?]}]
  {:pre [(not= show-edit? show-logo?)]}
  (let [liked (reagent/atom false)]
    (fn []
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
       (cond
         show-edit? [elems/edit-on-vimsical]
         show-logo? [icons/logo-and-type])])))

(defn preview-panel []
  [:div.preview-panel.jsb.dc
   [social-bar {:show-edit? true}]
   [preview-container]
   [timeline-bar]])
