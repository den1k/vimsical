(ns vimsical.common.util.core
  (:refer-clojure :exclude [max min halt-when])
  #?@(:clj
      [
       (:require
        [clojure.core.async :as a :refer [alt! go-loop]]
        [clojure.string :as string]
        [medley.core :as md])]
      :cljs
      [(:require
        [cljs.core.async :as a]
        [clojure.string :as string]
        [goog.object :as gobj]
        [medley.core :as md]
        [goog.functions :as gfns]
        [goog.string :as gstr])
       (:require-macros [cljs.core.async.macros :refer [alt! go-loop]])]))

(defn- next-item-with-comparator [comparator]
  (let [pos-fn (cond
                 (#{> >=} comparator) first
                 (#{< <=} comparator) last)]
    (fn next-item
      ([sorted-coll x] (next-item sorted-coll x nil))
      ([sorted-coll x default]
       (or (pos-fn (subseq sorted-coll comparator x))
           default)))))

(def next-smaller
  (next-item-with-comparator <))

(def next-smaller-or-equal
  (next-item-with-comparator <=))

(def next-bigger
  (next-item-with-comparator >))

(def next-bigger-or-equal
  (next-item-with-comparator >=))

(defn project
  "Turns a coll into a hashmap with keys of (f item)."
  [f coll]
  (into {} (map #(vector (f %) %)) coll))

(defn into-map [m]
  (fn [keyfn valfn coll]
    (into m (map (juxt keyfn valfn)) coll)))

(def into-hashmap
  "Takes a coll and returns a hashmap using the results of
  keyfn and valfn on each item as keyvals."
  (into-map {}))

(def into-sorted-map
  "Takes a coll and returns a sorted-map using the results of
  keyfn and valfn on each item as keyvals."
  (into-map (sorted-map)))

(def into-array-map
  "Takes a coll and returns a array-map using the results of
  keyfn and valfn on each item as keyvals."
  (into-map (array-map)))

(defn between? [[a b] x]
  (or (< a x b)
      (< b x a)))

(defn min-max [coll]
  [(apply clojure.core/max coll)
   (apply clojure.core/max coll)])

(defn ffilter [pred coll]
  (some #(if (pred %) %) coll))

(defn min
  ([] nil)
  ([coll]
   (when (seq coll)
     (apply clojure.core/min coll))))

(defn max
  ([] nil)
  ([coll]
   (when (seq coll)
     (apply clojure.core/max coll))))

(defn now []
  #?(:clj  (.getTime (java.util.Date.))
     :cljs (.now js/Date)))

(def merge-1
  "One level deep merge."
  (partial merge-with merge))

(defn merge-some
  "Like merge, but ignores keys with `nil` values."
  [& maps]
  (apply merge-with
         (fn [val-former val-letter]
           (if (nil? val-letter)
             val-former
             val-letter))
         maps))

(defn =by
  ([f a b]
   (= (f a) (f b)))
  ([f g a b]
   (= (f a) (g b))))

(defn perf-timestamp []
  #?(:cljs (.. js/window -performance now)))

;; The next 3 fns were snatched from prismatic/plumbing in order to remove the
;; dependency and all he warnings that came with it...

(def dissoc-in md/dissoc-in)
(def map-keys md/map-keys)
(def map-vals md/map-vals)
(def interleave-all md/interleave-all)

(defn index-of
  ([coll x]
   (index-of coll x 0))
  ([coll x start]
   (let [len (count coll)]
     (if (>= start len)
       -1
       (loop [idx (cond
                    (pos? start) start
                    (neg? start) (clojure.core/max 0 (+ start len))
                    :else start)]
         (if (< idx len)
           (if (= (nth coll idx) x)
             idx
             (recur (inc idx)))
           -1))))))

(defn namespace-keys
  "Namespaces keys in a map if they aren't already namespaced."
  [ns map]
  (letfn [(prefix [x] (if (namespace x)
                        x
                        (str (name ns) "/" (name x))))]
    (md/map-keys (comp keyword prefix) map)))

(defn update-when
  "Calls pred with every item of coll and applies f & args to it when true."
  [coll pred f & args]
  (mapv #(if (pred %)
           (apply f % args)
           %)
        coll))

(defn with-delay
  ([f]
   (with-delay f 0))
  ([f ms]
   #?(:cljs
      (if-not (some-> ms int pos-int?)
        (goog.async.nextTick f)
        (.setTimeout js/window f ms))
      :clj
      (.start (Thread. (fn [] (when (pos? ms) (Thread/sleep ms)) (f)))))))

(defmacro compare-cond
  "Takes an old, a new value and clauses and a set of test-f/result-f pairs.
   It compares the result of calling test-f ond old and new. When different,
   passes the result of test-f to result-f"
  [old new & clauses]
  (assert (even? (count clauses)) "Must contain an even number of clauses.")
  (let [clauses (partition 2 clauses)]
    `(do
       ~@(map (fn [[f cb]]
                (when clauses
                  (let [fold (list f old)
                        fnew (list f new)]
                    (list 'when (list 'not= fnew fold)
                          (list cb fnew)))))
              clauses))))

(defmacro space-join
  "Like (apply core.string/join \" \" coll) but runs at compile time using str."
  [& xs]
  `(str ~@(rest (interleave (repeat " ") xs))))

(defmacro f->
  "Wraps clojure.core/-> into an anonymous fn of one arg. Similar to comp, but
   doesn't require forms to be functions."
  [& body]
  `(fn [arg#]
     (-> arg#
         ~@body)))

(defn halt-when
  "Returns a transducer that ends transduction when pred returns true
  for an input. When retf is supplied it must be a fn of 2 arguments -
  it will be passed the (completed) result so far and the input that
  triggered the predicate, and its return value (if it does not throw
  an exception) will be the return value of the transducer. If retf
  is not supplied, the input that triggered the predicate will be
  returned. If the predicate never returns true the transduction is
  unaffected."
  ([pred] (halt-when pred nil))
  ([pred retf]
   (fn [rf]
     (fn
       ([] (rf))
       ([result]
        (if (and (map? result) (contains? result ::halt))
          (::halt result)
          (rf result)))
       ([result input]
        (if (pred input)
          (reduced {::halt (if retf (retf (rf result) input) input)})
          (rf result input)))))))

#?(:cljs
   (defn debounce [f interval]
     (gfns/debounce f interval)))

#?(:cljs
   (defn throttle [f interval]
     (gfns/debounce f interval)))

#?(:cljs
   (defn url-encode [s]
     (js/encodeURIComponent s)))

(defn deep-merge
  "Like merge, but merges maps recursively."
  [& maps]
  (if (every? map? (filter identity maps))
    (apply merge-with deep-merge maps)
    (last maps)))

;;
;; * String & Keyword Manipulation
;;

;; from https://github.com/expez/superstring/blob/master/src/superstring/core.cljs

(defn- split-words [s]
  (remove empty?
          (-> s
              (string/replace #"_|-" " ")
              (string/replace #"([A-Z])(([A-Z])([a-z0-9]))"
                              "$1 $2")
              (string/replace
               #"([a-z])([A-Z])" "$1 $2")
              (string/split
               #"[^\w0-9]+"))))

(defn lisp-case
  "Lower case s and separate words with dashes.
  foo bar => foo-bar
  camelCase => camel-case
  This is also referred to as kebab-case in some circles."
  [^String s]
  {:pre  [(string? s)]
   :post [(string? %)]}
  (string/join "-" (map string/lower-case (split-words s))))

(def lisp-case-keyword
  (comp keyword lisp-case))

#?(:cljs
   (defn norm-str [s]
     (some-> s not-empty gstr/collapseWhitespace)))