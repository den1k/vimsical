(ns vimsical.frontend.live-preview.views
  (:require
   [vimsical.common.util.core :as util]
   [reagent.core :as reagent]
   [vimsical.frontend.util.dom :as util.dom :refer-macros [e-> e>]]
   [re-frame.core :as re-frame]
   [vimsical.frontend.live-preview.handlers :as handlers]))

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

(defn preview-node [{:keys [file-type parent-ui-reg-key]}]
  (let [; todo subs to html/css/js string based on file-type
        text (get {:html "<h1>Live-Preview</h1>"
                   :css  "body {
                   display: flex;
                   align-items: center;
                   justify-content: space-around;
                   background: bisque;
                   color: white;
                   font-size: 50px;
                   -webkit-text-stroke: 1px grey;
                   };"
                   ;:javascript "console.log('hello')"
                   }
                  file-type)]
    (fn [c]
      (when text
        (re-frame/dispatch
         [::handlers/update-node parent-ui-reg-key file-type text]))
      [:div])))

(defn live-preview [{:keys [ui-reg-key]}]
  (let [;; todo subs to files
        filetypes #{:html :css :javascript}]
    (reagent/create-class
     {; component should maybe never update? TBD
      ;:should-component-update (fn [_ _ _] false)
      :render
      (fn [c]
        [:div.live-preview
         [:iframe.iframe
          {:key     ::iframe            ; ref only works when key is present
           :ref     (fn [node]
                      (if node
                        ; sync to make sure iframe is available before preview-nodes render
                        (do (re-frame/dispatch-sync
                             [::handlers/register-iframe ui-reg-key node]))
                        (re-frame/dispatch
                         [::handlers/dispose-iframe ui-reg-key])))
           :sandbox iframe-sandbox-opts}]
         (for [ft filetypes]
           ^{:key (str ::preview-node- ft)}
           [preview-node {:parent-ui-reg-key ui-reg-key
                          :file-type         ft}])])})))
