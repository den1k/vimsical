(ns vimsical.frontend.player.dev
  (:require
   [vimsical.frontend.core :as core]
   [re-frisk.core :refer [enable-re-frisk!]]))

(enable-console-print!)
(enable-re-frisk!)
(println "player dev mode")

(defn on-reload
  "Called by figwheel on reload. See project.clj."
  []
  (core/mount-root))
