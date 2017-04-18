(ns vimsical.frontend.styles.core
  "Core namespace for generating the the app.css stylesheet. Separate source-
  path from frontend because otherwise figwheel double reloads for .css and
  .cljs files."
  (:require [vimsical.frontend.app.style :refer [app]]
            [vimsical.frontend.vcr.style :refer [vcr]]
            [vimsical.frontend.nav.style :refer [nav]]
            [vimsical.frontend.player.style :refer [player]]
            [vimsical.frontend.views.style :refer [views]]))

(def styles
  "Read by garden. See config in project.clj"
  [app
   nav
   vcr

   ;; generic views
   views

   player])
