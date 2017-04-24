(ns vimsical.frontend.vcr.subs
  (:require [re-frame.core :as re-frame]))

(defn editors-by-type [db]
  (-> db :app/vcr :vcr/editor-instances-by-file-type))

(defn active-editor [db]
  (-> db :app/vcr :vcr/active-editor))

(defn active-editor-instance [db]
  (-> (editors-by-type db)
      (get (active-editor db))))

(re-frame/reg-sub
  ::editors-by-file-type
  (fn [db _]
    (editors-by-type db)))

(re-frame/reg-sub
  ::file-type->editor
  :<- [::editors-by-file-type]
  (fn [ef [_ file-type]]
    (get ef file-type)))

(re-frame/reg-sub
  ::active-editor
  (fn [db _]
    (active-editor db)))

(re-frame/reg-sub
  ::active-editor-instance
  :<- [::active-editor]
  :<- [::editors-by-file-type]
  (fn [[active ef]]
    (get ef active)))