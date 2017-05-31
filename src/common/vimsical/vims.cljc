(ns vimsical.vims
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.branch :as vcs.branch]
   [vimsical.common.util.core :as util]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.compiler :as compiler]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.lib :as lib])
  (:refer-clojure :exclude [uuid]))

(s/def ::uid uuid?)
(s/def :db/uid ::uid)
(s/def ::title string?)
(s/def ::cast boolean?)
;; NOTE branch owner is not defined in vcs
(s/def ::owner (s/keys :req [:db/uid]))
(s/def ::vcs.branch/owner ::owner)
(s/def ::branch
  (s/merge ::vcs.branch/branch (s/keys :req [::vcs.branch/owner])))
(s/def ::branches (s/every ::branch))

(s/def ::vims
  (s/keys :req [:db/uid ::owner ::branches] :opt [::title ::cast]))

(defn master-branch [vims] (-> vims ::branches vcs.branch/master))

;;
;; * New Vims
;;

(def js-libs
  [{:db/uid        (uuid)
    ::lib/title    "jQuery"
    ::lib/type     :text
    ::lib/sub-type :javascript
    ::lib/src      "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.1.1/jquery.min.js"}])

(def sub-type->libs (group-by ::lib/sub-type js-libs))

(def compilers
  [{:db/uid                (uuid)
    ::compiler/name        "Babel"
    ::compiler/type        :text
    ::compiler/sub-type    :babel
    ::compiler/to-sub-type :javascript}])

(def to-sub-type->compiler (util/project ::compiler/to-sub-type compilers))

(defn- new-file
  ([uid type sub-type] (new-file uid type sub-type nil nil))
  ([uid type sub-type lang-version compilers]
   (-> {:db/uid         uid
        ::file/type     type
        ::file/sub-type sub-type}
       (util/assoc-some
        ::file/lang-version lang-version
        ::file/compiler (get compilers sub-type)))))

(defn- new-branch
  [uid owner name files libs]
  (-> {:db/uid                       uid
       ::branch/owner                owner
       ::branch/name                 name
       ::branch/start-delta-uid      nil
       ::branch/branch-off-delta-uid nil
       ::branch/created-at           (util/now)
       ::branch/files                files}
      (util/assoc-some ::branch/libs libs)))

(defn new-vims
  ([uid owner] (new-vims uid owner nil {}))
  ([uid owner title] (new-vims uid owner title {}))
  ([uid owner title {:keys [js-libs compilers]}]
   (let [files    [(new-file (uuid) :text :html)
                   (new-file (uuid) :text :css)
                   (new-file (uuid) :text :javascript "5" compilers)]
         branches [(new-branch (uuid) owner "master" files (:javascript js-libs))]]

     {:db/uid    uid
      ::owner    owner
      ::title    title
      ::branches branches})))