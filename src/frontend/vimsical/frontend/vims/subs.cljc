(ns vimsical.frontend.vims.subs
  (:require [re-frame.core :as re-frame]
            [vimsical.queries.vims :as queries.vims]
            [vimsical.frontend.util.mapgraph :as util.mg]))

(re-frame/reg-sub
 ::vims
 (fn [[_ {vims-uid :db/uid}]]
   (re-frame/subscribe [:q queries.vims/pull-query [:db/uid vims-uid]]))
 (fn [vims] vims))
