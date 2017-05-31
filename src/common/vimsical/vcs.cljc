(ns vimsical.vcs
  (:require
   [vimsical.vims :as vims]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.core :as vcs]
   [vimsical.frontend.vcs.db :as vcs.db]
   [vimsical.common.uuid :refer [uuid]])
  (:refer-clojure :exclude [uuid]))

(defn init-vims [{:as vims :keys [db/uid] ::vims/keys [branches]}]
  (let [master             (branch/master branches)
        vcs-state          (vcs/empty-vcs branches)
        vcs-frontend-state {:db/uid             (uuid)
                            ::vcs.db/branch-uid (:db/uid master)
                            ::vcs.db/delta-uid  nil}
        vcs-entity         (merge vcs-state vcs-frontend-state)]
    (assoc vims ::vims/vcs vcs-entity)))
