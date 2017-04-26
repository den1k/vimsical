(ns vimsical.frontend.nav.views
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [re-com.core :as re-com]
   [vimsical.frontend.views.icons :as icons]
   [vimsical.frontend.util.re-frame :refer-macros [with-subs with-queries]]
   [vimsical.frontend.util.dom :refer-macros [e-> e->> e>]]
   [vimsical.frontend.util.dom :as util.dom :refer-macros [e-> e->> e>]]
   [vimsical.common.util.core :refer [=by] :as util]
   [vimsical.frontend.app.handlers :as app]
   [vimsical.frontend.user.views :as user.views]
   [vimsical.frontend.auth.views :as auth.views]))

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
      (with-queries
       [user [:app/user [:db/id]]

        {:vims/keys [title author] :as vims} [:app/vims
                                              [:vims/title
                                               {:vims/author [:db/id]}]]]
        (let [{:keys [editing? title-too-long?]}
              @state
              editable-title?
              (=by :db/id user author)
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
                                                       [:vims/set-title (util/norm-str inner-html)]))})
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
              :anchor title-html])])))))

(defn nav []
  (let [show-popup? (reagent/atom true)]
    (with-queries
     [{:user/keys [first-name last-name vimsae] :as user}
      [:app/user
       [:db/id
        :user/first-name
        :user/last-name
        :user/email
        {:user/vimsae [:db/id :vims/title]}]]

      {:vims/keys [title] :as app-vims} [:app/vims
                                         [:db/id
                                          :vims/title]]]
      [:div.main-nav.ac.jsb
       [:div.logo-and-type
        [:span.logo icons/vimsical-logo]
        [:span.type icons/vimsical-type]]
       (when app-vims
         [vims-info])
       #_[:div.vims-list
          (for [{:vims/keys [title] :as vims} vimsae]
            ^{:key title}
            [:div.vims-title
             {:on-click (e> (re-frame/dispatch [::app/open-vims vims]))}
             title])]

       [:div.auth-or-user
        {:on-click (e> (swap! show-popup? not))}
        (if false                       ;'logged-in?
          [:div.user.ac
           [auth.views/logout-popover-anchor
            {:showing? show-popup?
             :anchor   [user.views/avatar {:user user}]}]
           [user.views/full-name {:user user}]]
          [:div.auth
           [auth.views/login-popover-anchor
            {:showing? show-popup?
             :anchor   [:div.button "login"]}]])]])))
