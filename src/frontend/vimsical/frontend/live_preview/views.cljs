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
   We can also reload the iframe manually without having to reinject any of our code.

   Live-Preview with :error-catcher?
   Error-Catcher is a invisible iframe in charge of loading a vims' js libs and
   evaluating the user-written javascript to check for parse and runtime errors.
   When it is active Live-Preview only tracks non-js files and lets
   Error-Catcher check and run on js updates."
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [vimsical.common.util.core :as util :include-macros true]
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

;; TODO move this into handlers
(defn- err-catcher-files
  "Error Catcher tracks the js files already. So if it is on, we remove them
  from Live Preview's tracking"
  [files]
  (remove file/javascript? files))

(defn register [node {:keys [vims static? error-catcher?]}]
  {:pre [node vims]}
  (re-frame/dispatch-sync [::handlers/register-iframe vims node])
  (re-frame/dispatch [::handlers/update-iframe-src vims])
  (when-not static?
    (when error-catcher?
      (re-frame/dispatch [::error-catcher/init])
      (re-frame/dispatch [::error-catcher/track-start vims]))
    (re-frame/dispatch [::handlers/track-vims vims])))

(defn dispose [{:keys [vims static? error-catcher?]}]
  (when-not static?
    (when error-catcher?
      (re-frame/dispatch [::error-catcher/track-stop])
      (re-frame/dispatch [::error-catcher/dispose]))
    (re-frame/dispatch [::handlers/stop-track-vims vims]))
  ;; if live-preview is used in other components with the same vims
  ;; mount/unmount cycle can lead to race conditions which wrongly
  ;; dispose the iframe of the mounting component.
  #_(re-frame/dispatch [::handlers/dispose-iframe vims]))

(defn recycle
  [node old-opts new-opts]
  {:pre [node old-opts new-opts]}
  (dispose old-opts)
  (register node new-opts))

;;
;; * Components
;;

(defn iframe-preview [opts]
  (reagent/create-class
   {:component-did-mount
    (fn [c]
      (let [node (reagent/dom-node c)]
        (register node opts)))

    :component-will-unmount
    (fn [c]
      (dispose (reagent/props c)))

    :component-will-receive-props
    (fn [c [_ new-opts]]
      (recycle (reagent/dom-node c) (reagent/props c) new-opts))

    :reagent-render
    (fn [{:keys [vims static?]}]
      [:iframe.iframe
       (merge
        {:sandbox iframe-sandbox-opts}
        (when-not static?
          {:on-load #(re-frame/dispatch [::handlers/move-script-nodes vims])}))])}))

(defn live-preview [opts]
  [:div.live-preview
   [iframe-preview opts]])