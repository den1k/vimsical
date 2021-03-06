(ns vimsical.frontend.window-listeners.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [vimsical.frontend.util.dom :as util.dom :refer-macros [e-> e->> e>]]
            [vimsical.frontend.quick-search.handlers :as quick-search.handlers]
            [vimsical.frontend.util.re-frame :refer [<sub]]
            [vimsical.frontend.app.subs :as app.subs]
            [vimsical.frontend.app.handlers :as app.handlers]
            [vimsical.frontend.ui.subs :as ui.subs]
            [vimsical.frontend.ui.handlers :as ui.handlers]
            [vimsical.frontend.vcs.sync.handlers :as vcs.sync.handlers]
            [cljsjs.clipboard]
            [re-frame.interop :as interop]
            [vimsical.frontend.remotes.fx :as frontend.remotes.fx]))

;;
;; * Shortcuts
;;

(def ^:private key-map
  {220            :backslash
   191            :forwardslash
   #{:shift 32}   :shift-space
   #{:option 32}  :option-space
   #{:option 191} :option-forwardslash
   9              :tab
   221            :right-bracket
   219            :left-bracket
   37             :arrow-left
   38             :arrow-up
   39             :arrow-right
   40             :arrow-down
   13             :return
   27             :esc})

(defn modifier-keys [event]
  (let [modi        (zipmap
                     [:meta :ctrl :option :shift]
                     (map #(aget event %) ["metaKey" "ctrlKey" "altKey" "shiftKey"]))
        active-modi (set
                     (keep (fn [[k v]]
                             (when (true? v)
                               k))
                           modi))]
    (when (seq active-modi)
      (set active-modi))))

(defn get-key-combo [event]
  (let [keycode      (.. event -keyCode)
        modi         (modifier-keys event)
        key-or-combo (if modi
                       (conj modi keycode)
                       keycode)]
    (get key-map key-or-combo)))

(defn handle-shortcut [e]
  (when-let [shortcut (get-key-combo e)]
    (case shortcut
      :option-forwardslash (do
                             (.preventDefault e) ;; prevent typing
                             (re-frame/dispatch [::quick-search.handlers/toggle]))
      :esc (re-frame/dispatch [::app.handlers/close-modal])
      nil)))

(defn handle-resize [_]
  (re-frame/dispatch [::ui.handlers/on-resize]))

(defn handle-scroll [_]
  (re-frame/dispatch [::ui.handlers/on-scroll]))

(defn handle-before-unload [e]
  ;; subs cached in event handler will not be GC'd
  (when-let [vims-uid (:db/uid (<sub [::app.subs/vims [:db/uid]]))]
    (let [dispatch-vec [::vcs.sync.handlers/sync vims-uid]
          _            (re-frame/dispatch-sync dispatch-vec)
          status       (<sub [::frontend.remotes.fx/status :backend dispatch-vec])]
      (when (= status ::frontend.remotes.fx/pending)
        (set! (.-returnValue e) true)))))

(defn window-listeners []
  (let [state      (interop/ratom {:clipboard nil})
        on-mobile? (<sub [::ui.subs/on-mobile?])
        listeners  (cond-> {"keydown"      handle-shortcut
                            "resize"       handle-resize
                            "beforeunload" handle-before-unload}
                     on-mobile? (assoc "scroll" handle-scroll))]
    (reagent/create-class
     {:component-did-mount
      (fn [_]
        (swap! state assoc :clipboard (new js/Clipboard ".copy-to-clipboard"))
        (when on-mobile?
          (re-frame/dispatch [::ui.handlers/track-orientation]))
        (doseq [[event-type handler] listeners]
          (.addEventListener js/window event-type handler)))
      :component-will-unmount
      (fn []
        (.destroy ^js/Clipboard (:clipboard @state))
        (when on-mobile?
          (re-frame/dispatch [::ui.handlers/untrack-orientation]))
        (doseq [[event-type handler] listeners]
          (.removeEventListener js/window event-type handler)))
      :render
      (fn [_] [:div])})))
