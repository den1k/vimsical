(ns vimsical.frontend.app.subs
  (:require
   [re-frame.core :as re-frame]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.vims :as vims]
   [vimsical.user :as user]
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.interop :as interop]))

(re-frame/reg-sub
 ::route
 (fn [db _]
   (:app/route db)))

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
