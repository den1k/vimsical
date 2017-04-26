(ns vimsical.frontend.user.views
  (:require [reagent.core :as reagent]
            [re-com.core :as re-com]
            [vimsical.frontend.util.crypt :as crypt]))

(defn gravatar-img-url [email]
  {:pre [email]}
  (str "https://www.gravatar.com/avatar/" (crypt/md5 email) "?=50"))

(defn avatar [{:keys [user on-click img-url] :as opts}]
  (let [img-url (or img-url (gravatar-img-url (:user/email user)))]
    [:div.avatar
     {:on-click on-click}
     [:img.pic {:src img-url}]]))

(defn full-name [{:keys [user on-click] :as opts}]
  (let [{:user/keys [first-name last-name]} user]
    [:div.name
     {:on-click on-click}
     [:div.first-name first-name]
     [:div.last-name last-name]]))

(defn avatar-full-name
  [{:keys [user class img-url on-click on-click-avatar on-click-name] :as opts}]
  {:pre [(:user/first-name user) (:user/last-name user)]}
  (let [{:user/keys [first-name last-name email]} user]
    [:div.user.ac
     (select-keys opts [:class :on-click])
     [avatar {:user     user
              :on-click on-click-avatar}]
     [full-name {:user     user
                 :on-click on-click-name}]]))