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

(defn reload-logback!
  ([] (reload-logback! (io/file (io/resource "logback.xml"))))
  ([config]
   (let [context      ^LoggerContext (LoggerFactory/getILoggerFactory)
         ^JoranConfigurator configurator (JoranConfigurator.)]
     (assert config)
     (.reset context)
     (.setContext configurator context)
     (.doConfigure configurator ^java.io.File config))))

;;
;; * API
;;

(defmacro debug [& args] `(log/logp :debug ~@args))
(defmacro info  [& args] `(log/logp :info ~@args))
(defmacro warn  [& args] `(log/logp :warn ~@args))
(defmacro error [& args] `(log/logp :error ~@args))
(defmacro fatal [& args] `(log/logp :fatal ~@args))

(comment
  (reload-logback!)
  (reload-logback!
   (io/file "/Users/julien/projects/vimsical/resources/backend/logback/test/logback.xml")))
