(ns vimsical.frontend.ui.handlers
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.config :as config]
            [vimsical.frontend.ui.subs :as subs]
   #?(:cljs [vimsical.frontend.util.dom :as util.dom])
            [vimsical.frontend.util.re-frame :as util.re-frame]))

(re-frame/reg-event-fx
 ::init
 [(re-frame/inject-cofx :ui-db)]
 #?(:cljs
    (fn [{:keys [ui-db]} _]
      {:ui-db (assoc ui-db :orientation (util.dom/orientation)
                           :on-mobile? (util.dom/on-mobile?))})))

(re-frame/reg-event-fx
 ::on-resize
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub [::subs/on-mobile?])]
 #?(:cljs
    (fn [{:keys [ui-db] ::subs/keys [on-mobile?]} _]
      (cond-> {:ui-db (cond-> ui-db
                        true (assoc :orientation (util.dom/orientation))
                        ; check to switch the app between views in device dev simulator
                        config/debug? (assoc :on-mobile? (util.dom/on-mobile?)))}))))

(re-frame/reg-event-fx
 ::on-scroll
 [(re-frame/inject-cofx :ui-db)]
 #?(:cljs
    (fn [{:keys [ui-db]} _]
      {:debounce {:ms       200
                  :dispatch [::resize-app]}})))

(re-frame/reg-event-fx
 ::track-orientation
 (fn [_ _]
   {:track {:action       :register
            :id           :orientation-track
            :subscription [::subs/orientation]
            :event        [::resize-app]}}))

(re-frame/reg-event-fx
 ::untrack-orientation
 (fn [_ _]
   {:track {:action :dispose :id :orientation-track}}))

(re-frame/reg-event-fx
 ::resize-app
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub [::subs/orientation])]
 (fn [{:keys [ui-db] ::subs/keys [orientation]} _]
   #?(:cljs
      {:ui-db (assoc ui-db
                :height
                (case orientation
                  :portrait nil
                  :landscape (do (js/window.scrollTo 0 0)
                                 (.-innerHeight js/window))))})))