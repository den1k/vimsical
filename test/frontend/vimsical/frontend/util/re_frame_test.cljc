;; XXX this makes the backen-dtest target fail??

;; (ns  vimsical.frontend.util.re-frame-test
;;   #?@(:clj
;;       [(:require
;;         [clojure.test :as t :refer [are deftest is]]
;;         [orchestra.spec.test :as st]
;;         [re-frame.core :as re-frame]
;;         [vimsical.frontend.util.re-frame :as sut])]
;;       :cljs
;;       [(:require
;;         [clojure.spec.test :as st]
;;         [clojure.test :as t :refer-macros [are deftest is]]
;;         [re-frame.core :as re-frame]
;;         [vimsical.frontend.util.re-frame :as sut])]))

;; (re-frame/reg-sub ::foo (fn [db _] :foo))
;; (re-frame/reg-sub ::bar (fn [db _] :bar))
;; (re-frame/reg-sub ::foobar :<- [::foo] :<- [::bar] (fn [_ _] (foo!)))


;; TODO
;; 1. Find out if an injected sub that's not deref in a reagent component will update
;; 2. Test disposal logic (how can we do it without a mounting a reagent root?)
