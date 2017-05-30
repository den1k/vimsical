(ns vimsical.frontend.config
  (:require [clojure.spec :as s]))

(def debug?
  #?(:clj  false
     :cljs (let [dbg? ^boolean goog.DEBUG]
             (s/check-asserts dbg?)
             dbg?)))