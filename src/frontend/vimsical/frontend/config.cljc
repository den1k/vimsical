(ns vimsical.frontend.config)

(def debug?
  #?(:clj  false
     :cljs ^boolean js/goog.DEBUG))