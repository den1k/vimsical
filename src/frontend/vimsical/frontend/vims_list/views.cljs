(ns vimsical.frontend.vims-list.views
  (:require
   [re-frame.core :as re-frame]
   [vimsical.frontend.app.handlers :as app.handlers]
   [vimsical.frontend.live-preview.views :as live-preview.views]
   [vimsical.frontend.util.dom :refer-macros [e>]]
   [vimsical.frontend.util.re-frame :refer [<sub]]
   [vimsical.frontend.views.popovers :as popovers]
   [vimsical.frontend.vims-list.subs :as subs]
   [re-frame.interop :as interop]
   [vimsical.vims :as vims]))

(defn vims-list-item
  [{::vims/keys [title] :as vims}]
  (let [show-delete-tooltip? (interop/ratom false)]
    [:div.vims-list-item.jsb.ac
     {:on-click
      (e> (re-frame/dispatch [::app.handlers/open-vims vims]))}
     [live-preview.views/live-preview
      {:static?        true
       :from-snapshot? true
       :vims           vims}]
     [:div.vims-title-and-delete.jsb.ac
      [:div.vims-title (or title "Untitled")]
      [:div.delete-button
       {:on-mouse-enter (e> (reset! show-delete-tooltip? true))
        :on-mouse-leave (e> (reset! show-delete-tooltip? false))}
       [popovers/tooltip
        {:showing? show-delete-tooltip?
         :label    :delete
         :anchor   [:div.delete-x "+"]}]]]]))

(defn vims-list []
  (let [vimsae (<sub [::subs/vimsae])]
    [:div.vims-list.dc.ac
     [:div.list-box
      [:div.list
       (if (seq vimsae)
         (for [{:as vims key :db/uid} vimsae]
           ^{:key key} [vims-list-item vims])
         ^{:key ::empty} [:h1 "No vims Placeholder"])]]]))
