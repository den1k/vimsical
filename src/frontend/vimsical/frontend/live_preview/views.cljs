(ns vimsical.frontend.live-preview.views
  "Live-Preview is in charge of previewing the result of the current code state.
  On mount it creates a static markup string and sets it as `src` on iframe. The
  html loads css and js libs and css files in the head, and user written js
  alongside user-written markup in body. With `static?` true, that's the end of
  the story, otherwise:

  - after load it takes the js from the body and moves it into the head,
  so that the now loaded html can be changed without overwriting the js
  - it renders additional components with subscriptions to file changes
  - these nodes trigger incremental updates for css and html files and iframe
   `src` resets for js changes (this wipes js state from memory)

   Why the static markup?
   To avoid ghost JavaScript we must be able to reload the iframe. This happens
   automaticallly when we make blob, give it a URL and set it as `src` on iframe.
   We can also reload the iframe manually without having to reinject any of our code."
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
