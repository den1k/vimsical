(ns vimsical.frontend.live-preview.handlers
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.util.re-frame :as util.reframe :refer [<sub]]
            [com.stuartsierra.mapgraph :as mg]
            [vimsical.frontend.vcs.subs :as vcs.subs]
            [vimsical.vcs.branch :as branch]
            [vimsical.vcs.file :as file]
            [vimsical.frontend.live-preview.ui-db :as ui-db]
            [vimsical.vcs.lib :as lib]
            [vimsical.common.util.core :as util]
            [vimsical.frontend.util.preprocess.core :as preprocess]
            #?@(:cljs [[reagent.dom.server]
                       [vimsical.frontend.util.dom :as util.dom]])))

(defn- lib-node [{:keys [db/id] ::lib/keys [src sub-type] :as lib}]
  (let [tag (case sub-type :html :body :css :style :javascript :script)]
    [tag {:id id :src src}]))

(defn- file-node [{:keys [db/id] ::file/keys [sub-type] :as file}]
  (let [tag    (case sub-type :html :body :css :style :javascript :script)
        string (<sub [::vcs.subs/preprocessed-file-string file])]
    [tag
     {:id                      id
      :dangerouslySetInnerHTML {:__html string}}]))

(defn- file-node-markup [file]
  #?(:cljs
     (reagent.dom.server/render-to-static-markup
      [file-node file])))

(defn- iframe-markup [{:keys [files libs]}]
  (let [by-subtype (group-by ::file/sub-type files)]
    [:html
     [:head
      (doall
       (for [lib libs]
         ^{:key (:db/id lib)} [lib-node lib]))
      (doall
       (for [file (:css by-subtype)]
         ^{:key (:db/id file)} [file-node file]))]
     (let [html-file   (first (:html by-subtype))
           html-string (<sub [::vcs.subs/preprocessed-file-string html-file])
           body-string (transduce
                        (map file-node-markup)
                        str
                        html-string
                        (:javascript by-subtype))]
       [:body
        {:id                      (:db/id html-file)
         :dangerouslySetInnerHTML {:__html
                                   body-string}}])]))

(defn- iframe-markup-string [{:keys [files libs] :as opts}]
  #?(:cljs
     (reagent.dom.server/render-to-static-markup
      (iframe-markup opts))))

(defn update-iframe-src
  [{:keys [db ui-db]} [_ ui-reg-key {::branch/keys [files libs]}]]
  #?(:cljs
     (let [iframe        (ui-db/get-iframe ui-db ui-reg-key)
           prev-blob-url (ui-db/get-src-blob-url ui-db ui-reg-key)
           markup        (iframe-markup-string {:files files :libs libs})
           blob-url      (util.dom/blob-url markup "text/html")]
       (some-> prev-blob-url util.dom/revoke-blob-url)
       (aset iframe "src" blob-url)
       {:ui-db (ui-db/set-src-blob-url ui-db ui-reg-key blob-url)})))

(defmulti update-node!
  (fn [_ {::file/keys [sub-type]} _] sub-type)
  :default :css-or-javascript)

(defn swap-head-node!
  ([iframe content-type attrs]
   (swap-head-node! iframe content-type attrs ""))
  ([iframe content-type {:keys [id] :as attrs} value]
   {:pre [id]}
   #?(:cljs
      (let [doc      (.-contentDocument iframe)
            head     (.-head doc)
            old-node (.getElementById doc id)
            new-node (util.dom/create content-type attrs value)]
        (some-> old-node util.dom/remove!)
        (util.dom/append! head new-node)))))

(defmethod update-node! :html
  [iframe file]
  #?(:cljs
     (when iframe
       (let [body (.. iframe -contentDocument -body)
             html (<sub [::vcs.subs/preprocessed-file-string file])]
         (util.dom/set-inner-html! body html)))))

(defmethod update-node! :css-or-javascript
  [iframe {::file/keys [sub-type] :keys [db/id] :as file}]
  (when iframe
    (let [attrs  {:id id}
          string (<sub [::vcs.subs/preprocessed-file-string file])]
      (when (some? string)
        (swap-head-node! iframe sub-type attrs string)))))

(re-frame/reg-event-fx
 ::register-and-init-iframe
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ ui-reg-key iframe branch]]
   {:ui-db    (ui-db/set-iframe ui-db ui-reg-key iframe)
    :dispatch [::update-iframe-src ui-reg-key branch]}))

(re-frame/reg-event-fx
 ::update-iframe-src
 [(re-frame/inject-cofx :ui-db)]
 update-iframe-src)

(re-frame/reg-event-fx
 ::dispose-iframe
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ ui-reg-key]]
   {:ui-db (ui-db/set-iframe ui-db ui-reg-key nil)}))

(re-frame/reg-event-fx
 ::update-live-preview
 [(re-frame/inject-cofx :ui-db)
  (util.reframe/inject-sub
   (fn [[_ ui-reg-key branch {::file/keys [sub-type] :as file}]]
     [::vcs.subs/file-lint-or-preprocessing-errors file]))]
 (fn [{:as             cofx
       ::vcs.subs/keys [file-lint-or-preprocessing-errors]
       :keys           [db ui-db]}
      [_ ui-reg-key branch {::file/keys [sub-type] :as file}]]
   (when-some [iframe (ui-db/get-iframe ui-db ui-reg-key)]
     (if (= :javascript sub-type)
       (when (nil? file-lint-or-preprocessing-errors)
         {:dispatch [::update-iframe-src ui-reg-key branch]})
       {:dispatch [::update-preview-node ui-reg-key branch file]}))))

(re-frame/reg-event-fx
 ::update-preview-node
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db] :as cofx}
      [_ ui-reg-key branch {::file/keys [sub-type] :as file}]]
   (when-some [iframe (ui-db/get-iframe ui-db ui-reg-key)]
     (do (update-node! iframe file) nil))))

(re-frame/reg-event-fx
 ::move-script-nodes
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db]} [_ ui-reg-key {::branch/keys [files]}]]
   (when-some [iframe (ui-db/get-iframe ui-db ui-reg-key)]
     (let [doc          (.-contentDocument iframe)
           head         (.-head doc)
           js-files     (filter (fn [file] (= :javascript (::file/sub-type file))) files)
           script-nodes (mapv (fn [{:keys [db/id]}]
                                (.getElementById doc id)) js-files)]
       (doseq [node script-nodes]
         (.appendChild head node))))))

(re-frame/reg-event-fx
 ::track-start
 (fn [_ [_ ui-reg-key branch file]]
   {:track
    {:action       :register
     :id           [:iframe file]
     :subscription [::vcs.subs/file-string file]
     :event        [::update-live-preview ui-reg-key branch file]}}))

(re-frame/reg-event-fx
 ::track-stop
 (fn [_ [_ ui-reg-key branch file]]
   {:track
    {:action :dispose :id [:iframe file]}}))
