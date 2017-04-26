(ns vimsical.vcs.data.dll-test
  (:require
   #?(:clj  [clojure.test :as t :refer [is deftest testing]]
      :cljs [cljs.test :as t :refer-macros [is deftest testing]])
   [vimsical.vcs.data.dll :as dll])
  #?(:clj (:import [vimsical.vcs.data.dll DoublyLinkedList])))

(def test-kfn str)

(deftest ds-dll
  (testing "new dll"
    (let [dll (dll/doubly-linked-list test-kfn)]
      (is (dll/dll? dll))
      (is (nil? (.-head dll)))
      (is (nil? (.-tail dll)))
      (is (empty? (.-m dll)))))
  (testing "new dll with values"
    (let [dll (dll/doubly-linked-list test-kfn 1 2 3)]
      (is (= '(1 2 3) (seq dll)))
      (is (= "1" (.-head dll)))
      (is (= "3" (.-tail dll)))))
  (testing "add after/before, dll/update, disj, nth"
    (let [dll (dll/doubly-linked-list test-kfn 1 2 3)]
      (testing "add-after"
        (let [dll-mid  (dll/add-after dll 1 100)
              dll-tail (dll/add-after dll 3 100)]
          (is (= '(1 100 2 3) (seq dll-mid)))
          (is (= '(1 2 3 100) (seq dll-tail)))
          (is (= "100" (.-tail dll-tail)))))
      (testing "add-before"
        (let [dll-mid  (dll/add-before dll 2 100)
              dll-head (dll/add-before dll 1 100)]
          (is (= '(1 100 2 3) (seq dll-mid)))
          (is (= '(100 1 2 3) (seq dll-head)))
          (is (= "100" (.-head dll-head)))))
      (testing "replace"
        (let [dll-mid  (dll/replace dll 2 1000)
              dll-head (dll/replace dll 1 1000)
              dll-tail (dll/replace dll 3 1000)]
          (is (= '(1 1000 3) (seq dll-mid)))
          (is (= '(1000 2 3) (seq dll-head)))
          (is (= "1000" (.-head dll-head)))
          (is (= '(1 2 1000) (seq dll-tail)))
          (is (= "1000" (.-tail dll-tail)))))
      (testing "disj"
        (let [dll-mid  (disj dll 2)
              dll-head (disj dll 1)
              dll-tail (disj dll 3)]
          (is (= '(1 3) (seq dll-mid)))
          (is (= '(2 3) (seq dll-head)))
          (is (= "2" (.-head dll-head)))
          (is (= '(1 2) (seq dll-tail)))
          (is (= "2" (.-tail dll-tail)))))
      (testing "get"
        (is (= 2 (get dll 2)))
        (is (= 100 (get dll :not-there 100))))
      (testing "update"
        (let [dll-upd (dll/update dll 2 + 10)]
          (is (= '(1 12 3) (seq dll-upd)))))
      (testing "first"
        (is (= 1 (first dll))))
      (testing "last"
        (is (= 3 (last dll))))
      (testing "next"
        (is (= '(2 3) (next dll))))
      (testing "peek"
        (is (= 3 (peek dll))))
      (testing "nth"
        (is (= 1 (nth dll 0)))
        (is (= 3 (nth dll 2)))
        #?(:clj (is (thrown? Exception (nth dll 1000)))))))
  (testing "range"
    (let [dll (into (dll/doubly-linked-list str) (range 1 11))]
      (is (= '(3 4) (dll/subrange dll 3 5)))
      (is (= '(3 4) (take 2 (dll/subrange dll 3))))
      (is (= '(8 9 10) (dll/subrange dll 8 1000))))))
