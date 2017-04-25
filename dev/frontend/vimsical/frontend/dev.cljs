(ns vimsical.frontend.dev
  (:require
   [vimsical.frontend.core :as core]
   [re-frisk.core :refer [enable-re-frisk!]]))

(enable-console-print!)
(enable-re-frisk!)
(println "dev mode")

(defn on-reload
  "Called by figwheel on reload. See project.clj."
  []
  (core/mount-root))
