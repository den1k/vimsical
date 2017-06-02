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
            [vimsical.common.util.core :as util :include-macros true]
            [vimsical.frontend.util.preprocess.core :as preprocess]
            #?@(:cljs [[reagent.dom.server]
                       [vimsical.frontend.util.dom :as util.dom]])))

;;
;; * Iframe dom helpers
;;

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
  [iframe {::file/keys [sub-type] :keys [db/uid] :as file} string]
  (when (iframe-ready? iframe)
    (let [attrs {:id uid}]
      (swap-head-node! iframe sub-type attrs string))))

(defn update-iframe-markup!
  [ui-db key-entity markup]
  #?(:cljs
     (let [iframe        (ui-db/get-iframe ui-db key-entity)
           prev-blob-url (ui-db/get-src-blob-url ui-db key-entity)
           blob-url      (util.dom/blob-url markup "text/html")]
       (some-> prev-blob-url util.dom/revoke-blob-url)
       (aset iframe "src" blob-url)
       (ui-db/set-src-blob-url ui-db key-entity blob-url))))

;;
;; * IFrame registration
;;

(re-frame/reg-event-fx
 ::register-iframe
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ vims iframe]]
   {:ui-db (ui-db/set-iframe ui-db vims iframe)}))

(re-frame/reg-event-fx
 ::dispose-iframe
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ vims]]
   {:ui-db (ui-db/remove-iframe ui-db vims)}))

;;
;; * IFrame updates
;;

;; TODO
;; this could be grately simplified
;; update-iframe-src should always take either the branch or the markup directly
;; error-catcher complicates this by dispatching here without providing a branch
;; we could have something like code-analysis/results that track linters and
;; error-catcher's results and then dispatch here with a branch on success
(re-frame/reg-event-fx
 ::update-iframe-src
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub
   (fn [[_ vims]]
     {:pre [vims]}
     [::subs/vims-preprocessed-preview-markup vims]))]
 (fn [{:keys       [db ui-db]
       ::subs/keys [vims-preprocessed-preview-markup]}
      [_ vims]]
   {:ui-db (update-iframe-markup! ui-db vims vims-preprocessed-preview-markup)}))

(re-frame/reg-event-fx
 ::update-iframe-snapshots
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/snapshots vims]))
  (util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/libs vims]))]
 (fn [{:keys           [db ui-db]
       ::vcs.subs/keys [snapshots libs]} [_ vims]]
   (let [preview-markup (subs/snapshots-markup snapshots libs)]
     {:ui-db (update-iframe-markup! ui-db vims preview-markup)})))

;;
;; * Updates coordination
;;

(re-frame/reg-event-fx
 ::update-live-preview
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db]}
      [_ vims {::file/keys [sub-type] :as file} file-string file-lint-or-preprocessing-errors]]
   (if (file/javascript? file)
     (when (nil? file-lint-or-preprocessing-errors)
       {:debounce {:ms       500
                   :dispatch [::update-iframe-src vims]}})
     {:dispatch [::update-preview-node vims file file-string]})))

(re-frame/reg-event-fx
 ::update-preview-node
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db]}
      [_ vims {::file/keys [sub-type] :as file} string]]
   (when-some [iframe (ui-db/get-iframe ui-db vims)]
     (do (update-node! iframe file string) nil))))

(re-frame/reg-event-fx
 ::move-script-nodes
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/files vims]))]
 (fn [{:keys           [db ui-db]
       ::vcs.subs/keys [files]}
      [_ vims]]
   (when-some [iframe (ui-db/get-iframe ui-db vims)]
     (let [doc          (.-contentDocument iframe)
           head         (.-head doc)
           js-files     (filter file/javascript? files)
           script-nodes (keep (fn [{:keys [db/uid]}] (.getElementById doc uid)) js-files)]
       (doseq [node script-nodes]
         (.appendChild head node))))))

;;
;; * Tracks
;;

(re-frame/reg-event-fx
 ::track-vims
 (fn [_ [_ {:keys [db/uid] :as vims}]]
   {:pre [uid vims]}
   {:track
    {:action       :register
     :id           [::vims uid]
     :subscription [::vcs.subs/files vims]
     :val->event   (fn [files]
                     [::track-files vims files])}}))

(re-frame/reg-event-fx
 ::track-files
 (fn [_ [_ vims files]]
   {:track
    (for [{:keys [db/uid] :as file} files]
      {:action          :register
       :id              [::file uid]
       :subscription    [::subs/file-string+file-lint-or-preprocessing-errors vims file]
       :dispatch-first? false
       :val->event      (fn [[file-string file-lint-or-preprocessing-errors]]
                          [::update-live-preview vims file file-string file-lint-or-preprocessing-errors])})}))

(re-frame/reg-event-fx
 ::stop-track-vims
 [(util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/branch vims]))]
 (fn [{::vcs.subs/keys [branch]} [_ {:keys [db/uid]}]]
   {:track    {:action :dispose :id [::vims uid]}
    :dispatch [::stop-track-branch branch]}))

(re-frame/reg-event-fx
 ::stop-track-branch
 (fn [_ [_ {::branch/keys [files]}]]
   {:track
    (for [{:keys [db/uid]} files]
      {:action :dispose :id [::file uid]})}))
