(ns vimsical.frontend.vims-list.subs
  (:require [vimsical.frontend.vcs.subs :as vcs.subs]
            [vimsical.frontend.app.subs :as app.subs]
            [vimsical.frontend.user.subs :as user.subs]
            [vimsical.vims :as vims]
            [re-frame.core :as re-frame]
            [vimsical.vcs.core :as vcs]
            [vimsical.vcs.branch :as vcs.branch]
            [vimsical.common.util.core :as util]))

(re-frame/reg-sub
 ::vimsae
 :<- [::user.subs/vimsae [:db/uid ::vims/title ::vims/branches]]
 :<- [::app.subs/vims [:db/uid]]
 (fn [[vimsae cur-vims] _]
   (remove (fn [vims] (util/=by :db/uid cur-vims vims)) vimsae)))

(re-frame/reg-sub
 ::vims-preview-branch
 (fn [[_ vims]]
   {:pre [vims]}
   (re-frame/subscribe [::vcs.subs/vcs vims]))
 (fn [{:as vcs ::vcs/keys [branches]} _]
   (util/ffilter vcs.branch/master? branches)))