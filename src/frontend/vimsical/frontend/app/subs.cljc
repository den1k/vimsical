(ns vimsical.frontend.app.subs
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.user :as user]
   [vimsical.vims :as vims]))

(re-frame/reg-sub
 ::modal
 (fn [db _]
   (:app/modal db)))

(re-frame/reg-sub-raw
 ::user
 (fn [db [_ ?pattern]]
   (re-frame/subscribe [:q [:app/user (or ?pattern '[*])]])))

(re-frame/reg-sub-raw
 ::vims
 (fn [db [_ ?pattern]]
   (re-frame/subscribe [:q [:app/vims (or ?pattern '[*])]])))

(re-frame/reg-sub-raw
 ::libs
 (fn [db [_ ?pattern]]
   (re-frame/subscribe [:q [:app/libs (or ?pattern '[*])]])))

(re-frame/reg-sub-raw
 ::compilers
 (fn [db [_ ?pattern]]
   (re-frame/subscribe [:q [:app/compilers (or ?pattern '[*])]])))

(re-frame/reg-sub-raw
 ::vims-info
 (fn [db [_ ?vims]]
   (let [db      @db
         pattern [::vims/title {::vims/owner [::user/first-name ::user/last-name ::user/email]}]]
     (re-frame/subscribe
      (if ?vims
        [:q
         pattern
         (mg/ref-to db ?vims)]
        [::vims pattern])))))
