(ns vimsical.frontend.window-listeners.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [vimsical.frontend.util.dom :refer-macros [e-> e->> e>]]
            [vimsical.frontend.quick-search.handlers :as quick-search]))

;;
;; * Shortcuts
;;

(def ^:const ^:private key-map
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
    (.preventDefault e)                 ;; prevent typing
    (case shortcut
      :option-forwardslash (re-frame/dispatch [::quick-search/toggle])
      :esc (re-frame/dispatch [::quick-search/close])
      nil)))

(defn window-listeners []
  (let [listeners {"keydown" handle-shortcut}]
    (reagent/create-class
     {:component-did-mount
      #(doseq [[event-type handler] listeners]
         (.addEventListener js/window event-type handler))
      :component-will-unmount
      #(doseq [[event-type handler] listeners]
         (.removeEventListener js/window event-type handler))
      :render
      (fn [] [:div])})))
