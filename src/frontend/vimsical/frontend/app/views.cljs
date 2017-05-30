(ns vimsical.frontend.app.views
  (:require
   [re-com.core :as re-com]
   [vimsical.frontend.app.subs :as subs]
   [vimsical.frontend.auth.views :as auth.views]
   [vimsical.frontend.landing.views :refer [landing]]
   [vimsical.frontend.nav.views :refer [nav]]
   [vimsical.frontend.player.views.player :refer [player]]
   [vimsical.frontend.quick-search.views :refer [quick-search]]
   [vimsical.frontend.ui.subs :as ui.subs]
   [vimsical.frontend.util.re-frame :refer-macros [with-subs]]
   [vimsical.frontend.vcr.views :refer [vcr]]
   [vimsical.frontend.window-listeners.views :refer [window-listeners]]))

(defn app []
  (with-subs [route      [::subs/route]
              on-mobile? [::ui.subs/on-mobile?]
              vims       [::subs/vims]]
    [:div.app
     {:class (str "route-" (name route))}
     (when-not on-mobile? [nav])
     (case route
       :route/signup         [auth.views/signup]
       :route/signup-success [auth.views/signup-success]
       :route/landing        [landing]
       :route/vims           [(if on-mobile? player vcr) {:vims vims}])
     [window-listeners]
     [quick-search]]))
