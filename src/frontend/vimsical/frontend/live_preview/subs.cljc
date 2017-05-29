(ns vimsical.frontend.live-preview.subs
  (:require
   [re-frame.core :as re-frame]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.lib :as lib]
   [vimsical.vcs.branch :as branch]
   [vimsical.frontend.util.re-frame :refer [<sub]]
   #?(:cljs [reagent.dom.server :as reagent.dom.server])
   [vimsical.common.util.core :as util :include-macros true]
   [re-frame.interop :as interop]))

(defn- file-node [{:keys [db/uid] ::file/keys [sub-type] :as file} string]
  (let [tag (case sub-type :html :body :css :style :javascript :script)]
    [tag
     {:id                      uid
      :dangerouslySetInnerHTML {:__html string}}]))

(defn- lib-node [{:keys [db/uid] ::lib/keys [src sub-type] :as lib}]
  (let [tag (case sub-type :html :body :css :style :javascript :script)]
    [tag
     {:id  uid
      :src src}]))

(defn- file-node-markup [file string]
  #?(:cljs
     (reagent.dom.server/render-to-static-markup
      [file-node file string])))

(defn- lib-node-markup [lib]
  #?(:cljs
     (reagent.dom.server/render-to-static-markup
      [lib-node lib])))

(defn- preview-markup
  [{::branch/keys [files libs]}]
  #?(:cljs
     (reagent.dom.server/render-to-static-markup
      (let [by-subtype  (group-by ::file/sub-type files)
            libs-string (transduce (map lib-node-markup) str libs)
            head-string (transduce (map #(<sub [::preprocessed-file-markup %]))
                                   str
                                   libs-string
                                   (:css by-subtype))
            html-file   (-> by-subtype :html first)
            body-string (transduce
                         (map #(<sub [::preprocessed-file-markup %]))
                         str
                         (when html-file
                           (<sub [::preprocessed-file-markup html-file]))
                         (:javascript by-subtype))]
        [:html
         [:head
          {:dangerouslySetInnerHTML
           {:__html head-string}}]
         [:body
          {:id                      (:db/uid html-file)
           :dangerouslySetInnerHTML {:__html body-string}}]]))))

(re-frame/reg-sub
 ::preprocessed-file-markup
 (fn [[_ file]]
   (re-frame/subscribe [::vcs.subs/preprocessed-file-string file]))
 (fn [string [_ {::file/keys [sub-type] :as file}]]
   (file-node-markup file string)))

(re-frame/reg-sub-raw
 ::vims-preprocessed-preview-markup
 (fn [_ [_ vims]]
   {:pre [vims]}
   (interop/make-reaction
    #(let [branch (<sub [::vcs.subs/branch vims])]
       (preview-markup branch)))))

(re-frame/reg-sub-raw
 ::branch-preprocessed-preview-markup
 (fn [_ [_ vims]]
   (interop/make-reaction
    #(let [branch (<sub [::vcs.subs/branch vims])]
       (<sub [::vims-preprocessed-preview-markup branch])))))

(re-frame/reg-sub
 ::error-catcher-branch-libs
 :<- [::vcs.subs/branch]
 (fn [branch _]
   (-> branch
       (dissoc ::branch/files)
       (update ::branch/libs (fn [libs] (filter lib/javascript? libs))))))

(re-frame/reg-sub
 ::error-catcher-js-libs-markup
 :<- [::error-catcher-branch-libs]
 (fn [branch _]
   (preview-markup branch)))

(re-frame/reg-sub-raw
 ::error-catcher-js-file-string
 (fn [_ _]
   (interop/make-reaction
    #(let [{::branch/keys [files]} (<sub [::vcs.subs/branch])
           js-file (util/ffilter file/javascript? files)]
       (<sub [::vcs.subs/preprocessed-file-string js-file])))))
