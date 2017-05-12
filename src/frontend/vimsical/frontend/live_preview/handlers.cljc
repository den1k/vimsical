(ns vimsical.frontend.live-preview.handlers
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.util.re-frame :as util.reframe :refer [<sub]]
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

(def preview-iframe-path
  [::iframes :preview])

(def error-catcher-iframe-path
  [::iframes :error-catcher])

(defn- preview-iframe [ui-db]
  (get-in ui-db preview-iframe-path))

(defn- error-catcher-iframe [ui-db]
  (get-in ui-db error-catcher-iframe-path))

(defn update-iframe-src
  [{:keys [db ui-db]} [_ {::branch/keys [files libs]} {:keys [error-catcher?]}]]
  #?(:cljs
     (let [iframe        (ui-db/get-iframe ui-db)
           prev-blob-url (ui-db/get-src-blob-url ui-db)
           markup        (iframe-markup-string {:files files :libs libs})
           blob-url      (util.dom/blob-url markup "text/html")]
       (some-> prev-blob-url util.dom/revoke-blob-url)
       (aset iframe "src" blob-url)
       {:ui-db (ui-db/set-src-blob-url ui-db blob-url)})))

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
  [iframe file]
  #?(:cljs
     (when (iframe-ready? iframe)
       (let [body (.. iframe -contentDocument -body)
             html (<sub [::vcs.subs/preprocessed-file-string file])]
         (util.dom/set-inner-html! body html)))))

(defmethod update-node! :css
  [iframe {::file/keys [sub-type] :keys [db/id] :as file}]
  (when (iframe-ready? iframe)
    (let [attrs  {:id id}
          string (<sub [::vcs.subs/preprocessed-file-string file])]
      (swap-head-node! iframe sub-type attrs string))))

#_(:cljs (js/console.debug
          (let [js-markup (reagent.dom.server/render-to-static-markup
                           [:script
                            {:id                      :jsjsjsjs
                             :dangerouslySetInnerHTML {:__html "throw new Error('Whoops!');"}}])
                markup    (reagent.dom.server/render-to-static-markup
                           [:html
                            [:head]
                            [:body
                             {:id                      :BSDJSDN
                              :dangerouslySetInnerHTML {:__html
                                                        js-markup}}]])
                id        "error-catcher"
                src       (util.dom/blob-url markup "text/html")

                iframe    (or
                           (.getElementById js/document id)
                           (-> (util.dom/create :iframe
                                                {:id     id
                                                 :onload #(js/console.debug "I LOADED")
                                                 :style  "display: none"})
                               (js/document.body.appendChild)))
                script    (util.dom/create :javascript {} "throw new Error('Whoops!');")]
            (js/console.debug :START)
            (aset iframe "contentWindow" "onerror" #(js/console.log "NNONONON"))
            (aset iframe "src" src)
            ;(.. iframe -contentDocument -body (appendChild script))
            (iframe.contentWindow.location.reload)

            )
          ))

(re-frame/reg-event-fx
 ::register-and-init-iframe
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ iframe branch]]
   {:ui-db    (ui-db/set-iframe ui-db iframe)
    :dispatch [::update-iframe-src branch]}))

(re-frame/reg-event-fx
 ::update-iframe-src
 [(re-frame/inject-cofx :ui-db)]
 update-iframe-src)

(re-frame/reg-event-fx
 ::dispose-iframe
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_]]
   {:ui-db (ui-db/remove-iframe ui-db)}))

(re-frame/reg-event-fx
 ::update-live-preview
 [(re-frame/inject-cofx :ui-db)
  (util.reframe/inject-sub
   (fn [[_ branch {::file/keys [sub-type] :as file}]]
     [::vcs.subs/file-lint-or-preprocessing-errors file]))]
 (fn [{:as             cofx
       ::vcs.subs/keys [file-lint-or-preprocessing-errors]
       :keys           [db ui-db]}
      [_ branch {::file/keys [sub-type] :as file}]]
   (let [iframe (ui-db/get-iframe ui-db)]
     (if (= :javascript sub-type)
       (when (nil? file-lint-or-preprocessing-errors)
         {:dispatch [::update-iframe-src branch]})
       {:dispatch [::update-preview-node branch file]}))))

(re-frame/reg-event-fx
 ::update-live-preview
 [(re-frame/inject-cofx :ui-db)
  (util.reframe/inject-sub
   (fn [[_ _ file]] [::vcs.subs/file-lint-or-preprocessing-errors file]))]
 (fn [{:keys           [db ui-db]
       ::vcs.subs/keys [file-lint-or-preprocessing-errors]
       :as             cofx}
      [_ branch {::file/keys [sub-type] :as file}]]
   (let [iframe (preview-iframe ui-db)]
     (if (= :javascript sub-type)
       (when (nil? file-lint-or-preprocessing-errors)
         {:dispatch [::update-iframe-src branch]})
       {:dispatch [::update-preview-node branch file]}))))

(re-frame/reg-event-fx
 ::update-preview-node
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db] :as cofx}
      [_ branch {::file/keys [sub-type] :as file}]]
   (let [iframe (ui-db/get-iframe ui-db)]
     (do (update-node! iframe file) nil))))

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
     :event        [::update-live-preview branch file]}}))

(re-frame/reg-event-fx
 ::track-stop
 (fn [_ [_ branch file]]
   {:track
    {:action :dispose :id [:iframe file]}}))