(ns vimsical.frontend.db
  (:refer-clojure :exclude [uuid])
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.common.test :refer [uuid]]
   [vimsical.frontend.quick-search.commands :as quick-search.commands]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.vcs.compiler :as compiler]
   [vimsical.vcs.lib :as lib]))

(def js-libs
  [{:db/uid        (uuid :lib-js-jquery)
    ::lib/name    "jQuery"
    ::lib/type     :text
    ::lib/sub-type :javascript
    ::lib/src      "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.1.1/jquery.min.js"}])

(def compilers
  [{:db/uid                (uuid :babel-compiler)
    ::compiler/name        "Babel"
    ::compiler/type        :text
    ::compiler/sub-type    :babel
    ::compiler/to-sub-type :javascript}])

(def state
  {:app/user         {:db/uid (uuid)}
   :app/vims         nil
   :app/quick-search {:db/uid                           (uuid :quick-search)
                      :quick-search/show?               false
                      :quick-search/result-idx          0
                      :quick-search/query               ""
                      :quick-search/commands            quick-search.commands/commands
                      :quick-search/filter-idx          nil
                      :quick-search/filter-result-idx   nil
                      :quick-search/filter-category-idx nil}
   :app/libs         js-libs
   :app/compilers    compilers
   :app/route        :route/landing
   :app/modal        nil})

;;
;; * Mapgraph db
;;

(defn new-db
  [state]
  (-> (mg/new-db)
      (mg/add-id-attr :db/uid)
      (util.mg/add-linked-entities state)))

(def default-db (new-db state))

;;
;; * Re-frame
;;

(re-frame/reg-event-db ::init (constantly default-db))
(re-frame/reg-sub ::db (fn [db _] db))
