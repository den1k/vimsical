(ns vimsical.frontend.player.dev
  (:require
   [vimsical.frontend.player.core :as core]))

(enable-console-print!)
(println "player dev mode")

(defn on-reload
  "Called by figwheel on reload. See project.clj."
  []
  (core/mount-root))
