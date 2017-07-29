(ns frontend.vimsical.frontend.util.subgraph-test
  #?@(:clj
      [(:require
        [clojure.test :as t]
        [vimsical.subgraph :as sg]
        [vimsical.frontend.util.subgraph :as sut])]
      :cljs
      [(:require
        [cljs.test :as t :include-macros true]
        [vimsical.subgraph :as sg]
        [vimsical.frontend.util.subgraph :as sut])]))

(def db
  (-> (sg/new-db)
      (sg/add-id-attr :id)
      (sg/add {:id 1 :children [{:id 2 :children [{:id 3}]}]})))

(t/deftest add-join-test
  (t/testing "Single id-attr"
    (let [db'   (-> (sg/new-db)
                    (sg/add-id-attr :id)
                    (sg/add {:id 1 :children [{:id 2}]}))]
      (t/is (= db (sut/add-join db' {:id 2} :children {:id 3})))))
  (t/testing "Multiple id-attrs"
    (let [db   (-> (sg/new-db)
                   (sg/add-id-attr :id :id2)
                   (sg/add {:id 1 :children [{:id 2 :children [{:id2 3}]}]}))
          db'   (-> (sg/new-db)
                    (sg/add-id-attr :id :id2)
                    (sg/add {:id 1 :children [{:id 2}]}))]
      (t/is (= db (sut/add-join db' {:id 2} :children {:id2 3}))))))
