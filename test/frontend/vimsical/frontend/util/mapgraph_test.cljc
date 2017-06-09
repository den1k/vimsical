(ns frontend.vimsical.frontend.util.mapgraph-test
  #?@(:clj
      [(:require
        [clojure.test :as t]
        [com.stuartsierra.mapgraph :as mg]
        [vimsical.frontend.util.mapgraph :as sut])]
      :cljs
      [(:require
        [cljs.test :as t :include-macros true]
        [com.stuartsierra.mapgraph :as mg]
        [vimsical.frontend.util.mapgraph :as sut])]))

(def db
  (-> {}
      (mg/add-id-attr :id)
      (mg/add {:id 1 :children [{:id 2 :children [{:id 3}]}]})))

(t/deftest remove-entity-test
  (t/is (= (dissoc db [:id 3]) (sut/remove-entity db {:id 3})))
  (t/is (= (dissoc db [:id 3] [:id 2]) (sut/remove-entity db {:id 2 :children [{:id 3}]})))
  (t/is (= (mg/add-id-attr {} :id) (sut/remove-entity db {:id 1 :children [{:id 2 :children [{:id 3}]}]}))))

(t/deftest add-join-test
  (let [db'   (-> {}
                  (mg/add-id-attr :id)
                  (mg/add {:id 1 :children [{:id 2}]}))]
    (t/is (= db (sut/add-join db' {:id 2} :children {:id 3})))))
