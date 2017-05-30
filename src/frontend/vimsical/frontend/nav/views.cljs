(ns vimsical.frontend.nav.views
  (:require
   [re-com.core :as re-com]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [vimsical.common.util.core :as util :refer [=by] :include-macros true]
   [vimsical.frontend.app.handlers :as app.handlers]
   [vimsical.frontend.auth.views :as auth.views]
   [vimsical.frontend.user.views :as user.views]
   [vimsical.frontend.util.dom :as util.dom :refer-macros [e-> e>]]
   [vimsical.frontend.util.re-frame :refer [<sub]]
   [vimsical.frontend.views.icons :as icons]
   [vimsical.frontend.vims-list.views :as vims-list.views]
   [vimsical.user :as user]
   [vimsical.vims :as vims]))

(defn limit-title-length? [e]
  (let [txt             (-> e .-target .-innerHTML)
        limit-hit?      (>= (count txt) 25)
        ;; allow navigation (arrow keys) and delete but no addition of new letters
        key-code        (aget e "keyCode")
        forbidden-keys? (or (== key-code 32) (> key-code 40))]
    (and limit-hit? forbidden-keys?)))

(defn vims-info []
  (let [state             (reagent/atom {:editing?        false
                                         :title-too-long? false
                                         :hovering?       false})
        title-placeholder "Untitled Vims"
        keydown-handler   (fn [e]
                            (let [limit? (limit-title-length? e)]
                              ;; todo update limit
                              (swap! state assoc
                                     :title-too-long? limit?
                                     )
                              (if limit?
                                (.preventDefault e)
                                (util.dom/handle-key e
                                                     {:enter
                                                      (fn []
                                                        (.preventDefault e)
                                                        (util.dom/blur))}))))

        show-tooltip?     (reagent.ratom/make-reaction
                           #(let [{:keys [editing?
                                          title-too-long?
                                          hovering?]} @state]
                              (if editing?
                                title-too-long?
                                hovering?)))]
    (fn []
      (let
          [user            (<sub [:q [:app/user [:db/uid]]])

           {::vims/keys [title owner] :as vims}
           (<sub [:q [:app/vims
                      [::vims/title
                       {::vims/owner [:db/uid]}]]])

           {:keys [editing? title-too-long?]} @state

           editable-title? (=by :db/uid user owner)
           title-html
           [:div.title
            (when editable-title?
              {:content-editable                  true
               :suppress-content-editable-warning true
               :on-mouse-enter                    #(swap! state assoc :hovering? true)
               :on-mouse-leave                    #(swap! state assoc :hovering? false)
               :on-key-down                       (e-> keydown-handler)
               :on-click                          #(swap! state assoc :editing? true)
               :on-blur                           (e>
                                                   (swap! state assoc :editing? false)
                                                   (re-frame/dispatch
                                                    [::vims/set-title (util/norm-str inner-html)]))})
            (or title
                (if editing?
                  ""
                  title-placeholder))]]
        [:div.vims-info.jc.ac
         (if-not editable-title?
           title-html
           [re-com/popover-tooltip
            :label (if title-too-long? "Title is too long!" "Click to edit")
            :position :below-center
            :showing? show-tooltip?
            :anchor title-html])]))))

(defn nav []
  (let
      [show-popup?     (reagent/atom false)
       show-vims-list? (reagent/atom false)
       user            (<sub [:q [:app/user
                                  [:db/uid
                                   ::user/first-name
                                   ::user/last-name
                                   ::user/email
                                   {::user/vimsae [:db/uid ::vims/title]}]]])
       app-vims        (<sub [:q [:app/vims
                                  [:db/uid
                                   ::vims/title]]])]
    [:div.main-nav.ac.jsb
     [:div.logo-and-type
      {:on-click (e> (re-frame/dispatch [::app.handlers/route :route/landing]))
       :on-double-click (e> (re-frame/dispatch [::app.handlers/route :route/signup]))}
      [:span.logo icons/vimsical-logo]
      [:span.type icons/vimsical-type]]
     (when app-vims
       [vims-info])
     (when (user/logged-in? user)
       [vims-list.views/vims-list-popover
        {:showing? show-vims-list?
         :anchor   [:div.button
                    {:on-click (e> (swap! show-vims-list? not))}
                    "My Vims"]}])
     [:div.auth-or-user
      ;; popovers use no-op :on-cancel cb because event bubbles up here
      {:on-click (e> (swap! show-popup? not))}
      (if (user/anon? user)
        [:div.auth
         [auth.views/login-popover
          {:showing?  show-popup?
           :position  :below-left
           :anchor    [:div.button "login"]
           :on-cancel (constantly nil)}]]
        [:div.user.ac
         [auth.views/logout-popover
          {:showing?  show-popup?
           :anchor    [user.views/avatar {:user user}]
           :on-cancel (constantly nil)}]
         [user.views/full-name {:user user}]])]]))
