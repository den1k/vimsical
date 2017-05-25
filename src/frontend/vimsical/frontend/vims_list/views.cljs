(ns vimsical.frontend.vims-list.views
  (:require
   [re-com.core :as re-com]
   [vimsical.frontend.util.re-frame :refer [<sub]]
   [vimsical.frontend.util.dom :refer-macros [e> e-> e->>]]
   [vimsical.frontend.user.subs :as user.subs]
   [vimsical.frontend.vims-list.subs :as subs]
   [vimsical.vims :as vims]
   [vimsical.frontend.views.popovers :as popovers]
   [reagent.core :as reagent]
   [vimsical.frontend.live-preview.views :as live-preview.views]))

(defn vims-list-item [{:as vims :keys [db/uid] ::vims/keys [title]}]
  (let [show-delete-tooltip? (reagent/atom false)]
    [:div.vims-list-item.jsb.ac
     {:on-click (e> (prn "open vims"))}
     [live-preview.views/live-preview
      {:static? true
       :branch  (<sub [::subs/vims-preview-branch vims])}]
     ; todo (vims-preview vims)
     (if false                          ; todo delete-warning?
       [:div.delete-warning.dc
        [:div.warning "Are you sure?"]
        [:div.actions.jsa.asc
         #_(button
            (om/computed
             {}
             {:title    "Delete"
              :on-click #(om/transact! this `[(vims/delete ~vims)])}))
         #_(button
            (om/computed
             {}
             {:title    "Cancel"
              :on-click #(om/transact! this `[(vims/toggle-delete-warning ~vims)])}))]]
       [:div.vims-title-and-delete.jsb.ac
        [:div.vims-title title]
        [:div.delete-button
         {:on-mouse-enter (e> (reset! show-delete-tooltip? true))
          :on-mouse-leave (e> (reset! show-delete-tooltip? false))}
         [popovers/tooltip
          {:showing? show-delete-tooltip?
           :label    :delete
           :anchor   [:div.delete-x "+"]}]]])]))

(defn vims-list []
  (let [vimsae (<sub [::subs/vimsae])]
    [:div.vims-list
     [:div.list-box
      [:div.list
       (for [{:as vims key :db/uid} vimsae]
         ^{:key key} [vims-list-item vims])]]]))

(defn vims-list-popover [{:keys [showing? anchor]}]
  [popovers/popover
   {:anchor          anchor
    :showing?        showing?
    :position-offset 70
    :child           [vims-list]}])