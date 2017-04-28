(ns vimsical.frontend.live-preview.handlers
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.util.dom :as util.dom]
            [com.stuartsierra.mapgraph :as mg]
            [vimsical.frontend.vcs.subs :as vcs.subs]
            [vimsical.vcs.file :as file]))

(defmulti update-node!
  (fn [_ file _] (::file/sub-type file))
  :default :css-or-javascript)

(defn swap-head-node!
  ([iframe-info content-type attrs]
   (swap-head-node! iframe-info content-type attrs ""))
  ([{::keys [doc head]} content-type {:keys [id] :as attrs} value]
   {:pre [id]}
   (let [old-node (.getElementById doc id)
         new-node (util.dom/create content-type attrs value)]
     (some-> old-node util.dom/remove!)
     (util.dom/append! head new-node))))

(defmethod update-node! :html
  [{::keys [doc body]} _ string]
  (util.dom/set-inner-html! body string))

(defmethod update-node! :css-or-javascript
  [iframe-info file string]
  (swap-head-node! iframe-info
                   (::file/sub-type file)
                   {:id (:db/id file)}
                   string))

;; todo spec this
(defn iframe-info [iframe]
  {:pre  [iframe]
   :post [(not-any? nil? (vals %))]}
  (let [window (.-contentWindow iframe)
        doc    (.-contentDocument iframe)
        head   (.-head doc)
        body   (.-body doc)]
    {::iframe      iframe
     ::window      window
     ::doc         doc
     ::head        head
     ::body        body
     ::loaded-libs #{}}))

(defn all-libs-loaded?
  [{::keys [loaded-libs] :as iframe-info} {::file/keys [libs]}]
  (every? (comp loaded-libs :db/id) libs))

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
 ::init
 (fn [{:keys [db ui-db] :as cofx} [_ ui-reg-key files]]
   (let [iframe-info (get ui-db ui-reg-key)
         dispatches  (for [{::file/keys [libs] :as file} files]
                       (if (not-empty libs)
                         [::load-libs ui-reg-key file libs]
                         (let [string (-> db
                                          (vcs.subs/vims-vcs)
                                          (vcs.subs/file-string file))]
                           [::update-preview-node ui-reg-key file string])))]
     {:dispatch-n dispatches})))

(re-frame/reg-event-fx
 ::update-preview-node
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db] :as cofx}
      [_ ui-reg-key {::file/keys [sub-type libs] :as file} string]]
   (let [iframe-info (get ui-db ui-reg-key)]
     (when (all-libs-loaded? iframe-info file)
       (let [string (or string (-> db
                                   (vcs.subs/vims-vcs)
                                   (vcs.subs/file-string file)))]
         (update-node! iframe-info file string)))
     nil)))

(re-frame/reg-event-fx
 ::load-libs
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db]} [_ ui-reg-key file libs]]
   (let [iframe-info (get ui-db ui-reg-key)]
     (doseq [lib libs]
       (swap-head-node!
        iframe-info
        (::file/sub-type file)
        {:id     (:db/id lib)
         :src    (:lib/src lib)
         :onload #(re-frame/dispatch [::lib-loaded lib ui-reg-key file])})))))

(re-frame/reg-event-fx
 ::lib-loaded
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db]} [_ lib ui-reg-key file]]
   (let [iframe-info (get ui-db ui-reg-key)]
     {:ui-db    (update-in ui-db [ui-reg-key ::loaded-libs] conj (:db/id lib))
      :dispatch [::update-preview-node ui-reg-key file]})))