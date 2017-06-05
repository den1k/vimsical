(ns vimsical.backend.util.async
  (:require [clojure.core.async :as a]))

;;
;; * Chan
;;

(defn new-error-chan
  [n]
  (a/chan n (map identity) identity))

;;
;; * Blocks
;;

(defmacro go-try [& body]
  `(a/go
     (try
       (do ~@body)
       (catch Throwable t# t#))))

(defmacro thread-try [& body]
  `(a/thread
     (try
       (do ~@body)
       (catch Throwable t# t#))))

;;
;; * Parks
;;

(defn- throwable?
  [x]
  (and x (instance? Throwable x)))

(defn rethrow [x]
  (cond-> x
    (throwable? x) (throw)))

(defmacro <? [chan]
  `(rethrow (a/<! ~chan)))

(defn <?? [chan]
  (rethrow (a/<!! chan)))

(defn alts?
  ([ports] (alts? ports true))
  ([ports close-all-on-error?]
   (letfn [(cleanup! []
             (doseq [p ports] (a/close! p)))]
     (when-some [[x _] (a/alts! ports)]
       (if (throwable? x)
         (do
           (when close-all-on-error?
             (cleanup!))
           (throw x))
         x)))))

(defn alts??
  ([ports] (alts?? ports true))
  ([ports close-all-on-error?]
   (letfn [(cleanup! []
             (doseq [p ports] (a/close! p)))]
     (when-some [[x _] (a/alts!! ports)]
       (if (throwable? x)
         (do
           (when close-all-on-error?
             (cleanup!))
           (throw x))
         x)))))

(defn- val->idx [coll]
  (into {} (map-indexed #(vector %2 %1) coll)))

(defn parallel-promises
  "Take the first value of every port in `ports` and return a chan that will put
  a sequence of the values taken from each port, in the order that their ports
  appeared in `ports`."
  [ports]
  (let [port->idx (val->idx ports)]
    (a/go-loop
        [ports  (set ports)
         values (sorted-map)]
      (if-not (seq ports)
        (vals values)
        (let [[value port] (a/alts! (vec ports))]
          (recur
           (disj ports port)
           (assoc values (get port->idx port) value)))))))

(defn parallel-promises?
  "Same as `parallel-promises` but if any of the ports put an exception, stop
  the recursion, return that exception and close all ports if `close-on-error?`
  is true"
  ([ports] (parallel-promises? ports true))
  ([ports close-on-error?]
   (let [port->idx (val->idx ports)]
     (a/go-loop
         [ports  (set ports)
          values (sorted-map)]
       (if-not (seq ports)
         (vals values)
         (let [[value port] (a/alts! (vec ports))]
           (if (throwable? value)
             (do (when close-on-error? (doseq [p (seq ports)] (a/close! p))) value)
             (recur
              (disj ports port)
              (assoc values (get port->idx port) value)))))))))
