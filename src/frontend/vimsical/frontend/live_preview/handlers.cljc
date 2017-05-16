(ns vimsical.frontend.live-preview.handlers
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.util.re-frame :as util.re-frame :refer [<sub]]
            [com.stuartsierra.mapgraph :as mg]
            [vimsical.frontend.vcs.subs :as vcs.subs]
            [vimsical.vcs.branch :as branch]
            [vimsical.vcs.file :as file]
            [vimsical.frontend.live-preview.ui-db :as ui-db]
            [vimsical.vcs.lib :as lib]
            [vimsical.frontend.live-preview.subs :as subs]
            [vimsical.common.util.core :as util]
            [vimsical.frontend.util.preprocess.core :as preprocess]
   #?@(:cljs [[reagent.dom.server]
              [vimsical.frontend.util.dom :as util.dom]])))

(defn iframe-ready-state [iframe]
  (.. iframe -contentDocument -readyState))

(defn iframe-loading? [iframe]
  (= "loading" (iframe-ready-state iframe)))

(defn iframe-ready? [iframe]
  (= "complete" (iframe-ready-state iframe)))

(defmulti update-node!
  (fn [_ {::file/keys [sub-type]} _] sub-type))

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
  [iframe file string]
  #?(:cljs
     (when (iframe-ready? iframe)
       (let [body (.. iframe -contentDocument -body)]
         (util.dom/set-inner-html! body string)))))

(defmethod update-node! :css
  [iframe {::file/keys [sub-type] :keys [db/id] :as file} string]
  (when (iframe-ready? iframe)
    (let [attrs {:id id}]
      (swap-head-node! iframe sub-type attrs string))))

(re-frame/reg-event-fx
 ::register-and-init-iframe
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ iframe branch]]
   {:ui-db    (ui-db/set-iframe ui-db iframe)
    :dispatch [::update-iframe-src branch]}))

(re-frame/reg-event-fx
 ::update-iframe-src
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub
   (fn [_]
     [::subs/branch-preprocessed-preview-markup]))]
 (fn [{:keys [db ui-db] ::subs/keys [branch-preprocessed-preview-markup]}
      [_ {::branch/keys [files libs]}]]
   #?(:cljs
      (let [iframe        (ui-db/get-iframe ui-db)
            prev-blob-url (ui-db/get-src-blob-url ui-db)
            blob-url      (util.dom/blob-url branch-preprocessed-preview-markup "text/html")]
        (some-> prev-blob-url util.dom/revoke-blob-url)
        (aset iframe "src" blob-url)
        {:ui-db (ui-db/set-src-blob-url ui-db blob-url)}))))

(re-frame/reg-event-fx
 ::dispose-iframe
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_]]
   {:ui-db (ui-db/remove-iframe ui-db)}))

(re-frame/reg-event-fx
 ::update-live-preview
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub
   (fn [[_ _ file]] [::vcs.subs/file-lint-or-preprocessing-errors file]))]
 (fn [{:keys           [db ui-db]
       ::vcs.subs/keys [file-lint-or-preprocessing-errors]
       :as             cofx}
      [_ branch {::file/keys [sub-type] :as file} file-string]]
   (if (= :javascript sub-type)
     (when (nil? file-lint-or-preprocessing-errors)
       {:debounce {:ms       500
                   :dispatch [::update-iframe-src branch]}})
     {:dispatch [::update-preview-node branch file file-string]})))

(re-frame/reg-event-fx
 ::update-preview-node
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db] :as cofx}
      [_ branch {::file/keys [sub-type] :as file} string]]
   (let [iframe (ui-db/get-iframe ui-db)]
     (do (update-node! iframe file string) nil))))

(re-frame/reg-event-fx
 ::move-script-nodes
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db]} [_ {::branch/keys [files]}]]
   (let [iframe       (ui-db/get-iframe ui-db)
         doc          (.-contentDocument iframe)
         head         (.-head doc)
         js-files     (filter (fn [file] (= :javascript (::file/sub-type file))) files)
         script-nodes (mapv (fn [{:keys [db/id]}]
                              (.getElementById doc id)) js-files)]
     (doseq [node script-nodes]
       (.appendChild head node)))))

(re-frame/reg-event-fx
 ::track-start
 (fn [_ [_ branch file]]
   {:track
    {:action       :register
     :id           [:iframe file]
     :subscription [::vcs.subs/file-string file]
     :val->event   (fn [string] [::update-live-preview branch file string])}}))

(re-frame/reg-event-fx
 ::track-stop
 (fn [_ [_ branch file]]
   {:track
    {:action :dispose :id [:iframe file]}}))