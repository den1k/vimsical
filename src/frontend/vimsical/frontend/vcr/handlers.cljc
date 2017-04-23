(ns vimsical.frontend.vcr.handlers
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.vcr.subs :as subs]
            [vimsical.frontend.code-editor.handlers :as code-editor.handlers]))

(re-frame/reg-event-db ::register-editor
  (fn [db [_ file-type editor-instance]]
    (assoc-in db [:app/vcr :vcr/editor-instances-by-file-type file-type] editor-instance)))

(re-frame/reg-event-db ::active-editor
  (fn [db [_ file-type]]
    (assoc-in db [:app/vcr :vcr/active-editor] file-type)))

(re-frame/reg-event-db ::paste
  (fn [db [_ string]]
    (let [active-editor-instance (subs/active-editor-instance db)]
      (re-frame/dispatch [::code-editor.handlers/paste
                          active-editor-instance
                          string]))
    db))