(ns vimsical.frontend.app.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::route
 (fn [db _]
   (:app/route db)))
