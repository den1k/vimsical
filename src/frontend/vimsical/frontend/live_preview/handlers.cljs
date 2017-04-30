(ns vimsical.frontend.live-preview.handlers
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.util.dom :as util.dom]
            [com.stuartsierra.mapgraph :as mg]
            [vimsical.frontend.vcs.subs :as vcs.subs]
            [vimsical.vcs.branch :as branch]
            [vimsical.vcs.file :as file]
            [vimsical.vcs.lib :as lib]))

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
  [{::keys [loaded-libs] :as iframe-info} libs]
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
 (fn [{:keys [db ui-db] :as cofx} [_ ui-reg-key {:as branch ::branch/keys [libs files]}]]
   (let [iframe-info (get ui-db ui-reg-key)]
     {:dispatch (if (not-empty libs)
                  [::load-libs ui-reg-key branch]
                  [::update-preview-nodes])})))

(re-frame/reg-event-fx
 ::update-preview-node
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db] :as cofx}
      [_ ui-reg-key {::branch/keys [libs]} file ?string]]
   (let [iframe-info (get ui-db ui-reg-key)]
     (when (all-libs-loaded? iframe-info libs)
       (let [string (or ?string (-> db
                                    (vcs.subs/vims-vcs)
                                    (vcs.subs/file-string file)))]
         (update-node! iframe-info file string))))
   nil))

(re-frame/reg-event-fx
 ::update-preview-nodes
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db] :as cofx}
      [_ ui-reg-key {::branch/keys [libs files]}]]
   (let [iframe-info (get ui-db ui-reg-key)]
     (when (all-libs-loaded? iframe-info libs)
       (doseq [file files]
         (let [string (-> db
                          (vcs.subs/vims-vcs)
                          (vcs.subs/file-string file))]
           (update-node! iframe-info file string)))))))

(re-frame/reg-event-fx
 ::load-libs
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db]} [_ ui-reg-key {:as branch ::branch/keys [libs]}]]
   (let [iframe-info (get ui-db ui-reg-key)]
     (doseq [lib libs]
       (swap-head-node!
        iframe-info
        (::lib/sub-type lib)
        {:id     (:db/id lib)
         :src    (::lib/src lib)
         :onload #(re-frame/dispatch [::lib-loaded ui-reg-key branch lib])})))))

(re-frame/reg-event-fx
 ::lib-loaded
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db]} [_ ui-reg-key branch lib]]
   (let [iframe-info (get ui-db ui-reg-key)]
     {:ui-db    (update-in ui-db [ui-reg-key ::loaded-libs] conj (:db/id lib))
      :dispatch [::update-preview-nodes ui-reg-key branch]})))