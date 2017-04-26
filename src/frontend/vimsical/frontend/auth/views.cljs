(ns vimsical.frontend.auth.views
  (:require [re-com.core :as re-com]
            [reagent.core :as reagent]
            [vimsical.frontend.util.dom :as util.dom]
            [vimsical.frontend.util.dom :refer-macros [e> e-> e->>]]))

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


(defn auth-popover [{:keys [showing? body] :as opts}]
  [re-com/popover-content-wrapper
   :arrow-gap 2
   :arrow-width 20
   :on-cancel (fn [])                   ; needed render background overlay
   :arrow-length 10
   :body body
   :showing-injected? showing?
   :position-injected (reagent/atom :below-center)])

(defn login-popover-anchor [{:keys [showing? anchor style] :as opts}]
  {:pre [anchor]}
  [re-com/popover-anchor-wrapper
   :showing? showing?
   :anchor anchor
   :position :below-left
   :style style
   :popover [auth-popover
             {:showing? showing?
              :body     [login]}]])

(defn logout []
  [:div.auth.logout "Logout"])

(defn logout-popover-anchor [{:keys [showing? anchor style] :as opts}]
  {:pre [anchor]}
  [re-com/popover-anchor-wrapper
   :showing? showing?
   :anchor anchor
   :position :below-left
   :style style
   :popover [auth-popover
             {:showing? showing?
              :body     [logout]}]])