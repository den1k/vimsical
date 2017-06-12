(ns vimsical.frontend.router.views
  (:require
   [vimsical.frontend.app.subs :as app.subs]
   [vimsical.frontend.auth.views :as auth.views :refer [signup]]
   [vimsical.frontend.landing.views :refer [landing]]
   [vimsical.frontend.player.views.player :refer [player]]
   [vimsical.frontend.router.routes :as routes]
   [vimsical.frontend.ui.subs :as ui.subs]
   [vimsical.frontend.util.re-frame :as util.re-frame :refer [<sub]]
   [vimsical.frontend.vcr.views :refer [vcr]]))

(defmulti  view-for (fn [{::routes/keys [route-handler]}] route-handler))

(defmethod view-for :default [route] (throw (ex-info "No view defined for route" {:route route})))

(defmethod view-for ::routes/signup  [_] [signup])

(defmethod view-for ::routes/landing [_] [landing])

(defmethod view-for ::routes/vims    [_]
  (let [vims       (<sub [::app.subs/vims [:db/uid]])
        on-moblie? (<sub [::ui.subs/on-mobile?])]
    (when vims
      [(if on-moblie? player vcr) {:vims vims}])))
