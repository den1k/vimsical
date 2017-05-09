(ns vimsical.frontend.ui-db
  (:require [re-frame.core :as re-frame]))

(defonce ui-db
  (atom {}))

(re-frame/reg-cofx
 :ui-db
 (fn [cofx _]
   (assoc cofx :ui-db @ui-db)))

;; runs after
(re-frame/reg-fx
 :ui-db
 (fn [value]
   (reset! ui-db value)))