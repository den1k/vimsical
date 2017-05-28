(ns vimsical.backend.util.async-test
  (:require
   [clojure.core.async :as a]
   [clojure.core.async.impl.protocols :as ap]
   [clojure.test :as t]
   [vimsical.backend.util.async :as sut]))

(t/deftest errors-test
  (let [e (ex-info "FOO" {})]
    (t/testing "blocks"
      (t/is (= e (a/<!! (sut/go-try (throw e)))))
      (t/is (= e (a/<!! (sut/thread-try (throw e))))))
    (t/testing "nesting"
      (t/is (= e (a/<!! (sut/go-try (sut/<? (sut/thread-try (throw e))))))))
    (t/testing "rethrows"
      (t/is (thrown? clojure.lang.ExceptionInfo (sut/<?? (sut/go-try (throw e))))))
    (t/testing "alts"
      (t/testing "cleanup"
        (let [a (sut/go-try
                 (a/<! (a/timeout 100))
                 (throw e))
              b (sut/go-try (throw e))]
          (t/is (thrown? clojure.lang.ExceptionInfo (sut/alts?? [a b])))
          (t/is (ap/closed? a))
          (t/is (ap/closed? b))))
      (t/testing "no cleanup"
        (let [a (sut/go-try
                 (a/<! (a/timeout 100))
                 (throw e))
              b (sut/go-try (throw e))]
          (t/is (thrown? clojure.lang.ExceptionInfo (sut/alts?? [a b] false)))
          (t/is (not (ap/closed? a)))
          (t/is (ap/closed? b)))))))

(t/deftest parallel-takes-test
  (t/is (= [0 1 2 3]
           (a/<!! (sut/parallel-promises
                   [(a/go (a/<! (a/timeout 1000)) 0)
                    (a/go (a/<! (a/timeout 100)) 1)
                    (a/go (a/<! (a/timeout 10)) 2)
                    (a/go (a/<! (a/timeout 1)) 3)]))))
  (let [e (ex-info "FOO" {})]
    (t/is (= e (a/<!! (sut/parallel-promises?
                       [(a/go (a/<! (a/timeout 1000)) 0)
                        (a/go (a/<! (a/timeout 100)) 1)
                        (a/go (a/<! (a/timeout 10)) 2)
                        (a/go (a/<! (a/timeout 300)) e)]))))
    (t/is (thrown? clojure.lang.ExceptionInfo
                   (sut/<?? (sut/parallel-promises?
                             [(a/go (a/<! (a/timeout 1000)) 0)
                              (a/go (a/<! (a/timeout 100)) 1)
                              (a/go (a/<! (a/timeout 10)) 2)
                              (a/go (a/<! (a/timeout 300)) e)]))))))
