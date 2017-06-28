(ns vimsical.frontend.vims.subs
  (:require [re-frame.core :as re-frame]
            [vimsical.vims :as vims]
            [vimsical.queries.vims :as queries.vims]))

(re-frame/reg-sub
 ::vims
 (fn [[_ {vims-uid :db/uid}]]
   (re-frame/subscribe [:q queries.vims/pull-query [:db/uid vims-uid]]))
 (fn [vims] vims))

(re-frame/reg-sub
 ::vcs-vims
 (fn [[_ vims-uid]]
   (re-frame/subscribe [:q
                        queries.vims/frontend-pull-query
                        [:db/uid vims-uid]]))
 (fn [{::vims/keys [vcs] :as vims}]
   (when vcs vims)))