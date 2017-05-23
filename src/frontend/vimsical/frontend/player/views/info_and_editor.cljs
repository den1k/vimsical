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
            [vimsical.vcs.branch :as branch]
            [vimsical.vcs.file :as file]
            [clojure.string :as string]
            [vimsical.common.util.core :as util]
            [vimsical.vims :as vims]
            [vimsical.user :as user]
            [vimsical.frontend.player.views.elems :as elems]))

(defn active-file-badge [{:keys [file]}]
  (let [{::file/keys [sub-type]} file]
    [:div.active-file-type
     {:class sub-type}
     (-> sub-type name string/upper-case)]))

(defn user-full-name [{::user/keys [first-name last-name]}]
  (util/space-join first-name last-name))

(defn info-and-editor-container []
  (let [show-info? (reagent/atom true)
        desc       (util.content/lorem-ipsum 2)]
    (reagent/create-class
     {:render
      (fn [c]
        (let [{:as vims ::vims/keys [title owner]} (<sub [::app.subs/vims-info])
              files                 (<sub [::vcs.subs/files])
              temp-first-file       (first files)
              active-file-uid       (or (<sub [::subs/active-file-uid])
                                        (:db/uid temp-first-file))
              uid->file             (util/project :db/uid files)
              active-file           (get uid->file active-file-uid)
              file-uid->code-editor (util/map-vals
                                     (fn [fl]
                                       ^{:key (:db/uid fl)}
                                       [code-editor {:file     fl
                                                     :compact? true}])
                                     uid->file)]
          [:div.info-and-editor-panel.dc
           {:on-mouse-enter (e>
                             (reset! show-info? true))
            :on-mouse-out   (e>
                             (when-not (util.dom/view-contains-related-target? c e)
                               (reset! show-info? false)))}
           [:div.info
            {:class (when-not @show-info? "pan-out")}
            [:div.header.ac
             [user.views/avatar
              {:user owner}]
             [:div.title-and-creator
              [:div.title.truncate title]
              [:div.creator.truncate (user-full-name owner)]]]
            (when desc
              [:div.desc desc])]
           (get file-uid->code-editor active-file-uid)
           [:div.logo-and-file-type.bar
            (case (<sub [::ui.subs/orientation])
              :landscape [icons/logo-and-type]
              :portrait [elems/edit-on-vimsical])
            (active-file-badge {:file active-file})]]))})))
