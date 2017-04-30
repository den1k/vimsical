(ns vimsical.frontend.live-preview.views
  (:require
   [vimsical.common.util.core :as util]
   [reagent.core :as reagent]
   [vimsical.frontend.util.dom :as util.dom :refer-macros [e-> e>]]
   [re-frame.core :as re-frame]
   [vimsical.frontend.live-preview.handlers :as handlers]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.file :as file]
   [vimsical.frontend.vcs.subs :as vcs.subs]))

(def iframe-sandbox-opts
  "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/iframe"
  (util/space-join "allow-forms"
                   "allow-modals"
                   "allow-pointer-lock"
                   "allow-popups"
                   "allow-same-origin"
                   "allow-scripts"))

;;
;; * Components
;;

(defn preview-node-dispatch [{:keys [ui-reg-key branch file]}]
  (let [{::file/keys [sub-type]} file
        string (re-frame/subscribe [::vcs.subs/file-string file])]
    (re-frame/dispatch [::handlers/update-live-preview ui-reg-key branch file @string])
    [:div]))

(defn- iframe-attrs [{:keys [branch ui-reg-key static?] :as opts}]
  (let [defaults
        {:key     ::iframe              ; ref only works when key is present
         :sandbox iframe-sandbox-opts
         :ref     (fn [node]
                    (if node
                      ; sync to make sure iframe is available before preview-nodes render
                      (re-frame/dispatch-sync
                       [::handlers/register-and-init-iframe
                        ui-reg-key node branch])
                      (re-frame/dispatch-sync
                       [::handlers/dispose-iframe ui-reg-key])))}]
    (cond-> defaults
      (not static?)
      (assoc :on-load
             #(re-frame/dispatch
               [::handlers/move-script-nodes ui-reg-key branch])))))

(defn live-preview [{:keys [branch ui-reg-key static?] :as opts}]
  (fn [_]
    (reagent/create-class
     {; component should maybe never update? TBD
      ;:should-component-update (fn [_ _ _] false)
      :render
      (fn [_]
        [:div.live-preview
         [:iframe.iframe
          (iframe-attrs opts)]
         (when-not static?
           (for [file (::branch/files branch)]
             ^{:key (:db/id file)}
             [preview-node-dispatch {:ui-reg-key ui-reg-key
                                     :branch     branch
                                     :file       file}]))])})))
