(ns vimsical.frontend.app.subs
  (:require
   [vimsical.subgraph :as sg]
   [re-frame.core :as re-frame]
   [vimsical.user :as user]
   [vimsical.vims :as vims]
   [vimsical.queries.user :as queries.user]
   [vimsical.queries.vims :as queries.vims]
   [vimsical.frontend.vcs.subs :as vcs.subs]))

(re-frame/reg-sub
 ::modal
 (fn [db _]
   (:app/modal db)))

(re-frame/reg-sub-raw
 ::user
 (fn [db [_ ?pattern]]
   (re-frame/subscribe [:q [:app/user (or ?pattern queries.user/pull-query)]])))

(re-frame/reg-sub-raw
 ::vims
 (fn [db [_ ?pattern]]
   (re-frame/subscribe [:q [:app/vims (or ?pattern queries.vims/pull-query)]])))

(re-frame/reg-sub-raw
 ::vims-branch-uid
 (fn [db _]
    (re-frame/subscribe [::vcs.subs/branch-uid (:app/vims @db)])))

(re-frame/reg-sub-raw
 ::libs
 (fn [db [_ ?pattern]]
   (re-frame/subscribe [:q [:app/libs (or ?pattern '[*])]])))

(re-frame/reg-sub-raw
 ::compilers
 (fn [db [_ ?pattern]]
   (re-frame/subscribe [:q [:app/compilers (or ?pattern '[*])]])))

(re-frame/reg-sub-raw
 ::vims-info
 (fn [db [_ ?vims]]
   (let [db      @db
         pattern [::vims/title {::vims/owner [::user/first-name ::user/last-name ::user/email]}]]
     (re-frame/subscribe
      (if ?vims
        [:q
         pattern
         (sg/ref-to db ?vims)]
        [::vims pattern])))))
