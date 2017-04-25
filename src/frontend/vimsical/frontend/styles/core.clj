(ns vimsical.frontend.styles.core
  "Core namespace for generating the the app.css stylesheet. Separate source-
  path from frontend because otherwise figwheel double reloads for .css and
  .cljs files."
  (:require [vimsical.frontend.app.style :as app]
            [vimsical.frontend.vcr.style :refer [vcr]]
            [vimsical.frontend.nav.style :refer [nav]]
            [vimsical.frontend.player.style :refer [player]]
            [vimsical.frontend.views.style :refer [views]]
            [vimsical.frontend.styles.text :refer [text]]
            [vimsical.frontend.quick-search.style :refer [quick-search]]
            [vimsical.frontend.landing.style :refer [landing]]))

(def styles
  "Read by garden. See config in project.clj"
  [app/defaults
   nav
   landing
   vcr

   quick-search

   ;; generic views
   views

   player

   ;; typography
   text
   ])
