(ns vimsical.vcs.edit-event-test
  (:require
   [vimsical.vcs.edit-event :as sut]
   #?(:clj  [clojure.test :as t]
      :cljs [cljs.test :as t :include-macros true])))

(t/deftest prospective-index-offset-test
  (t/testing "str/ins"
    (t/is (= 1 (sut/prospective-idx-offset {::sut/op :str/ins ::sut/idx 0 ::sut/diff "a"})))
    (t/is (= 2 (sut/prospective-idx-offset {::sut/op :str/ins ::sut/idx 0 ::sut/diff "ab"}))))
  (t/testing "str/rem"
    (t/is (= 0 (sut/prospective-idx-offset {::sut/op :str/rem ::sut/idx 0 ::sut/amt 1})))
    (t/is (= 0 (sut/prospective-idx-offset {::sut/op :str/rem ::sut/idx 0 ::sut/amt 10}))))
  (t/testing "str/rplc"
    (t/is (= 0  (sut/prospective-idx-offset {::sut/op :str/rplc ::sut/idx 0 ::sut/amt 1 ::sut/diff "a"})))
    (t/is (= 1  (sut/prospective-idx-offset {::sut/op :str/rplc ::sut/idx 0 ::sut/amt 1 ::sut/diff "ab"})))
    (t/is (= -1 (sut/prospective-idx-offset {::sut/op :str/rplc ::sut/idx 2 ::sut/amt 2 ::sut/diff "a"}))))
  (t/testing "crsr/mv"
    (t/is (= 0 (sut/prospective-idx-offset {::sut/op :crsr/mv ::sut/idx 100}))))
  (t/testing "crsr/sel"
    (t/is (= 20 (sut/prospective-idx-offset {::sut/op :crsr/sel ::sut/range [80 100]})))
    (t/is (= -20 (sut/prospective-idx-offset {::sut/op :crsr/sel ::sut/range [100 80]})))))
