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
   [vimsical.frontend.live-preview.error-catcher :as error-catcher]
   [vimsical.frontend.live-preview.handlers :as handlers]
   [vimsical.vcs.file :as file]))

;;
;; * iFrame helpers
;;

(def iframe-sandbox-opts
  "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/iframe"
  (util/space-join "allow-forms"
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

(defn register [node {:keys [vims static? error-catcher?] :as opts}]
  {:pre [node vims]}
  (re-frame/dispatch [::handlers/register-iframe opts node])
  (if static?
    (do
      (re-frame/dispatch [::handlers/update-iframe-snapshots opts])
      (re-frame/dispatch [::handlers/update-iframe-src opts]))
    (do
      (when error-catcher?
        (re-frame/dispatch [::error-catcher/init])
        (re-frame/dispatch [::error-catcher/track-start opts]))
      (re-frame/dispatch [::handlers/track-vims opts]))))

(defn dispose [{:keys [vims static? error-catcher?] :as opts}]
  (when-not static?
    (when error-catcher?
      (re-frame/dispatch [::error-catcher/track-stop])
      (re-frame/dispatch [::error-catcher/dispose]))
    (re-frame/dispatch [::handlers/stop-track-vims opts]))
  (re-frame/dispatch [::handlers/dispose-iframe opts]))

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
      (let [old-opts (reagent/props c)]
        (when (and old-opts (not (util/=by (comp :db/uid :vims) old-opts new-opts)))
          (recycle (reagent/dom-node c) (reagent/props c) new-opts))))

    :reagent-render
    (fn [{:keys [static? vims] :as opts}]
      [:iframe.iframe
       (merge
        {:sandbox iframe-sandbox-opts}
        (when-not static?
          {:on-load #(re-frame/dispatch [::handlers/move-script-nodes opts])}))])}))

(defn live-preview [opts]
  ;; XXX There is a race condition if we register a vims that doesn't have its
  ;; files yet (due to async loading). The exact issue is still TBD but in these
  ;; cases, once the file tracks update with the file strings we end up with
  ;; markup for the document that's missing the script nodes in the header and
  ;; the content in the body.
  (letfn [(opts-ready? [{:keys [from-snapshot? files]}]
            (or from-snapshot? (not-empty files)))]
    [:div.live-preview
     (when (opts-ready? opts)
       [iframe-preview opts])]))
