(ns vimsical.frontend.auth.views
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.frontend.auth.handlers :as handlers]
   [vimsical.frontend.remotes.fx :as frontend.remotes.fx]
   [vimsical.frontend.util.dom :as util.dom :refer-macros [e-> e>]]
   [vimsical.frontend.util.re-frame :as util.re-frame :refer [<sub]]
   [vimsical.frontend.views.popovers :as popovers]
   [vimsical.user :as user]))

;;
;; * Signup
;;

(defn signup-success
  []
  [:div (name ::signup-success)])

;; XXX make status subs with [status messages]

(defn signup []
  (let [status-key (reagent/current-component)
        status     (<sub [::frontend.remotes.fx/status :backend status-key])
        state      (reagent/atom {:db/uid (uuid)})
        on-change  (fn [k] (e> (swap! state assoc k value)))]
    (fn []
      [:div.auth.signup.dc.ac
       [:div.beta-signup
        "Private Beta Signup"]
       [:form.form.jsb.dc
        {:on-submit (fn [e]
                      (.preventDefault e)
                      (when (.. e -target checkValidity)
                        (re-frame/dispatch [::handlers/signup @state status-key])))}
        [:div.first-last.jsb
         [:input.first
          {:class         "first"
           :type          "text"
           :name          "name"
           :placeholder   "First Name"
           :auto-complete "given-name"
           :on-change     (on-change ::user/first-name)}]
         [:input.last
          {:type          "text"
           :name          "name"
           :placeholder   "Last Name"
           :auto-complete "family-name"
           :on-change     (on-change ::user/last-name)}]]
        [:input
         {:type          "email"
          :name          "email"
          :placeholder   "Email"
          :auto-complete "email"
          :on-change     (on-change ::user/email)}]
        [:input
         {:type          "password"
          :name          "password"
          :placeholder   "Password"
          :auto-complete "new-password"
          :min-length    8
          :on-change     (on-change ::user/password)}]
        [:input.input-button.signup-button.asc
         {:type  "submit"
          :value (case status
                   nil                           "Sign up"
                   ::frontend.remotes.fx/pending "Signing you up...")}]]])))

;;
;; * Login
;;

(defn login []
  (let [status-key (reagent/current-component)
        status     (<sub [::frontend.remotes.fx/status :backend status-key])
        state      (reagent/atom {:db/uid (uuid)})
        on-change  (fn [k] (e> (swap! state assoc k value)))]
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
        :on-change     (on-change ::user/email)}]

      [:input
       {:type          "password"
        :name          "password"
        :placeholder   "Password"
        :auto-complete "password"
        :min-length    8
        :on-change     (on-change ::user/password)
        :on-key-press  (e-> (util.dom/handle-key
                             {:enter #(re-frame/dispatch [::handlers/login @state status-key])}))}]
      [:div.cookie-and-forgot-pass.jsb.ac
       [:label.cookie.ac
        [:input.checkbox
         {:type            "checkbox"
          :default-checked true}]
        [:span.hint "Remember me"]]]
      [:input.input-button.login-button
       {:type     "submit"
        :value    (case status
                    nil                           "Log in"
                    ::frontend.remotes.fx/pending "Logging you in...")
        :on-click #(js/console.debug "trying to log in here")}]]
     [:div.closed-beta
      [:div.title "New to Vimsical?"]
      [:div.stmt
       "Vimsical is in private beta. Click "
       [:a {:href     "#"
            :on-click (e>
                       (js/console.debug "route to landing and scroll to waitlist")
                       #_(doto (util.dom/qsel ".bottom-waitlist .email")
                           util.dom/scroll-to
                           .focus))}
        "here"]
       " to join our waitlist."]]]))

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
