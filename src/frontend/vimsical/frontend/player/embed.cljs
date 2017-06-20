(ns vimsical.frontend.player.embed
  (:require
   [reagent.dom.server]))

(def embed-version "0.0.1")

(def ^:private player-embed-src
  "URL of the javascript embed script"
  (let [host (.-host (.-location js/window))]
    (str "//" host "/embed/" embed-version "/embed.js")))

(defn player-iframe-markup [{:keys [db/uid] :as vims}]
  {:pre [uid]}
  (reagent.dom.server/render-to-static-markup
   [:script
    {:src           player-embed-src
     :async         true
     :data-vims-uid uid}]))
