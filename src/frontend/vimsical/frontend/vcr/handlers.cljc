(ns vimsical.frontend.vcr.handlers
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.vcr.data :as data]
            [vimsical.frontend.code-editor.handlers :as code-editor.handlers]))

(defn editors-by-type [db]
  (::data/editors db))

(defn active-editor [db]
  (::data/active-editor db))

(defn active-editor-instance [db]
  (-> (editors-by-type db)
      (get (active-editor db))))

(re-frame/reg-event-db
 ::active-editor
 (fn [db [_ file-type]]
   (assoc db ::data/active-editor file-type)))

(re-frame/reg-event-fx
 ::paste
 (fn [{:keys [db]} [_ string]]
   (let [active-editor-instance (active-editor-instance db)]
     {:db       db
      :dispatch [::code-editor.handlers/paste
                 active-editor-instance
                 string]})))