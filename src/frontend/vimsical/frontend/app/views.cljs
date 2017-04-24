(ns vimsical.frontend.app.views
  (:require
   [re-com.core :as re-com]
   [vimsical.frontend.nav.views :refer [nav]]
   [vimsical.frontend.vcr.views :refer [vcr]]
   [vimsical.frontend.player.views :refer [player]]
   [vimsical.frontend.quick-search.views :refer [quick-search]]
   [vimsical.frontend.window-listeners.views :refer [window-listeners]]
   [vimsical.frontend.util.re-frame :refer-macros [with-subs with-queries]]
   [vimsical.frontend.app.subs :as subs]))

(defn app []
  (fn []
    (with-subs [route [::subs/route]]
      [:div.app
       [nav]
       (case route
         :route/vcr [vcr]
         :route/player [player])
       [window-listeners]
       [quick-search]])))
