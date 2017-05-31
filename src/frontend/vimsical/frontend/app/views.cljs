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
   [vimsical.frontend.util.dom :refer-macros [e>]]
   [vimsical.frontend.app.subs :as subs]
   [vimsical.frontend.ui.subs :as ui.subs]
   [vimsical.frontend.auth.views :as auth.views]
   [vimsical.frontend.modal.views :refer [modal]]
   [vimsical.frontend.app.handlers :as handlers]
   [re-frame.core :as re-frame]))

(defn app []
  (with-subs [route      [::subs/route]
              vims       [::subs/vims]
              modal      [::subs/modal]
              on-mobile? [::ui.subs/on-mobile?]]
    [modal]
    [:div.app
     {:class    (str "route-" (name route))
      :on-click (e> (re-frame/dispatch [::handlers/close-modal]))}
     (when-not on-mobile? [nav])
     [:div.main
      {:class (when modal "modal-overlay")}
      (case route
        :route/signup [auth.views/signup]
        :route/landing [landing]
        :route/vims [(if on-mobile? player vcr) {:vims vims}])
      [quick-search]]
     [window-listeners]]))