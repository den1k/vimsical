(ns vimsical.frontend.styles.core
  "Core namespace for generating the the app.css stylesheet. Separate source-
  path from frontend because otherwise figwheel double reloads for .css and
  .cljs files."
  (:require [vimsical.frontend.app.style :refer [app]]
            [vimsical.frontend.vcr.style :refer [vcr]]
            [vimsical.frontend.nav.style :refer [nav]]
            [vimsical.frontend.player.style :refer [player]]
            [vimsical.frontend.views.style :refer [views]]
            [vimsical.frontend.styles.text :refer [text]]
            [vimsical.frontend.quick-search.style :refer [quick-search]]))

(def styles
  "Read by garden. See config in project.clj"
  [app
   nav
   vcr

   quick-search

   ;; generic views
   views

   player

   ;; typography
   text
   ])
