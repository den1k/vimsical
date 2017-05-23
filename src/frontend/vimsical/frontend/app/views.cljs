(ns vimsical.frontend.app.views
  (:require
   [re-com.core :as re-com]
   [vimsical.frontend.nav.views :refer [nav]]
   [vimsical.frontend.vcr.views :refer [vcr]]
   [vimsical.frontend.landing.views :refer [landing]]
   [vimsical.frontend.player.views.player :refer [player]]
   [vimsical.frontend.quick-search.views :refer [quick-search]]
   [vimsical.frontend.window-listeners.views :refer [window-listeners]]
   [vimsical.frontend.util.re-frame :refer-macros [with-subs]]
   [vimsical.frontend.app.subs :as subs]
   [vimsical.frontend.ui.subs :as ui.subs]))

(defn app []
  (fn []
    (with-subs [route      [::subs/route]
                on-mobile? [::ui.subs/on-mobile?]]
      [:div.app
       {:class (str "route-" (name route))}
       (when-not on-mobile? [nav])
       (case route
         :route/landing [landing]
         :route/vims [(if on-mobile? player vcr)])
       [window-listeners]
       [quick-search]])))