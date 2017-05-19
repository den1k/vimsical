(ns vimsical.frontend.player.views.info-and-editor
  (:require [vimsical.frontend.views.splits :as splits]
            [reagent.core :as reagent]
            [re-com.core :as re-com]
            [vimsical.frontend.util.dom :as util.dom :refer-macros [e> e->]]
            [vimsical.frontend.util.re-frame :refer [<sub]]
            [vimsical.frontend.live-preview.views :refer [live-preview]]
            [vimsical.frontend.code-editor.views :refer [code-editor]]
            [vimsical.frontend.util.content :as util.content]
            [vimsical.frontend.views.icons :as icons]
            [vimsical.frontend.user.views :as user]
            [vimsical.frontend.app.subs :as app.subs]
            [vimsical.frontend.vcs.subs :as vcs.subs]
            [vimsical.frontend.player.subs :as subs]
            [vimsical.frontend.player.handlers :as handlers]
            [vimsical.vcs.branch :as branch]
            [vimsical.vcs.file :as file]
            [clojure.string :as string]
            [vimsical.common.util.core :as util]))

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
              author-full-name     (->> author
                                        ((juxt :user/first-name :user/last-name))
                                        (string/join " "))
              {:as           branch
               ::branch/keys [files]} (<sub [::vcs.subs/branch])
              temp-first-file      (-> branch ::branch/files first)
              active-file-id       (<sub [::subs/active-file-id])
              active-file          (util/ffilter #(= active-file-id (:db/id %)) files)
              sub-type->file       (util/project ::file/sub-type files)
              file-id->code-editor (util/into-hashmap
                                    :db/id
                                    (fn [fl]
                                      ^{:key (:db/id fl)}
                                      [code-editor {:file     fl
                                                    :compact? true}])
                                    files)]
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
           (get file-id->code-editor active-file-id)
           [:div.logo-and-file-type.bar
            [icons/logo-and-type]
            ;; todo dynamic
            [:div.active-file-type.css
             ;; todo dynamic
             "HTML"]]]))})))
