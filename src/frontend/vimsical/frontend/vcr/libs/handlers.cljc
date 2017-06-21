(ns vimsical.frontend.vcr.libs.handlers
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.util.mapgraph :as util.mg]
            [vimsical.frontend.util.re-frame :as util.re-frame]
            [vimsical.frontend.app.subs :as app.subs]
            [vimsical.frontend.app.vims.subs :as app.vims.subs]
            [vimsical.vcs.lib :as vcs.lib]
            [vimsical.frontend.vcs.handlers :as vcs.handlers]
            [vimsical.common.util.core :as util]))

(re-frame/reg-event-fx
 ::add-lib
 [(util.re-frame/inject-sub [::app.subs/vims])]
 (fn [{:keys [db] vims ::app.subs/vims} [_ lib]]
   {:dispatch [::vcs.handlers/add-lib vims lib]}))

(re-frame/reg-event-fx
 ::remove-lib
 [(util.re-frame/inject-sub [::app.subs/vims])]
 (fn [{:keys [db] vims ::app.subs/vims} [_ lib]]
   {:dispatch [::vcs.handlers/remove-lib vims lib]}))

(re-frame/reg-event-fx
 ::toggle-lib
 [(util.re-frame/inject-sub [::app.vims.subs/added-libs])
  (util.re-frame/inject-sub [::app.subs/vims])]
 (fn [{:keys      [db]
       added-libs ::app.vims.subs/added-libs
       vims       ::app.subs/vims}
      [_ lib]]
   (let [dispatch (if (util/ffilter (util/=by ::vcs.lib/src lib) added-libs)
                    ::vcs.handlers/remove-lib
                    ::vcs.handlers/add-lib)]
     {:dispatch [dispatch vims lib]})))
