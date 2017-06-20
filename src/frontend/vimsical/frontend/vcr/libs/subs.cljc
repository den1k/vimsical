(ns vimsical.frontend.vcr.libs.subs
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.app.subs :as app.subs]
            [vimsical.frontend.app.vims.subs :as app.vims.subs]
            [vimsical.vcs.lib :as vcs.lib]
            [vimsical.common.util.core :as util]))

(defn- by-sub-type [libs]
  (group-by ::vcs.lib/sub-type libs))

(defn annotate-libs [libs added-libs]
  (let [src->added-libs (util/project ::vcs.lib/src added-libs)
        mark-added      (map (fn [{::vcs.lib/keys [src] :as lib}]
                               (cond-> lib
                                 (get src->added-libs src)
                                 (assoc :added? true))))]
    (into [] mark-added libs)))

(re-frame/reg-sub
 ::libs-by-sub-type
 :<- [::app.subs/libs]
 :<- [::app.vims.subs/added-libs]
 (fn [[libs added-libs]]
   (-> (annotate-libs libs added-libs)
       (by-sub-type))))

