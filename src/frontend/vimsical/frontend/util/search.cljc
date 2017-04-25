(ns vimsical.frontend.util.search
  (:require [vimsical.common.util.core :as util]))

;; -------------------------------------------------------------------
;; clj-fuzzy Levenshtein
;; -------------------------------------------------------------------
;;
;;
;;   Author: PLIQUE Guillaume (Yomguithereal)
;;   Version: 0.1
;;   Source: https://gist.github.com/vishnuvyas/958488
;;

(defn- next-row
  [previous current other-seq]
  (reduce
   (fn [row [diagonal above other]]
     (let [update-val (if (= other current)
                        diagonal
                        (inc (min diagonal above (peek row))))]
       (conj row update-val)))
   [(inc (first previous))]
   (map vector previous (next previous) other-seq)))

(defn levenstein-distance
  "Compute the levenshtein distance between two [sequences]."
  [sequence1 sequence2]
  (peek
   (reduce (fn [previous current] (next-row previous current sequence2))
           (map #(identity %2) (cons nil sequence2) (range))
           sequence1)))

(defn score
  "Returns the best (lowest) score for a query and a sequence of search terms."
  [query search-terms]
  (->> (mapv (partial levenstein-distance query) search-terms)
       (apply min)))

(defn search
  "Searches through terms-fn-map and returns best matches in sorted order.
  terms-fns-map should have this format:
  {[<search terms>] {:title \"Command Title\"
                     :fn <callback function (thunk)>}
   ... }
  "
  [query terms-fns-map]
  (when (seq query)
    (->> (util/map-keys (partial score query) terms-fns-map)
         sort
         vals
         vec)))




#_(def t {["search test"]    {:fn    #(prn "1")
                              :title "Run Search Test"}
          ["something else"] {:fn #(prn "2") :title "Do Something Else"}})

