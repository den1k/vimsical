(ns vimsical.frontend.live-preview.subs
  (:require
   [re-frame.core :as re-frame]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.frontend.license.subs :as license.subs]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.lib :as lib]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.snapshot :as snapshot]
   [vimsical.frontend.util.re-frame :refer [<sub]]
   #?(:cljs [reagent.dom.server :as reagent.dom.server])
   [vimsical.common.util.core :as util :include-macros true]
   [re-frame.interop :as interop]))

(defn- file-node [{:keys [db/uid] ::file/keys [sub-type] :as file} string]
  (let [tag (case sub-type :html :body :css :style :javascript :script)]
    [tag
     {:id                      uid
      :dangerouslySetInnerHTML {:__html string}}]))

(defn- snapshot-node [{:keys [db/uid] ::snapshot/keys [sub-type text] :as snapshot}]
  (let [tag (case sub-type :html :body :css :style :javascript :script)]
    [tag
     {:id                      uid
      :dangerouslySetInnerHTML {:__html text}}]))

(defn- lib-node [{:keys [db/uid] ::lib/keys [src sub-type] :as lib}]
  (let [tag (case sub-type :html :body :css :style :javascript :script)]
    [tag
     {:id  uid
      :src src}]))

(defn- file-node-markup [file string]
  #?(:cljs
     (reagent.dom.server/render-to-static-markup
      [file-node file string])))

(defn- snapshot-node-markup [snapshot]
  #?(:cljs
     (reagent.dom.server/render-to-static-markup
      [snapshot-node snapshot])))

(defn- lib-node-markup [lib]
  #?(:cljs
     (reagent.dom.server/render-to-static-markup
      [lib-node lib])))

(defn- preview-markup
  [vims {::branch/keys [files libs]}]
  #?(:cljs
     (reagent.dom.server/render-to-static-markup
      (let [by-subtype     (group-by ::file/sub-type files)
            libs-string    (transduce (map lib-node-markup) str libs)
            license-string (<sub [::license.subs/license-string-html-comment vims])
            license+libs   (str license-string libs-string)
            head-string    (transduce (map #(<sub [::preprocessed-file-markup vims %]))
                                      str
                                      license+libs
                                      (:css by-subtype))
            html-file      (-> by-subtype :html first)
            body-string    (transduce
                            (map #(<sub [::preprocessed-file-markup vims %]))
                            str
                            (when html-file
                              (<sub [::preprocessed-file-markup vims html-file]))
                            (:javascript by-subtype))]
        [:html
         [:head
          {:dangerouslySetInnerHTML
           {:__html head-string}}]
         [:body
          {:id                      (:db/uid html-file)
           :dangerouslySetInnerHTML {:__html body-string}}]]))))

(defn snapshots-markup
  [snapshots libs license-string]
  #?(:cljs
     (reagent.dom.server/render-to-static-markup
      (let [{:keys
             [html
              css
              javascript]} (group-by ::snapshot/sub-type snapshots)
            libs-string  (transduce (map lib-node-markup) str libs)
            license+libs (str license-string libs-string)
            head-string  (transduce (map snapshot-node-markup) str license+libs css)
            body-string  (transduce (map snapshot-node-markup) str (concat html javascript))
            body-id      (-> html first ::snapshot/file-uid)]
        [:html
         [:head
          {:dangerouslySetInnerHTML
           {:__html head-string}}]
         [:body
          {:id                      body-id
           :dangerouslySetInnerHTML {:__html body-string}}]]))))

(re-frame/reg-sub
 ::preprocessed-file-markup
 (fn [[_ vims file]]
   (re-frame/subscribe [::vcs.subs/preprocessed-file-string vims file]))
 (fn [string [_ _ {::file/keys [sub-type] :as file}]]
   (file-node-markup file string)))

(re-frame/reg-sub
 ::vims-preprocessed-preview-markup
 (fn [[_ vims]]
   (re-frame/subscribe [::vcs.subs/branch vims]))
 (fn [branch [_ vims]]
   (preview-markup vims branch)))

(re-frame/reg-sub
 ::file-string+file-lint-or-preprocessing-errors
 (fn [[_ vims file]]
   [(re-frame/subscribe [::vcs.subs/file-string vims file])
    (re-frame/subscribe [::vcs.subs/file-lint-or-preprocessing-errors vims file])
    ; sub libs to trigger tracker in live-preview
    (re-frame/subscribe [::vcs.subs/libs vims])])
 (fn [file-string+file-lint-or-preprocessing-errors+libs _]
   (vec file-string+file-lint-or-preprocessing-errors+libs)))

(re-frame/reg-sub
 ::error-catcher-branch-libs
 (fn [[_ vims]]
   (re-frame/subscribe [::vcs.subs/branch vims]))
 (fn [branch _]
   (-> branch
       (dissoc ::branch/files)
       (update ::branch/libs (fn [libs] (filter lib/javascript? libs))))))

(re-frame/reg-sub
 ::error-catcher-js-libs-markup
 (fn [[_ vims]]
   (re-frame/subscribe [::error-catcher-branch-libs vims]))
 (fn [branch [_ vims]]
   (preview-markup vims branch)))

(re-frame/reg-sub-raw
 ::error-catcher-js-file-string
 (fn [_ [_ vims]]
   (interop/make-reaction
    #(let [{::branch/keys [files]} (<sub [::vcs.subs/branch vims])
           js-file (util/ffilter file/javascript? files)]
       (<sub [::vcs.subs/preprocessed-file-string js-file])))))
