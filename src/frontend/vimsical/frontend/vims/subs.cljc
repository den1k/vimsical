(ns vimsical.frontend.vims.subs
  (:require [re-frame.core :as re-frame]
            [vimsical.vims :as vims]
            [vimsical.queries.vims :as queries.vims]
            [vimsical.vcs.branch :as vcs.branch]))

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

(re-frame/reg-sub
 ::branches
 (fn [[_ vims]]
    (re-frame/subscribe [::vims vims]))
 (fn [vims _]
    (::vims/branches vims)))

(re-frame/reg-sub
 ::master-branch
 (fn [[_ vims]]
    (re-frame/subscribe [::branches vims]))
 (fn [branches _]
    (vcs.branch/master branches)))

(re-frame/reg-sub
 ::snapshot-libs
 (fn [[_ vims]]
    (re-frame/subscribe [::master-branch vims]))
 (fn [master-branch _]
    (::vcs.branch/libs master-branch)))