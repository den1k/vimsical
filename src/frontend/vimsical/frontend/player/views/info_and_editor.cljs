(ns vimsical.frontend.player.views.info-and-editor
  (:require [vimsical.frontend.views.splits :as splits]
            [reagent.core :as reagent]
            [re-com.core :as re-com]
            [vimsical.frontend.ui.subs :as ui.subs]
            [vimsical.frontend.util.dom :as util.dom :refer-macros [e> e->]]
            [vimsical.frontend.util.re-frame :refer [<sub]]
            [vimsical.frontend.live-preview.views :refer [live-preview]]
            [vimsical.frontend.code-editor.views :refer [code-editor]]
            [vimsical.frontend.util.content :as util.content]
            [vimsical.frontend.views.icons :as icons]
            [vimsical.frontend.user.views :as user.views]
            [vimsical.frontend.app.subs :as app.subs]
            [vimsical.frontend.vcs.subs :as vcs.subs]
            [vimsical.frontend.player.subs :as subs]
            [vimsical.frontend.player.handlers :as handlers]
            [vimsical.frontend.timeline.subs :as timeline.subs]
            [vimsical.vcs.branch :as branch]
            [vimsical.vcs.file :as file]
            [clojure.string :as string]
            [vimsical.common.util.core :as util :include-macros true]
            [vimsical.vims :as vims]
            [vimsical.user :as user]
            [vimsical.frontend.player.views.elems :as elems]))

(defn active-file-badge [{:keys [file]}]
  (when file
    (let [{::file/keys [sub-type]} file
          title (name (get {:javascript :js} sub-type sub-type))]
      [:div.active-file-type
       {:class title}
       (string/upper-case title)])))

(defn info-and-editor-container []
  (let [info-hover? (reagent/atom true)
        desc        nil #_(util.content/lorem-ipsum 2)]
    (reagent/create-class
     {:render
      (fn [c]
        (let [{:keys [vims show-info? orientation read-only? ui-key] :as opts}
              (reagent/props c)
              {::vims/keys [title owner]} (<sub [::app.subs/vims-info vims])
              files                 (<sub [::vcs.subs/files vims])
              temp-first-file       (first files)
              active-file-uid       (or (<sub [::subs/active-file-uid vims])
                                        (:db/uid temp-first-file))
              uid->file             (util/project :db/uid files)
              active-file           (get uid->file active-file-uid)
              file-uid->code-editor (util/map-vals
                                     (fn [fl]
                                       ^{:key (:db/uid fl)}
                                       [code-editor {:vims        vims
                                                     :file        fl
                                                     :ui-key      ui-key
                                                     :read-only?  read-only?
                                                     :compact?    true
                                                     :no-history? true}])
                                     uid->file)
              on-mobile?            (<sub [::ui.subs/on-mobile?])
              show-info?            (and show-info?
                                         (not (<sub [::timeline.subs/active? vims])))]
          [:div.info-and-editor-panel.dc
           {:class (when show-info? "show-info")
            :on-mouse-enter
                   (e> (reset! info-hover? true))
            :on-mouse-out
                   (e> (when-not (util.dom/view-contains-related-target? c e)
                         (reset! info-hover? false)))}
           [:div.info
            [:div.header.ac
             [user.views/avatar {:user owner}]
             [:div.title-and-creator
              [:div.title.truncate (or title "Untitled")]
              [:div.creator.truncate (user/full-name owner)]]]
            (when desc
              [:div.desc desc])]
           (get file-uid->code-editor active-file-uid)
           [:div.logo-and-file-type.bar
            (let [logo-and-type [icons/logo-and-type {:on-click (e> (util.dom/open "https://vimsical.com"))}]]
              (case orientation
                :landscape logo-and-type
                :portrait logo-and-type #_[elems/explore opts]))
            (active-file-badge {:file active-file})]]))})))
