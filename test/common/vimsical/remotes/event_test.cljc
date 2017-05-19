(ns vimsical.remotes.event-test
  (:require
   [clojure.spec :as s]
   [vimsical.remotes.backend.auth.commands :as commands]
   #?(:clj [orchestra.spec.test :as st])
   [vimsical.remotes.event :as sut]
   [vimsical.user :as user]
   #?(:clj [clojure.test :as t]
      :cljs [cljs.test :as t :include-macros true])))

#?(:clj (st/instrument))

(t/deftest event-validation-test
  (let [event-id   ::commands/login
        event-args {::user/email "abf" ::user/password "anaba"}]
    (t/is (s/valid? ::sut/event [event-id event-args]))
    (t/is (not (s/valid? ::sut/event [event-id (dissoc event-args ::user/password)])))))

(t/deftest namespace-helpers-test
  (let [event [::foo 123]]
    (t/is (= [::foo-result 456] (sut/event-result event 456)))
    (t/is (= [::foo-error 456] (sut/event-error event 456)))))
