(ns vimsical.frontend.player.embed
  (:require [reagent.dom.server]
            [vimsical.common.util.core :as util :include-macros true]))

(def iframe-sandbox-opts
  "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/iframe"
  (util/space-join "allow-forms"
                   "allow-modals"
                   "allow-pointer-lock"
                   "allow-popups"
                   "allow-same-origin"
                   "allow-scripts"))

(defn player-iframe [opts]
  {:pre [(:src opts)]}
  [:iframe (merge {:sandbox iframe-sandbox-opts} opts)])

(defn player-script [opts]
  {:pre [(:src opts)]}
  [:script (merge {:async "async"} opts)])

(defn player-iframe-markup [opts]
  (reagent.dom.server/render-to-static-markup [player-iframe opts]))

(defn player-script-markup [opts]
  (reagent.dom.server/render-to-static-markup [player-script opts]))