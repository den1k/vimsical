(ns vimsical.frontend.code-editor.interop-test
  (:require [vimsical.frontend.code-editor.interop :as sut]
            #?(:clj [clojure.test :as t]
               :cljs [cljs.test :as t :include-macros true])))

(t/deftest idx->pos-test
  (let [s "1234\n5678"]
    (t/are [pos idx] (t/is (= pos (sut/idx->pos idx s)))
      {:line 1, :col 1} 0
      {:line 1, :col 2} 1
      {:line 1, :col 3} 2
      {:line 1, :col 4} 3
      {:line 1, :col 5} 4
      {:line 2, :col 1} 5
      {:line 2, :col 2} 6
      {:line 2, :col 3} 7
      {:line 2, :col 4} 8
      {:line 2, :col 5} 9
      nil 10)))
