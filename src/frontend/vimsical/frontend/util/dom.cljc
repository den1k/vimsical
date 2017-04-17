(ns vimsical.frontend.util.dom
  "Util.dom is meant to be used exclusively in the browser. To avoid
  double reloads of .clj style fyles through figwheel, we made this a .cljc file."
  (:require [vimsical.common.util.core :as util]))

;; There is a trap when writing DOM event handlers.  This looks innocent enough:
;;
;;     :on-mouse-out  #(reset! my-over-atom false)
;;
;; But notice that it inadvertently returns false.  returning false means something!!
;; v0.11 of ReactJS will invoke  both stopPropagation() and preventDefault()
;; on the  event. Almost certainly not what we want.
;;
;; Note: v0.12 of ReactJS will do the same as v0.11, except it also issues a
;; deprecation warning about false returns.
;;
;; Note: ReactJS only tests explicitly for false, not falsy values. So 'nil' is a
;; safe return value.
;;
;; So 'handler-fn' is a macro which will stop you from inadvertently returning
;; false in a handler.
;;
;;
;; Examples:
;;
;;     :on-mouse-out  (handler-fn (reset! my-over-atom false))    ;; notice no # in front reset! form
;;
;;
;;     :on-mouse-out  (handler-fn
;;                       (reset! over-atom false)     ;; notice: no need for a 'do'
;;                       (now do something else)
;;                       (.preventDefault event))     ;; notice access to the 'event'

#?(:clj
   (defmacro e->
     "Event handler macro. Wraps clojure.core/-> into an fn that takes e and
     threads it through exprs. Similar to comp, but doesn't require forms to be
     functions."
     [& body]
     `(fn event-handler# [e#]
        (-> e#
            ~@body)
        nil)))                                              ;; force return nil

#?(:clj
   (defmacro e-handler
     ([& body]
      `(fn e-handler# [~'e] ~@body nil))))                  ;; force return nil
