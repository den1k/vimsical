(ns vimsical.frontend.db
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.common.test :refer [uuid]]
   [vimsical.common.util.core :as util]
   [vimsical.frontend.quick-search.commands :as quick-search.commands]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.compiler :as compiler]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.lib :as lib])
  (:refer-clojure :exclude [uuid]))

;;
;; * State
;;

(def js-libs
  [{:db/id         (uuid :lib-js-jquery)
    ::lib/title    "jQuery"
    ::lib/type     :text
    ::lib/sub-type :javascript
    ::lib/src      "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.1.1/jquery.min.js"}])

(def sub-type->libs (group-by ::lib/sub-type js-libs))

(def compilers
  [{:db/id                 (uuid :babel-compiler)
    ::compiler/name        "Babel"
    ::compiler/type        :text
    ::compiler/sub-type    :babel
    ::compiler/to-sub-type :javascript}])

(def to-sub-type->compiler (util/project ::compiler/to-sub-type compilers))

(defn new-file
  ([uuid type sub-type] (new-file uuid type sub-type nil nil))
  ([uuid type sub-type lang-version compilers]
   (-> {:db/id          uuid
        ::file/type     type
        ::file/sub-type sub-type}
       (util/assoc-some
        ::file/lang-version lang-version
        ::file/compiler (get compilers sub-type)))))

(defn new-branch
  [uuid name files libs]
  (-> {:db/id                       uuid
       ::branch/name                name
       ::branch/start-delta-id      nil
       ::branch/branch-off-delta-id nil
       ::branch/created-at          (util/now)
       ::branch/files               files}
      (util/assoc-some ::branch/libs libs)))

(defn new-vims
  ([author-ref title] (new-vims author-ref title {}))
  ([author-ref title {:keys [js-libs compilers]}]
   (let [files    [(new-file (uuid title :file-html) :text :html)
                   (new-file (uuid title :file-css) :text :css)
                   (new-file (uuid title :file-js) :text :javascript "5" compilers)]
         branches [(new-branch (uuid title :master) "master" files (:javascript js-libs))]]
     {:db/id         (uuid title)
      :vims/author   author-ref
      :vims/title    title
      :vims/branches branches})))
(let [state     {:app/user         {:db/id           (uuid :user)
                                    :user/first-name "Jane"
                                    :user/last-name  "Applecrust"
                                    :user/email      "kalavox@gmail.com"
                                    :user/vimsae
                                    [(new-vims [:db/id (uuid :user)] "NLP Chatbot running on React Fiber")
                                     (new-vims [:db/id (uuid :user)] "CatPhotoApp" {:js-libs sub-type->libs :compilers to-sub-type->compiler})]}
                 :app/vims         [:db/id (uuid "NLP Chatbot running on React Fiber")]
                 :app/quick-search {:db/id                            (uuid :quick-search)
                                    :quick-search/show?               false
                                    :quick-search/result-idx          0
                                    :quick-search/query               ""
                                    :quick-search/commands            quick-search.commands/commands
                                    :quick-search/filter-idx          nil
                                    :quick-search/filter-result-idx   nil
                                    :quick-search/filter-category-idx nil}
                 :app/libs         js-libs
                 :app/compilers    compilers
                 :app/route        :route/vcr}]

  (def default-db
    (-> (mg/new-db)
        (mg/add-id-attr :db/id)
        (util.mg/add-linked-entities state))))

(re-frame/reg-event-db ::init (constantly default-db))
