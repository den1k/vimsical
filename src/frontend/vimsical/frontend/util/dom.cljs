(ns vimsical.frontend.util.dom
  (:require [goog.dom :as gdom]
            [vimsical.common.util.core :as util]
            [reagent.core :as reagent]
            [clojure.string :as str])
  (:refer-clojure :exclude [contains?]))

(defn create
  "Creates a DOM node for either CSS or JavaScript."
  ([type attrs]
   (create type attrs nil))
  ([type attrs inner-html]
   (-> {:css "style" :javascript "script"}
       (get type)
       (gdom/createDom (clj->js attrs) inner-html))))

(defn append!
  "Append child to parent. Returns parent."
  ([parent child]
   (doto parent
     (.appendChild child)))
  ([parent child & more-children]
   (doseq [c (cons child more-children)]
     (append! parent c))
   parent))

(defn parent [elem]
  (.-parentNode elem))

(defn remove!
  "Remove `elem` from `parent`, return `parent`"
  ([elem]
   (let [p (parent elem)]
     (assert p "Target element must have a parent")
     (remove! p elem)))

  ([p elem]
   (doto p (.removeChild elem))))

(defn set-inner-html!
  "Set the innerHTML of elem to html"
  [elem html]
  (set! (.-innerHTML elem) html)
  elem)

(defn contains? [?parent node]
  (.contains ?parent node))

(defn view-contains? [v node]
  (contains? (reagent/dom-node v) node))

(defn view-contains-related-target?
  "Useful for mouse-events to find out if mouse left component."
  [v e]
  (view-contains? v (.-relatedTarget e)))

(defn open-ext-popup [url & opts]
  (.open js/window url
         ;; window name
         (str (gensym))
         (str/join ", " (map util/lisp-case opts))))

;;
;; * UI Actions
;;

(defn unfocus []
  (if-let [sel (.getSelection js/document)]
    (.empty sel)
    (.. js/window getSelection removeAllRanges)))

(defn blur []
  (when-let [el (.-activeElement js/document)]
    (.blur el)))

(defn clear-active-element []
  (some-> (.-activeElement js/document)
          (set-inner-html! "")))

;;
;; * Key Handlers
;;

(defn e->key [e]
  (util/lisp-case-keyword (.-key e)))

(defn handle-key [e key->fn]
  (when-let [f (-> e e->key key->fn)]
    (f)))
