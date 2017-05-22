(ns vimsical.frontend.app.subs
  (:require
   [re-frame.core :as re-frame]
   [vimsical.frontend.util.mapgraph :as util.mg]))

(re-frame/reg-sub
 ::route
 (fn [db _]
   (:app/route db)))

(re-frame/reg-sub-raw
 ::vims
 (fn [db [_ ?pattern]]
    (re-frame/subscribe [:q [:app/vims (or ?pattern '[*])]])))

(re-frame/reg-sub
 ::libs
 (fn [db [_ ?pattern]]
    (re-frame/subscribe [:app/libs (or ?pattern '[*])])))

(re-frame/reg-sub
 ::compilers
 (fn [db [_ ?pattern]]
    (re-frame/subscribe [:app/compilers (or ?pattern '[*])])))

(re-frame/reg-sub
 ::vims-info
 :<- [::vims
      [:vims/title
       {:vims/author [:user/first-name
                      :user/last-name
                      :user/email]}]]
 (fn [info _]
    info))