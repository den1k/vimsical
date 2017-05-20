(ns vimsical.frontend.live-preview.error-catcher
  "Error Catcher is a complicated beast.

  It's purpose is to pre-run the user's js code, catch and report errors if
  there are any, and if not, trigger an update to Live Preview.

  We use Error Catcher for efficiency, because updating the js on Live Preview
  requires a complete refresh of its iframe, which is pointless if there are
  errors and causes an ugly flash.

  Error Catcher is an invisible iframe that only loads js libs and files.
  Two things matter to Error Catcher.

  1. Did the code throw any errors?
  2. Did the code run successfully?

  For 1. it attaches an error handler to the iframe and then injects js code.
  For 2. it injects some js code after the user-written code that posts a message
  to the parent. If it never is, an errors have not been thrown, we're in a
  infinite loop."
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.util.dom :as util.dom]
            [vimsical.frontend.live-preview.ui-db :as ui-db]
            [vimsical.frontend.live-preview.subs :as subs]
            [vimsical.frontend.live-preview.handlers :as handlers]
            [vimsical.frontend.util.re-frame :as util.re-frame :refer [<sub]]
            [vimsical.frontend.vcs.subs :as vcs.subs]
            [vimsical.vcs.branch :as branch]
            [vimsical.vcs.file :as file]
            [vimsical.frontend.code-editor.handlers :as code-editor.handlers]
            [clojure.string :as string]))

(defn error-catcher []
  #?(:cljs
     (doto
      (util.dom/create :iframe
                       {:id     "error-catcher"
                        :style  "display: none"
                        :onload #(re-frame/dispatch [::on-load])})
       (js/document.body.appendChild))))


(defn set-error-handler
  "Error must be set everytime iframe is reloaded or source is reset"
  [iframe]
  (doto iframe
    (aset "contentWindow" "onerror"
          (fn [msg url line col error]
            (let [pos      {:line (dec line) ; dec to account for wrapping of js
                            :col  col}
                  err-info {:msg msg :url url :error error :pos pos}]
              (re-frame/dispatch [::on-error err-info]))))))

(defonce dispatch-status-message        ;; defonce to keep same fn ref over reloads
  (fn [m]
    (let [status {:status (keyword (.. m -data -status))}]
      (re-frame/dispatch [::on-status status]))))

(defn setup-message-listener []
  #?(:cljs
     (.addEventListener js/window "message" dispatch-status-message)))

(defn teardown-message-listener []
  #?(:cljs
     (.removeEventListener js/window "message" dispatch-status-message)))

(re-frame/reg-event-fx
 ::init
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} _]
   (setup-message-listener)
   {:ui-db (ui-db/set-error-catcher ui-db (error-catcher))}))

(re-frame/reg-event-fx
 ::dispose
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} _]
   #?(:cljs (js/document.body.removeChild (ui-db/get-error-catcher ui-db)))
   (teardown-message-listener)
   {:ui-db (ui-db/remove-error-catcher ui-db)}))

(re-frame/reg-event-fx
 ::update-error-catcher-src
 [(re-frame/inject-cofx :ui-db)]
 #?(:cljs
    (fn [{:keys [ui-db]} [_ markup]]
      (let [catcher       (ui-db/get-error-catcher ui-db)
            prev-blob-url (ui-db/get-error-catcher-src-blob-url ui-db)
            blob-url      (util.dom/blob-url markup "text/html")]
        (some-> prev-blob-url util.dom/revoke-blob-url)
        (aset catcher "src" blob-url)
        {:ui-db (ui-db/set-error-catcher-src-blob-url ui-db blob-url)}))))

(defn msg-wrap-js-string
  "Wraps js code in an IIFE (Immediately Invoked Function Expression) and
  appends a post message call to parent context (js/window) of the embedded iframe.

  The IIFE serves to encapsulate the js code and to avoid parse errors against
  the post message code. postMessage will only be called if the js code doesn't
  throw.

  Trims the string to its right to avoid errors around the trailing semicolon.
  Adds new lines to ease calculating the relative position of the js code on
  error. (see error-handler)"
  [string]
  (str
   "(function() {\n" (string/trimr string) " ;\n})()
   parent.postMessage({status: 'success'}, '*')"))

(re-frame/reg-event-fx
 ::update-error-catcher-js
 [(re-frame/inject-cofx :ui-db)]
 #?(:cljs
    (fn [{:keys [ui-db]} [_ string]]
      (let [catcher (ui-db/get-error-catcher ui-db)]
        (when (handlers/iframe-ready? catcher)
          (let [string   (msg-wrap-js-string string)
                node-id  "error-catcher-js"
                doc      (.-contentDocument catcher)
                old-node (.getElementById doc node-id)
                new-node (util.dom/create :javascript {:id node-id} string)]
            (if old-node
              (.replaceChild (.-body doc) new-node old-node)
              (.appendChild (.-body doc) new-node))
            nil))))))

(re-frame/reg-event-fx
 ::on-load
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} _]
   (let [catcher (ui-db/get-error-catcher ui-db)]
     (set-error-handler catcher)
     nil)))

(re-frame/reg-event-fx
 ::on-status
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ {:keys [status data]}]]
   (let [catcher (ui-db/get-error-catcher ui-db)]
     (.. catcher -contentWindow -location reload)
     (cond->
      {:debounce
       {:id         ::on-status
        :ms         500
        :dispatch-n (case status
                      :success [[::code-editor.handlers/clear-error-markers]
                                [::handlers/update-iframe-src]]
                      :error [[::code-editor.handlers/set-error-markers [data]]])}}
       (= :success status) (merge {:dispatch [::code-editor.handlers/clear-error-markers]})))))

(re-frame/reg-event-fx
 ::on-error
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ err-info]]
   {:dispatch [::on-status {:status :error :data err-info}]
    :ui-db    (ui-db/set-error-catcher-error ui-db err-info)}))

(re-frame/reg-event-fx
 ::track-start
 (fn [_ _]
   {:track
    [{:action       :register
      :id           ::error-catcher-markup
      :subscription [::subs/error-catcher-js-libs-markup]
      :val->event   (fn [markup]
                      [::update-error-catcher-src markup])}
     {:action          :register
      ;; Don't check on first run. This allow the iframe to load libs first.
      ;; Would be better to inject js the first time on-load runs
      :dispatch-first? false
      :id              ::error-catcher-files
      :subscription    [::subs/error-catcher-js-file-string]
      :val->event      (fn [string] [::update-error-catcher-js string])}]}))

(re-frame/reg-event-fx
 ::track-stop
 (fn [_ _]
   {:track
    [{:action :dispose :id ::error-catcher-markup}
     {:action :dispose :id ::error-catcher-files}]}))