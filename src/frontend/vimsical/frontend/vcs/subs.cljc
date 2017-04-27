(ns vimsical.frontend.vcs.subs
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.frontend.vcs.queries :as queries]
   [vimsical.vcs.core :as vcs]))


(re-frame/reg-sub
 ::vims-vcs
 (fn [db [_]]
   (:vims/vcs
    (mg/pull-link db queries/vims-vcs :app/vims))))

(re-frame/reg-sub
 ::file-string
 :<- [::vims-vcs]
 (fn [vcs [_ {:keys [db/id] :as file}]]
   (vcs/file-string vcs id)))
