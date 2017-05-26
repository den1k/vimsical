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

(re-frame/reg-event-fx
 ::register-iframe
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ iframe vims]]
   {:ui-db (ui-db/set-iframe ui-db vims iframe)}))

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
     (if vims
       [::subs/vims-preprocessed-preview-markup vims]
       [::subs/branch-preprocessed-preview-markup])))
  (util.re-frame/inject-sub [::vcs.subs/branch])]
 (fn [{:keys       [db ui-db]
       ::subs/keys [vims-preprocessed-preview-markup branch-preprocessed-preview-markup]
       cur-branch  ::vcs.subs/branch}
      [_ vims]]
   #?(:cljs
      (let [markup        (or vims-preprocessed-preview-markup branch-preprocessed-preview-markup)
            iframe        (ui-db/get-iframe ui-db vims)
            prev-blob-url (ui-db/get-src-blob-url ui-db vims)
            blob-url      (util.dom/blob-url markup "text/html")]
        (some-> prev-blob-url util.dom/revoke-blob-url)
        (aset iframe "src" blob-url)
        {:ui-db (ui-db/set-src-blob-url ui-db vims blob-url)}))))

(re-frame/reg-event-fx
 ::dispose-iframe
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ vims]]
   {:ui-db (ui-db/remove-iframe ui-db vims)}))

(re-frame/reg-event-fx
 ::update-live-preview
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub
   (fn [[_ _ file]] [::vcs.subs/file-lint-or-preprocessing-errors file]))]
 (fn [{:keys       [db ui-db]
       ::subs/keys [file-lint-or-preprocessing-errors]
       :as         cofx}
      [_ vims {::file/keys [sub-type] :as file} file-string]]
   (if (file/javascript? file)
     (when (nil? file-lint-or-preprocessing-errors)
       {:debounce {:ms       500
                   :dispatch [::update-iframe-src vims]}})
     {:dispatch [::update-preview-node vims file file-string]})))

(re-frame/reg-event-fx
 ::update-preview-node
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db] :as cofx}
      [_ vims {::file/keys [sub-type] :as file} string]]
   (let [iframe (ui-db/get-iframe ui-db vims)]
     (do (update-node! iframe file string) nil))))

(re-frame/reg-event-fx
 ::move-script-nodes
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/vims-branch vims]))]
 (fn [{:keys                  [db ui-db]
       {files ::branch/files} ::vcs.subs/vims-branch}
      [_ vims]]
   (let [iframe       (ui-db/get-iframe ui-db vims)
         doc          (.-contentDocument iframe)
         head         (.-head doc)
         js-files     (filter file/javascript? files)
         script-nodes (mapv (fn [{:keys [db/uid]}]
                              (.getElementById doc uid)) js-files)]
     (doseq [node script-nodes]
       (.appendChild head node)))))

(re-frame/reg-event-fx
 ::track-vims
 (fn [_ [_ vims]]
   {:pre [vims]}
   {:track
    {:action       :register
     :id           [::vims vims]
     :subscription [::vcs.subs/vims-branch vims]
     :val->event   (fn [branch]
                     {:pre [branch]}
                     [::track-branch vims branch])}}))

(re-frame/reg-event-fx
 ::track-branch
 (fn [_ [_ vims {:as branch files ::branch/files}]]
   {:pre [branch files]}
   {:track
    (for [file files]
      {:action       :register
       :id           [::file file]
       :subscription [::vcs.subs/file-string file]
       :val->event   (fn [string]
                       ;; ?? this is called on dispose with string being nil
                       (when string
                         [::update-live-preview vims file string]))})}))


(re-frame/reg-event-fx
 ::stop-track-vims
 (fn [_ [_ vims]]
   {:pre [vims]}
   {:track    {:action :dispose :id [::vims vims]}
    :dispatch [::stop-track-branch vims]}))

(re-frame/reg-event-fx
 ::stop-track-branch
 [(util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/vims-branch vims]))]
 (fn [{branch ::vcs.subs/vims-branch} [_ vims]]
   {:pre [branch]}
   {:track
    (for [file (::branch/files branch)]
      {:action :dispose :id [::file file]})}))
