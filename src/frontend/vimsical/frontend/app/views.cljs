(ns vimsical.frontend.app.views
  (:require
   [re-frame.core :as re-frame]
   [vimsical.frontend.app.handlers :as handlers]
   [vimsical.frontend.app.subs :as subs]
   [vimsical.frontend.auth.views :as auth.views]
   [vimsical.frontend.landing.views :refer [landing]]
   [vimsical.frontend.modal.views :as views.modal]
   [vimsical.frontend.nav.views :refer [nav]]
   [vimsical.frontend.player.views.player :refer [player]]
   [vimsical.frontend.quick-search.views :refer [quick-search]]
   [vimsical.frontend.ui.subs :as ui.subs]
   [vimsical.frontend.util.dom :refer-macros [e>]]
   [vimsical.frontend.util.re-frame :refer-macros [with-subs]]
   [vimsical.frontend.vcr.views :refer [vcr]]
   [vimsical.frontend.window-listeners.views :refer [window-listeners]]))

(defn app []
  (with-subs [route      [::subs/route]
              vims       [::subs/vims]
              modal      [::subs/modal]
              on-mobile? [::ui.subs/on-mobile?]
              height     [::ui.subs/height]]
    [:div.app
     {:class    (str "route-" (name route))
      :on-click (e> (re-frame/dispatch [::handlers/close-modal]))
      ;; height is set for landscape mode on mobile
      :style    {:height height}}
     (when-not on-mobile? [nav])
     [views.modal/modal]
     [:div.main
      {:class (when modal "modal-overlay")}
      (case route
        :route/signup [auth.views/signup]
        :route/landing [landing]
        :route/vims [(if on-mobile? player vcr) {:vims vims}])
      [quick-search]]
     [window-listeners]]))
