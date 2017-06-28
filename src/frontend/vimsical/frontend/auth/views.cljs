(ns vimsical.frontend.auth.views
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.frontend.auth.handlers :as handlers]
   [vimsical.remotes.backend.auth.commands :as auth.commands]
   [vimsical.frontend.remotes.fx :as frontend.remotes.fx]
   [vimsical.frontend.auth.subs :as subs]
   [vimsical.frontend.util.dom :as util.dom :refer-macros [e-> e>]]
   [vimsical.frontend.util.re-frame :as util.re-frame :refer [<sub]]
   [vimsical.frontend.views.popovers :as popovers]
   [vimsical.user :as user]
   [vimsical.frontend.landing.views :as landing.views]
   [re-frame.interop :as interop]))

(defmethod frontend.remotes.fx/error-message ::auth.commands/duplicate-email
  [_] "Email already registered")

(defmethod frontend.remotes.fx/error-message ::auth.commands/invalid-credentials
  [_] "Invalid email or password")

;;
;; * Signup
;;

(defn- signup-form [{:keys [invite? state submit-dispatch status invite-status]}]
  (letfn [(on-change [k] (e> (swap! state assoc k value)))
          (dispatch! [e]
            (.preventDefault e)
            (when (.. e -target checkValidity)
              (re-frame/dispatch submit-dispatch)))
          (status-msg [status]
            (case status
              nil "Sign up"
              ::frontend.remotes.fx/pending "Signing you up..."
              ::frontend.remotes.fx/success "Success!"
              (frontend.remotes.fx/error-message status)))]
    [:div.auth.signup.dc.ac
     [:div.beta-signup "Private Beta Signup"]
     (let [form [:form.form.jsb.dc
                 {:on-submit dispatch!}
                 [:div.first-last.jsb
                  [:input.first
                   {:class         "first"
                    :type          "text"
                    :name          "name"
                    :placeholder   "First Name"
                    :auto-complete "given-name"
                    :on-change     (on-change ::user/first-name)
                    :value         (::user/first-name @state)}]
                  [:input.last
                   {:type          "text"
                    :name          "name"
                    :placeholder   "Last Name"
                    :auto-complete "family-name"
                    :on-change     (on-change ::user/last-name)
                    :value         (::user/last-name @state)}]]
                 [:input
                  {:type          "email"
                   :name          "email"
                   :placeholder   "Email"
                   :auto-complete "email"
                   :on-change     (on-change ::user/email)
                   :value         (::user/email @state)}]
                 [:input
                  {:type          "password"
                   :name          "password"
                   :placeholder   "Password"
                   :auto-complete "new-password"
                   :min-length    8
                   :on-change     (on-change ::user/password)
                   :value         (::user/password @state)}]
                 [:input.input-button.signup-button.asc
                  {:type  "submit"
                   :value (status-msg status)}]]]

       (if-not invite?
         form
         (case invite-status
           (nil ::frontend.remotes.fx/pending) nil

           ::frontend.remotes.fx/success form

           [:h3 "This invite link has expired."])))]))

(defn signup []
  (let [state (reagent/atom {:db/uid (uuid)})]
    (fn []
      (let [status-key (reagent/current-component)
            status     (<sub [::frontend.remotes.fx/status :backend status-key])]
        [signup-form {:invite?         false
                      :state           state
                      :status          status
                      :submit-dispatch [::handlers/signup @state status-key]}]))))
;;
;; * Invite Signup
;;

(defn invite-signup
  ([token] [invite-signup token (reagent/atom (<sub [::subs/user]))])
  ([token state]
   (let [invite-status (<sub [::frontend.remotes.fx/status :backend ::handlers/invite])
         status-key    (reagent/current-component)
         status        (<sub [::frontend.remotes.fx/status :backend status-key])]
     [signup-form {:invite?         true
                   :state           state
                   :submit-dispatch [::handlers/invite-signup token @state status-key]
                   :status          status
                   :invite-status   invite-status}])))

;;
;; * Login
;;

(defn login []
  (let [status-key (reagent/current-component)
        state      (reagent/atom {:db/uid (uuid)})]
    (letfn [(on-change [k]
              (e> (swap! state assoc k value)))
            (dispatch! []
              (re-frame/dispatch [::handlers/login @state status-key]))
            (status-msg [status]
              (case status
                nil                           "Log in"
                ::frontend.remotes.fx/pending "Logging you in..."
                ::frontend.remotes.fx/success "Success!"
                (frontend.remotes.fx/error-message status)))]
      (fn []
        (let [status (<sub [::frontend.remotes.fx/status :backend status-key])]
          [:div.auth.login
           ;; prevent propagating click towards parent and closing popover
           {:on-click (e> (.stopPropagation e))}
           [:form.form.dc.jsb
            {:on-submit (e> (.preventDefault e))}
            [:input
             {:type          "email"
              :name          "email"
              :placeholder   "Email"
              :auto-complete "email"
              :auto-focus    true
              :on-change     (on-change ::user/email)}]

            [:input
             {:type          "password"
              :name          "password"
              :placeholder   "Password"
              :auto-complete "password"
              :min-length    8
              :on-change     (on-change ::user/password)
              :on-key-press  (e-> (util.dom/handle-key {:enter dispatch!}))}]
            [:div.cookie-and-forgot-pass.jsb.ac
             [:label.cookie.ac
              [:input.checkbox
               {:type            "checkbox"
                :default-checked true}]
              [:span.hint "Remember me"]]]
            [:input.input-button.login-button
             {:type     "submit"
              :value    (status-msg status)
              :on-click dispatch!}]]
           [:div.closed-beta
            [:div.title "New to Vimsical?"]
            [:div.stmt
             "Vimsical is in private beta. Click "
             [:a {:href     "#"
                  :on-click (e> (landing.views/scroll-to-waitlist))}
              "here"]
             " to join our waitlist."]]])))))

(defn logout []
  [:div.auth.logout
   {:on-click (e> (re-frame/dispatch [::handlers/logout]))}
   "Logout"])

;;
;; * Popover
;;

(defn login-popover [opts]
  [popovers/popover (assoc opts :child [login])])

(defn logout-popover [opts]
  [popovers/popover (assoc opts :child [logout])])
