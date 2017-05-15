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
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [vimsical.common.util.core :as util]
   [vimsical.frontend.live-preview.handlers :as handlers]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.file :as file]
   [vimsical.frontend.live-preview.error-catcher :as error-catcher]))

;;
;; * iFrame helpers
;;

(def iframe-sandbox-opts
  "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/iframe"
  (util/space-join "allow-forms"
                   "allow-modals"
                   "allow-pointer-lock"
                   "allow-popups"
                   "allow-same-origin"
                   "allow-scripts"))

(defn- iframe-ref-handler
  [branch]
  (fn [node]
    (if node
      (do
        (re-frame/dispatch [::error-catcher/init])
        (re-frame/dispatch [::handlers/register-and-init-iframe node branch]))
      (do
        (re-frame/dispatch [::error-catcher/dispose])
        (re-frame/dispatch [::handlers/dispose-iframe])))))

(defn- iframe-on-load-handler
  [branch]
  (fn []
    (re-frame/dispatch [::handlers/move-script-nodes branch])))

(defn- iframe-attrs
  [{:keys [branch static?] :as opts}]
  (cond-> {:key     ::iframe            ; ref only works when key is present
           :sandbox iframe-sandbox-opts
           :ref     (iframe-ref-handler branch)}
    (not static?) (assoc :on-load (iframe-on-load-handler branch))))

(defn- err-catcher-files
  "Error Catcher tracks the js files already. So if it is on, we remove them
  from Live Preview's tracking"
  [files]
  (remove (fn [{::file/keys [sub-type]}] (= :javascript sub-type)) files))

;;
;; * Components
;;

(defn live-preview
  [{:as                     opts
    {::branch/keys [files]} :branch
    :keys                   [branch static? error-catcher?]
    :or                     {error-catcher? true}}]
  (let [files (cond-> files error-catcher? err-catcher-files)]
    (reagent/create-class
     (merge
      (when-not static?
        {:component-did-mount
         (fn [_]
           (when error-catcher?
             (re-frame/dispatch [::error-catcher/track-start]))
           (doseq [file files]
             (re-frame/dispatch [::handlers/track-start branch file])))

         :component-will-unmount
         (fn [_]
           (when error-catcher?
             (re-frame/dispatch [::error-catcher/track-stop]))
           (doseq [file files]
             (re-frame/dispatch [::handlers/track-stop branch file])))})
      {:render
       (fn [_]
         [:div.live-preview
          [:iframe.iframe (iframe-attrs opts)]])}))))
