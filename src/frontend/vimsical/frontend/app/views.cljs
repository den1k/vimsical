(ns vimsical.frontend.app.views
  (:require
   [re-frame.core :as re-frame]
   [vimsical.frontend.app.handlers :as handlers]
   [vimsical.frontend.app.subs :as subs]
   [vimsical.frontend.modal.views :as views.modal]
   [vimsical.frontend.nav.views :refer [nav]]
   [vimsical.frontend.quick-search.views :refer [quick-search]]
   [vimsical.frontend.router.routes :as routes]
   [vimsical.frontend.router.subs :as router.subs]
   [vimsical.frontend.router.views :as router.views]
   [vimsical.frontend.ui.subs :as ui.subs]
   [vimsical.frontend.util.dom :refer-macros [e>]]
   [vimsical.frontend.util.re-frame :refer-macros [with-subs]]
   [vimsical.frontend.window-listeners.views :refer [window-listeners]]))

(defn route->class
  [{::routes/keys [route-handler]}]
  (str "route-" (name route-handler)))

(defn app []
  (with-subs [route      [::router.subs/route]
              modal      [::subs/modal]
              height     [::ui.subs/height]
              on-mobile? [::ui.subs/on-mobile?]]
    (router.views/title-for route)
    [:div.app
     {:class    (route->class route)
      :on-click (e> (re-frame/dispatch [::handlers/close-modal]))
      ;; height is set for landscape mode on mobile
      :style    {:height height}}
     (when (or (routes/landing? route) (not on-mobile?)) [nav])
     [views.modal/modal]
     [:div.main {:class (when modal "modal-overlay")}
      [router.views/view-for route]
      [quick-search]]
     [window-listeners]]))
