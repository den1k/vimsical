(ns vimsical.frontend.live-preview.handlers
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.util.dom :as util.dom]))

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

(defmulti update-node!
  (fn [_ content-type _] content-type)
  :default :css-or-javascript)

(defn- document-ready? [doc]
  (some-> doc .-readyState (identical? "complete")))

(defmethod update-node! :html
  [{:keys [doc body]} _ text]
  (when (document-ready? doc)

    (util.dom/set-inner-html! body text)))

(defmethod update-node! :css-or-javascript
  [{:keys [doc head]} content-type text]
  (when (document-ready? doc)
    (let [old-node (.getElementById doc (prefix-ns content-type))
          new-node (util.dom/create content-type {:id (prefix-ns content-type)} text)]
      (some-> old-node util.dom/remove!)
      (util.dom/append! head new-node))))

(defn iframe-info [iframe]
  {:pre  [iframe]
   :post [(not-any? nil? (vals %))]}
  (let [doc  (.-contentDocument iframe)
        head (.-head doc)
        body (.-body doc)]
    {:iframe iframe
     :doc    doc
     :head   head
     :body   body}))

(re-frame/reg-event-fx
 ::register-iframe
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ ui-reg-key iframe]]
   {:ui-db (assoc ui-db ui-reg-key (iframe-info iframe))}))

(re-frame/reg-event-fx
 ::dispose-iframe
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ ui-reg-key]]
   {:ui-db (dissoc ui-db ui-reg-key)}))

(re-frame/reg-event-fx
 ::update-node
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ ui-reg-key file-type text]]
   (let [iframe-info (get ui-db ui-reg-key)]
     (update-node! iframe-info file-type text)
     nil)))                             ; non-nil value will overwrite the cofx map
