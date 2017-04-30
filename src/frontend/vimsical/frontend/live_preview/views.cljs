(ns vimsical.frontend.live-preview.views
  (:require
   [vimsical.common.util.core :as util]
   [reagent.core :as reagent]
   [vimsical.frontend.util.dom :as util.dom :refer-macros [e-> e>]]
   [re-frame.core :as re-frame]
   [vimsical.frontend.live-preview.handlers :as handlers]
   [vimsical.vcs.branch :as branch]
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

(defn preview-node [{:keys [ui-reg-key branch file]}]
  (let [string (re-frame/subscribe [::vcs.subs/file-string file])]
    (fn []
      (re-frame/dispatch [::handlers/update-preview-node ui-reg-key branch file @string])
      [:div])))

(defn live-preview [{:keys [branch ui-reg-key]}]
  (reagent/create-class
   {; component should maybe never update? TBD
    ;:should-component-update (fn [_ _ _] false)
    :render
    (fn [c]
      (re-frame/dispatch [::handlers/init ui-reg-key branch]) ; re-init on every render
      [:div.live-preview
       [:iframe.iframe
        {:key     ::iframe              ; ref only works when key is present
         :ref     (fn [node]
                    (if node
                      ; sync to make sure iframe is available before preview-nodes render
                      (re-frame/dispatch-sync
                       [::handlers/register-iframe ui-reg-key node])
                      (re-frame/dispatch-sync
                       [::handlers/dispose-iframe ui-reg-key])))
         :sandbox iframe-sandbox-opts}]
       (for [file (::branch/files branch)]
         ^{:key (:db/id file)}
         [preview-node {:ui-reg-key ui-reg-key
                        :branch     branch
                        :file       file}])])}))
