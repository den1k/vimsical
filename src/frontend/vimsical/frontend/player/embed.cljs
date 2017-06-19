(ns vimsical.frontend.player.embed
  (:require
   [reagent.dom.server]))

(def ^:private player-embed-src
  "URL of the javascript embed script"
  (let [host (.-host (.-location js/window))
        path "embed.js"]
    (str "//" host "/" path)))

(defn- player-iframe-src
  "URL of the embedded iframe"
  [{:keys [db/uid]}]
  {:pre [uid]}
  (let [origin (.-origin (.-location js/window))
        path   "/embed/"]
    (str origin path uid)))

(defn player-iframe-markup [vims]
  (reagent.dom.server/render-to-static-markup
   [:script
    {:src             player-embed-src
     :async           true
     :data-iframe-src (player-iframe-src vims)}]))
