(ns vimsical.frontend.views.user
  (:require [reagent.core :as reagent]
            [re-com.core :as re-com]
            [vimsical.frontend.util.crypt :as crypt]))

(defn gravatar-img-url [email]
  {:pre [email]}
  (str "https://www.gravatar.com/avatar/" (crypt/md5 email) "?=50"))

(defn avatar [{:keys [class img-url on-click] :as opts}]
  {:pre [img-url]}
  [:div.avatar
   (dissoc opts :img-url)
   [:img.pic {:src img-url}]])

(defn avatar-full-name [{:keys [user class img-url on-click] :as opts}]
  {:pre [(:user/first-name user) (:user/last-name user)]}
  (let [{:user/keys [first-name last-name email]} user]
    [:div.user
     (avatar {:img-url (or img-url (gravatar-img-url email))})
     [:div.name
      [:div.first-name first-name]
      [:div.last-name last-name]]]))