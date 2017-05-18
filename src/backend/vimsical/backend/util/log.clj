(ns vimsical.backend.util.log
  (:require [clojure.tools.logging :as log]))

(defmacro debug [& args] `(log/logp :debug ~@args))
(defmacro info  [& args] `(log/logp :info ~@args))
(defmacro warn  [& args] `(log/logp :warn ~@args))
(defmacro error [& args] `(log/logp :error ~@args))
(defmacro fatal [& args] `(log/logp :fatal ~@args))
