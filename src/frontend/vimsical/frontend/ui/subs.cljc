(ns vimsical.frontend.ui.subs
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.ui-db :as ui-db]))

(re-frame/reg-sub
 ::orientation
 :<- [::ui-db/ui-db]
 (fn [ui-db _]
   (:orientation ui-db)))

(re-frame/reg-sub
 ::on-mobile?
 :<- [::ui-db/ui-db]
 (fn [ui-db _]
   (:on-mobile? ui-db)))