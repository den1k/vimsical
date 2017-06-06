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
 [(re-frame/inject-cofx :ui-db)]
 #?(:cljs
    (fn [{:keys [ui-db]} _]
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
            :event        [::resize-app-delay]}}))

(re-frame/reg-event-fx
 ::resize-app-delay
 (fn [_ _]
   ;; window.innerHeight takes awhile to update after an orienation change
   {:dispatch-later [{:ms       10
                      :dispatch [::resize-app]}]}))

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
      (let [ios-chrome?  (and (= :ios util.dom/operating-system)
                              (= :chrome util.dom/browser))
            inner-height (.-innerHeight js/window)
            height       (if ios-chrome?
                           (- inner-height 25) ; don't ask
                           (if (= :portrait orientation)
                             nil
                             inner-height))]
        (js/window.scrollTo 0 0)
        {:ui-db (assoc ui-db :height height)}))))
