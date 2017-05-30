(ns vimsical.frontend.db
  (:require
   [vimsical.vims :as vims]
   [vimsical.user :as user]
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.common.test :refer [uuid]]
   [vimsical.common.util.core :as util :include-macros true]
   [vimsical.remotes.backend.status.queries :as status.queries]
   [vimsical.frontend.quick-search.commands :as quick-search.commands]
   [vimsical.frontend.util.mapgraph :as util.mg])
  (:refer-clojure :exclude [uuid]))

(def state
  {:app/user         {:db/uid           (uuid :user)
                      ::user/first-name "Jane"
                      ::user/last-name  "Applecrust"
                      ::user/email      "kalavox@gmail.com"
                      ::user/vimsae     [(vims/new-vims (uuid "NLP Chatbot running on React Fiber") {:db/uid (uuid :user)} "NLP Chatbot running on React Fiber")
                                         (vims/new-vims (uuid "CatPhotoApp") {:db/uid (uuid :user)} "CatPhotoApp" {:js-libs vims/sub-type->libs})]}
   :app/vims         [:db/uid (uuid "CatPhotoApp")]
   :app/quick-search {:db/uid                           (uuid :quick-search)
                      :quick-search/show?               false
                      :quick-search/result-idx          0
                      :quick-search/query               ""
                      :quick-search/commands            quick-search.commands/commands
                      :quick-search/filter-idx          nil
                      :quick-search/filter-result-idx   nil
                      :quick-search/filter-category-idx nil}
   :app/libs         vims/js-libs
   :app/compilers    vims/compilers
   :app/route        :route/vims})

(def default-db
  (-> (mg/new-db)
      (mg/add-id-attr :db/uid)
      (util.mg/add-linked-entities state)))

(re-frame/reg-event-fx
 ::init
 (fn [_ _]
   {:db     default-db
    :remote {:id :backend :event [::status.queries/status]}}))

(re-frame/reg-event-fx
 ::status.queries/status-result
 (fn [_ [_ result]] (println result)))
