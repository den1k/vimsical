(ns vimsical.frontend.util.dom
  (:require [goog.dom :as gdom]
            [vimsical.common.util.util :as util]))

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