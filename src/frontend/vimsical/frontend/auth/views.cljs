(ns vimsical.frontend.auth.views
  (:require [re-com.core :as re-com]
            [reagent.core :as reagent]
            [vimsical.frontend.util.dom :as util.dom]
            [vimsical.frontend.util.dom :refer-macros [e> e-> e->>]]
            [vimsical.frontend.views.popovers :as popovers]
            [goog.string :as gstr]))

(defn signup []
  (let [status    nil
        state     (reagent/atom {})
        on-change (fn [k] (e> (swap! state assoc k target)))]
    (fn []
      (let [{:keys [first-name last-name email password]} @state]
        [:div.signup
         [:.beta-signup
          "Private Beta Signup"]
         [:form.form
          {:on-submit (fn [e]
                        (.preventDefault e)
                        (when (.. e -target .checkValidity)
                          (prn :SIGNUP-ME-UP)))}
          [:.first-last
           [:input.first
            {:class         "first"
             :type          "text"
             :name          "name"
             :placeholder   "First Name"
             :auto-complete "given-name"
             :on-change     (on-change :first-name)}]
           [:input.last
            {:type          "text"
             :name          "name"
             :placeholder   "Last Name"
             :auto-complete "family-name"
             :on-change     (on-change :last-name)}]]
          [:input
           {:type          "email"
            :name          "email"
            :placeholder   "Email"
            :auto-complete "email"
            :on-change     (on-change :email)}]
          [:input
           {:type          "password"
            :name          "password"
            :placeholder   "Password"
            :auto-complete "new-password"
            :min-length    8
            :title         "Please use at least 8 characters"
            :on-change     (on-change :password)}]
          [:input.input-button.signup-button
           {:type  "submit"
            :value (case status
                     :status/pending "Signing you up..."
                     "Sign up")}]]]))))

(defn login []
  [:div.auth.login
   ;; prevent propagating click towards parent and closing popover
   {:on-click (e> (.stopPropagation e))}
   [:form.form.dc.jsb
    {:on-submit (e> (.preventDefault e))}
    [:input
     {:type          "email"
      :name          "email"
      :placeholder   "Email"
      :auto-complete "email"}]

    [:input
     {:type          "password"
      :name          "password"
      :placeholder   "Password"
      :auto-complete "password"
      :min-length    8
      :on-key-press  (e-> (util.dom/handle-key
                           {:enter #(js/console.debug "trying to log in here")}))}]
    [:div.cookie-and-forgot-pass.jsb.ac
     [:label.cookie.ac
      [:input.checkbox
       {:type            "checkbox"
        :default-checked true}]
      [:span.hint "Remember me"]]
     #_[:a "Forgot Password?"]]
    [:input.input-button.login-button
     {:type     "submit"
      :value    (case :nil #_status
                  :status/pending "Logging you in..."
                  "Log in")
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
     " to join our waitlist."]]])

(defn logout []
  [:div.auth.logout "Logout"])

(defn login-popover [opts]
  [popovers/popover (assoc opts :child [login])])

(defn logout-popover [opts]
  [popovers/popover (assoc opts :child [logout])])