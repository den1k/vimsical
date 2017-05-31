(ns vimsical.frontend.ui-db
  (:require
   [re-frame.core :as re-frame]
   [re-frame.interop :as interop]))

(defonce ui-db
  (interop/ratom {}))

(re-frame/reg-sub-raw
 ::ui-db
 (fn [_ [_ k]]
   (interop/make-reaction #(cond-> (deref ui-db) k (get k)))))

(re-frame/reg-cofx
 :ui-db
 (fn [cofx]
   (assoc cofx :ui-db @ui-db)))

;; runs after
(re-frame/reg-fx
 :ui-db
 (fn [value]
   (reset! ui-db value)))
