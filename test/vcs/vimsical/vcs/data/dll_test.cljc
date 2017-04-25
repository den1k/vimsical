(ns vimsical.vcs.data.dll-test
  (:require
   #?(:clj  [clojure.test :as t]
      :cljs [cljs.test :as t :include-macros true])
   [vimsical.vcs.data.dll :as dll])
  #?(:clj (:import [vimsical.vcs.data.dll DoublyLinkedList])))

(def test-kfn str)

(t/deftest ds-dll
  (t/testing "new dll"
    (let [dll (dll/doubly-linked-list test-kfn)]
      (t/is (dll/dll? dll))
      (t/is (nil? (.-head dll)))
      (t/is (nil? (.-tail dll)))
      (t/is (empty? (.-m dll)))))
  (t/testing "new dll with values"
    (let [dll (dll/doubly-linked-list test-kfn 1 2 3)]
      (t/is (= '(1 2 3) (seq dll)))
      (t/is (= "1" (.-head dll)))
      (t/is (= "3" (.-tail dll)))))
  (t/testing "add after/before, dll/update, disj, nth"
    (let [dll (dll/doubly-linked-list test-kfn 1 2 3)]
      (t/testing "add-after"
        (let [dll-mid  (dll/add-after dll 1 100)
              dll-tail (dll/add-after dll 3 100)]
          (t/is (= '(1 100 2 3) (seq dll-mid)))
          (t/is (= '(1 2 3 100) (seq dll-tail)))
          (t/is (= "100" (.-tail dll-tail)))))
      (t/testing "add-before"
        (let [dll-mid  (dll/add-before dll 2 100)
              dll-head (dll/add-before dll 1 100)]
          (t/is (= '(1 100 2 3) (seq dll-mid)))
          (t/is (= '(100 1 2 3) (seq dll-head)))
          (t/is (= "100" (.-head dll-head)))))
      (t/testing "replace"
        (let [dll-mid  (dll/replace dll 2 1000)
              dll-head (dll/replace dll 1 1000)
              dll-tail (dll/replace dll 3 1000)]
          (t/is (= '(1 1000 3) (seq dll-mid)))
          (t/is (= '(1000 2 3) (seq dll-head)))
          (t/is (= "1000" (.-head dll-head)))
          (t/is (= '(1 2 1000) (seq dll-tail)))
          (t/is (= "1000" (.-tail dll-tail)))))
      (t/testing "disj"
        (let [dll-mid  (disj dll 2)
              dll-head (disj dll 1)
              dll-tail (disj dll 3)]
          (t/is (= '(1 3) (seq dll-mid)))
          (t/is (= '(2 3) (seq dll-head)))
          (t/is (= "2" (.-head dll-head)))
          (t/is (= '(1 2) (seq dll-tail)))
          (t/is (= "2" (.-tail dll-tail)))))
      (t/testing "get"
        (t/is (= 2 (get dll 2)))
        (t/is (= 100 (get dll :not-there 100))))
      (t/testing "update"
        (let [dll-upd (dll/update dll 2 + 10)]
          (t/is (= '(1 12 3) (seq dll-upd)))))
      (t/testing "first"
        (t/is (= 1 (first dll))))
      (t/testing "last"
        (t/is (= 3 (last dll))))
      (t/testing "next"
        (t/is (= '(2 3) (next dll))))
      (t/testing "peek"
        (t/is (= 3 (peek dll))))
      (t/testing "nth"
        (t/is (= 1 (nth dll 0)))
        (t/is (= 3 (nth dll 2)))
        #?(:clj (t/is (thrown? Exception (nth dll 1000)))))))
  (t/testing "range"
    (let [dll (into (dll/doubly-linked-list str) (range 1 11))]
      (t/is (= '(3 4) (dll/subrange dll 3 5)))
      (t/is (= '(3 4) (take 2 (dll/subrange dll 3))))
      (t/is (= '(8 9 10) (dll/subrange dll 8 1000))))))
