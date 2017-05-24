(ns vimsical.backend.handlers.multi-test
  (:require
   [clojure.spec :as s]
   [clojure.test :as t]
   [orchestra.spec.test :as st]
   [vimsical.backend.handlers.multi :as sut]))

(st/instrument)

;;
;; * Request context
;;

(def request-context
  {:request
   {:body
    [:vimsical.remotes.backend.auth.commands/login
     {:vimsical.user/email "foo" :vimsical.user/password "asd"}]}})

(t/deftest request-context-spec-test
  (t/is (s/valid? ::sut/request-context request-context))
  (t/is (not (s/valid? ::sut/request-context (assoc-in request-context [:request :body 0] 1))))
  (t/is (not (s/valid? ::sut/request-context (assoc-in request-context [:request :body 1 :vimsical.user/email] 1)))))

;;
;; * Response context
;;

(def response-context
  (assoc request-context :response
         {:body {:vimsical.user/email "as" :vimsical.user/password "asd"}}))

(t/deftest response-context-spec-test
  (t/is (s/valid? ::sut/response-context response-context))
  (t/is (not (s/valid? ::sut/response-context (assoc-in response-context [:response :body :vimsical.user/email] 1)))))
