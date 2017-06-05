(ns vimsical.backend.components.session-store-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [ring.middleware.session.store :as store]
   [vimsical.common.test :refer [uuid]]
   [vimsical.backend.components.session-store.fixture :refer [*session-store* session-store]]
   [orchestra.spec.test :as st]
   [vimsical.user :as user]))

(st/instrument)

(use-fixtures :each session-store)

(deftest session-store-test
  (let [expect {::user/uid (uuid)}
        k      (store/write-session *session-store* nil expect)
        actual (store/read-session *session-store* k)]
    (is (= expect actual))
    (store/delete-session *session-store* k)
    (is (nil? (store/read-session *session-store* k)))))
