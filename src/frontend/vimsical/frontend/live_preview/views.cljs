(ns vimsical.frontend.live-preview.views
  (:require
   [vimsical.common.util.util :as util]
   [reagent.core :as reagent]
   [vimsical.frontend.util.dom :as util.dom :refer-macros [e-> e-handler]]
   [clojure.string :as string])
  (:refer-clojure :exclude [create-node]))

(def prefix-ns
  (let [ns-str (namespace ::_)]
    (fn prefix
      ([kw]
       (str ns-str "/" (name kw)))
      ([kw1 kw2]
       (str ns-str "/" (name kw1) "." (name kw2)))
      ([kw1 kw2 & kws]
       (str ns-str "/" (name kw1) "." (name kw2) "."
            (transduce (comp (map name) (interpose ".")) str kws))))))

(def iframe-sandbox-opts
  "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/iframe"
  (util/space-join "allow-forms"
                   "allow-modals"
                   "allow-pointer-lock"
                   "allow-popups"
                   "allow-same-origin"
                   "allow-scripts"))

(defmulti update-node!
  (fn [content-type _ _] content-type)
  :default :css-or-javascript)

(defn- document-ready? [iframe]
  (some-> iframe .-contentDocument .-readyState (identical? "complete")))

(defmethod update-node! :html
  [_ iframe text]
  (when (document-ready? iframe)
    (-> iframe .-contentDocument .-body (util.dom/set-inner-html! text))))

(defmethod update-node! :css-or-javascript
  [content-type iframe text]
  (when (document-ready? iframe)
    (let [doc      (-> iframe .-contentDocument)
          old-node (.getElementById doc (prefix-ns content-type))
          new-node (util.dom/create content-type {:id (prefix-ns content-type)} text)]
      (some-> old-node util.dom/remove!)
      (util.dom/append! (.-head doc) new-node))))

;;
;; * Components
;;

(defn preview-node [{:keys [iframe-node file-type]}]
  (let [; todo subs to html/css/js string based on file-type
        text (get {:html "<h1>Live-Preview</h1>"
                   :css  "body {
                   background: bisque;
                   color: white;
                   text-align: center;
                   margin-top: 50%;
                   font-size: 50px;
                   -webkit-text-stroke: 1px grey;
                   };"
                   ;:javascript "console.log('hello')"
                   }
                  file-type)]
    (fn [c]
      (when text
        (update-node! file-type iframe-node text))
      [:div])))

(defn live-preview []
  (let [iframe-node (atom nil)          ;; regular atom. only set once
        ;; todo subs to files
        filetypes   #{:html :css :javascript}]
    (reagent/create-class
     {; component should maybe never update? TBD
      ;:should-component-update (fn [_ _ _] false)
      :render
      (fn [c]
        [:div.live-preview
         (when-let [iframe-node @iframe-node]
           (doall
            (for [ft filetypes]
              ^{:key (prefix-ns :preview-node ft)}
              [preview-node {:iframe-node iframe-node
                             :file-type   ft}])))
         [:iframe.iframe
          {:key     (prefix-ns "iframe")
           :ref     (fn [node]
                      (when (and node (nil? @iframe-node))
                        (reset! iframe-node node)
                        (.forceUpdate c)))
           :sandbox iframe-sandbox-opts}]])})))
