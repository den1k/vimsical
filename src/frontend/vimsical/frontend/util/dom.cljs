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
       (get type (name type))
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
;; * Event Transformers and Handlers
;;

(defn e->key [e]
  (util/lisp-case-keyword (.-key e)))

(defn handle-key
  "Takes a dom-event and a hash-map of key-name -> thunk.
  If a match is found, calls thunk and prevents event."
  [e key->fn]
  (when-let [f (-> e e->key key->fn)]
    (.preventDefault e)
    (f)))

(defn e->mouse-coords [e] [(.-clientX e) (.-clientY e)])
(defn e->mouse-deltas [e] [(.-deltaX e) (.-deltaY e)])

;;
;; * Dimensions
;;

(defn bounding-client-rect
  "Returns a map of the bounding client rect of `elem`
   as a map with [:top :left :right :bottom :width :height]"
  [elem]
  (let [r      (.getBoundingClientRect elem)
        height (.-height r)]
    {:top    (.-top r)
     :bottom (.-bottom r)
     :left   (.-left r)
     :right  (.-right r)
     :width  (.-width r)
     :height height
     :middle (/ height 2)}))

(def component-rect
  (comp bounding-client-rect
        reagent/dom-node))

(defn body-rect []
  (bounding-client-rect (.-body js/document)))

(defn e->rel-mouse-coords
  ([e] (e->rel-mouse-coords e (.-target e)))
  ([e rel-to-elem]
   (let [rect  (bounding-client-rect rel-to-elem)
         [x y] (e->mouse-coords e)
         rel-x (- x (:left rect))
         rel-y (- y (:top rect))]
     [rel-x rel-y])))

(defn e->rel-mouse-coords-percs
  ([e] (e->rel-mouse-coords e (.-target e)))
  ([e rel-to-elem]
   (let [{:keys [left top width height]} (bounding-client-rect rel-to-elem)
         [x y] (e->mouse-coords e)
         rel-x  (- x left)
         rel-y  (- y top)
         x-perc (-> rel-x (/ width) (* 100) (util/clamp 0 100))
         y-perc (-> rel-y (/ height) (* 100) (util/clamp 0 100))]
     [x-perc y-perc])))

(defn rel-component-mouse-coords [c e]
  (e->rel-mouse-coords e (reagent/dom-node c)))

(defn rel-component-mouse-coords-percs [c e]
  (e->rel-mouse-coords-percs e (reagent/dom-node c)))

;;
;; * Blobs
;;

(defn blob [data type]
  (-> data
      array
      (js/Blob. #js {:type type})))

(defn blob-url
  ([blob]
   (.createObjectURL js/URL blob))
  ([data type]
   (blob-url (blob data type))))

(defn revoke-blob-url [url]
  (.revokeObjectURL js/URL url))

;;
;; * Mobile
;;

(defn orientation []
  (let [{:keys [width height]} (body-rect)]
    (if (> width height) :landscape :portrait)))

(defn on-mobile? []
  (let [user-agent (str/lower-case (.. js/window -navigator -userAgent))
        rxp        #"android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini"]
    (boolean (re-find rxp user-agent))))