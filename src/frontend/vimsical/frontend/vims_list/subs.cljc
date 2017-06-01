(ns vimsical.frontend.vims-list.subs
  (:require
   [re-frame.core :as re-frame]
   [vimsical.common.util.core :as util]
   [vimsical.frontend.app.subs :as app.subs]
   [vimsical.frontend.user.subs :as user.subs]
   [vimsical.queries.vims :as queries.vims]))

(re-frame/reg-sub
 ::vimsae
 :<- [::user.subs/vimsae queries.vims/pull-query]
 :<- [::app.subs/vims [:db/uid]]
 (fn [[vimsae cur-vims] _]
   (->> vimsae
        (remove (partial util/=by :db/uid cur-vims))
        (reverse))))
