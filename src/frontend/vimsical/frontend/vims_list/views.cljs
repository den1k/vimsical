(ns vimsical.frontend.vims-list.views
  (:require
   [re-com.core :as re-com]
   [re-frame.core :as re-frame]
   [re-frame.interop :as interop]
   [vimsical.frontend.app.handlers :as app.handlers]
   [vimsical.frontend.live-preview.views :as live-preview.views]
   [vimsical.frontend.util.dom :refer-macros [e>]]
   [vimsical.frontend.util.re-frame :refer [<sub]]
   [vimsical.frontend.views.popovers :as popovers]
   [vimsical.frontend.vims-list.subs :as subs]
   [vimsical.frontend.vims.handlers :as vims.handlers]
   [vimsical.vims :as vims]))

(defn vims-list-item
  [{::vims/keys [title] :as vims}]
  {:pre [(:db/uid vims)]}
  (let [show-delete-tooltip? (interop/ratom false)]
    [:div.vims-list-item.jsb.ac
     {:on-click
      (e> (re-frame/dispatch [::app.handlers/open-vims vims]))}
     [live-preview.views/live-preview
      {:static?        true
       :ui-key         :vims-list
       :from-snapshot? true
       :vims           vims}]
     [:div.vims-title-and-delete.jsb.ac
      [:div.vims-title (or title "Untitled")]
      [:div.delete-button
       {:on-mouse-enter (e> (reset! show-delete-tooltip? true))
        :on-mouse-leave (e> (reset! show-delete-tooltip? false))
        :on-click       (e> (.stopPropagation e) (re-frame/dispatch [::vims.handlers/delete vims]))}
       [popovers/tooltip
        {:showing? show-delete-tooltip?
         :label    :delete
         :anchor   [:div.delete-x "+"]}]]]]))

(defn vims-list []
  (let [state (interop/ratom {:list-partition-idx 0})]
    (fn []
      (let [vimsae (<sub [::subs/vimsae {:per-page 5}])
            {:keys [list-partition-idx]} @state]
        [:div.vims-list.dc.ac
         [:div.list-box.jsb.ac
          [re-com/md-icon-button
           :style {:visibility (when (zero? list-partition-idx) :hidden)}
           :class "chevron"
           :on-click (e> (.stopPropagation e)
                         (swap! state update :list-partition-idx dec))
           :md-icon-name "zmdi-chevron-left"]
          [:div.list
           (for [{:as vims key :db/uid} (get vimsae list-partition-idx)]
             ^{:key key} [vims-list-item vims])]
          [re-com/md-icon-button
           :style {:visibility (when-not (get vimsae (inc list-partition-idx)) :hidden)}
           :class "chevron"
           :on-click (e> (.stopPropagation e)
                         (swap! state update :list-partition-idx inc))
           :md-icon-name "zmdi-chevron-right"]]]))))
