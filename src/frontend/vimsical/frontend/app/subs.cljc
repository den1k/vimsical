(ns vimsical.frontend.app.subs
  (:require
   [re-frame.core :as re-frame]
   [vimsical.frontend.util.mapgraph :as util.mg]))

(re-frame/reg-sub
 ::route
 (fn [db _]
   (:app/route db)))

(re-frame/reg-sub
 ::vims
 (fn [db [_ ?pattern]]
   (util.mg/pull* db [:app/vims (or ?pattern '[*])])))

(re-frame/reg-sub
 ::libs
 (fn [db [_ ?pattern]]
   (util.mg/pull* db [:app/libs (or ?pattern '[*])])))

(re-frame/reg-sub
 ::compilers
 (fn [db [_ ?pattern]]
   (util.mg/pull* db [:app/compilers (or ?pattern '[*])])))
