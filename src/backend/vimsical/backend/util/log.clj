(ns vimsical.backend.util.log
  (:require
   [clojure.tools.logging :as log]
   [clojure.java.io :as io])
  (:import
   (ch.qos.logback.classic.joran JoranConfigurator)
   (ch.qos.logback.classic LoggerContext)
   (org.slf4j LoggerFactory)))

;;
;; * Helpers
;;

(defn reload-logback! []
  (let [context      ^LoggerContext (LoggerFactory/getILoggerFactory)
        configurator (JoranConfigurator.)
        config       (io/resource "logback.xml")]
    (assert config)
    (.reset context)
    (.setContext configurator context)
    (.doConfigure configurator config)))

;;
;; * API
;;

(defmacro debug [& args] `(log/logp :debug ~@args))
(defmacro info  [& args] `(log/logp :info ~@args))
(defmacro warn  [& args] `(log/logp :warn ~@args))
(defmacro error [& args] `(log/logp :error ~@args))
(defmacro fatal [& args] `(log/logp :fatal ~@args))
