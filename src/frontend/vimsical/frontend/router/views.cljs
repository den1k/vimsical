(ns vimsical.frontend.router.views
  (:require
   [vimsical.frontend.app.subs :as app.subs]
   [vimsical.frontend.auth.views :as auth.views :refer [signup invite]]
   [vimsical.frontend.landing.views :refer [landing]]
   [vimsical.frontend.player.views.player :refer [player]]
   [vimsical.frontend.router.routes :as routes]
   [vimsical.frontend.ui.subs :as ui.subs]
   [vimsical.user :as user]
   [vimsical.vims :as vims]
   [vimsical.frontend.util.re-frame :as util.re-frame :refer [<sub]]
   [vimsical.frontend.vcr.views :refer [vcr]]
   [vimsical.common.util.core :as util]
   [vimsical.frontend.util.dom :as util.dom]))

;;
;; Views
;;

(defmulti view-for (fn [{::routes/keys [route-handler]}] route-handler))

(defmethod view-for :default [route] (throw (ex-info "No view defined for route" {:route route})))

(defmethod view-for ::routes/signup [_] [signup])
(defmethod view-for ::routes/invite [route]
  [invite (routes/get-arg route :token)])

(defmethod view-for ::routes/landing [_] [landing])

(defmethod view-for ::routes/vims [_]
  (let [vims       (<sub [::app.subs/vims [:db/uid]])
        on-moblie? (<sub [::ui.subs/on-mobile?])]
    (when vims
      [(if on-moblie? player vcr) {:vims vims}])))

;;
;; Titles (visible in browser tab)
;;

(defmulti title-for (fn [{::routes/keys [route-handler]}] route-handler))

(defmethod title-for :default [_] (util.dom/set-title "Vimsical"))

(defmethod title-for ::routes/vims [_]
  (let [{::vims/keys [title owner]} (<sub [::app.subs/vims
                                           [:db/uid ::vims/title
                                            {::vims/owner [::user/first-name
                                                           ::user/last-name]}]])
        {::user/keys [first-name last-name]} owner]
    (util.dom/set-title
     (util/space-join (or title "A vims") "by" first-name last-name))))
