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

(defmacro alts?
  ([ports] (alts? ports true))
  ([ports close-all-on-error?]
   `(letfn [(cleanup! []
              (doseq [p# ~ports] (a/close! p#)))]
      (when-some [[x#] (a/alts! ~ports)]
        (if (throwable? x#)
          (do
            (when ~close-all-on-error?
              (cleanup!))
            (throw x#))
          x#)))))

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
