(ns vimsical.backend.data
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.common.test :as uuid :refer [uuid]]
   [vimsical.common.util.core :as util]
   [vimsical.user :as user]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.compiler :as compiler]
   [vimsical.vcs.core :as vcs]
   [vimsical.vcs.data.gen.diff :as diff]
   [vimsical.vcs.editor :as editor]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.lib :as lib]
   [vimsical.vcs.snapshot :as snapshot]
   [vimsical.vims :as vims]))

;;
;; * Helpers
;;

(def js-libs
  [{:db/uid        (uuid ::lib/jquery)
    ::lib/name    "jQuery"
    ::lib/type     :text
    ::lib/sub-type :javascript
    ::lib/src      "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.1.1/jquery.min.js"}])

(def sub-type->libs (group-by ::lib/sub-type js-libs))

(def compilers
  [{:db/uid                (uuid ::compiler/babel)
    ::compiler/name        "Babel"
    ::compiler/type        :text
    ::compiler/sub-type    :babel
    ::compiler/to-sub-type :javascript}])

(def to-sub-type->compiler (util/project ::compiler/to-sub-type compilers))

(defn- new-file
  ([uuid type sub-type] (new-file uuid type sub-type nil nil))
  ([uuid type sub-type lang-version compilers]
   (-> {:db/uid         uuid
        ::file/type     type
        ::file/sub-type sub-type}
       (util/assoc-some
        ::file/lang-version lang-version
        ::file/compiler (get compilers sub-type)))))

(defn- new-branch
  [uuid user-uid name files libs]
  (-> {:db/uid                       uuid
       ::branch/owner                {:db/uid user-uid}
       ::branch/name                 name
       ::branch/created-at           (util/now)
       ::branch/files                files}
      (util/assoc-some ::branch/libs libs)))

;;
;; * Entities
;;

(def html-file (new-file (uuid ::html) :text :html))
(def css-file (new-file (uuid ::css) :text :css))
(def javascript-file (new-file (uuid ::js) :text :javascript "5" compilers))

(def files
  [html-file css-file javascript-file])

(def branches
  [(new-branch (uuid ::master) (uuid ::user) "master" files (:javascript js-libs))])

(def vims
  {:db/uid         (uuid ::vims)
   ::vims/owner    {:db/uid (uuid ::user)}
   ::vims/title    "Title"
   ::vims/branches branches})

(s/assert ::vims/vims vims)

(def user
  {:db/uid           (uuid ::user)
   ::user/first-name "Jane"
   ::user/last-name  "Applecrust"
   ::user/email      "kalavox@gmail.com"
   ::user/vimsae     [vims]})

(s/assert ::user/user user)

;;
;; * VCS data
;;

(def html-file (first files))
(def html-file-string "abcdef")

;;
;; ** Edit events
;;

(def edit-events (diff/diffs->edit-events "" [html-file-string]))

;;
;; ** Effects
;;

(def deltas-uuid-gen  (uuid/uuid-gen))
(def deltas-uuid-fn   (:f deltas-uuid-gen))
(def deltas-uuid-seq  (:seq deltas-uuid-gen))

(def effects
  {::editor/pad-fn       (constantly 1)
   ::editor/timestamp-fn (constantly 2)
   ::editor/uuid-fn      deltas-uuid-fn})

;;
;; * Deltas
;;

(def deltas
  (let [{html-uid :db/uid}     html-file
        vcs                    (vcs/empty-vcs branches)
        [vcs deltas delta-uid] (diff/diffs->vcs vcs effects html-uid (uuid ::master) nil "" [html-file-string])
        deltas                 (vcs/deltas vcs delta-uid)]
    deltas))

;;
;; * Snapshots
;;

(def snapshots
  [{:db/uid              (uuid ::html-snapshot)
    ::snapshot/text      html-file-string
    ::snapshot/file-uid  (uuid ::html)
    ::snapshot/user-uid  (uuid ::user)
    ::snapshot/vims-uid  (uuid ::vims)
    ::snapshot/type      :text
    ::snapshot/sub-type  :html
    ::snapshot/delta-uid nil}])

(def me
  (update-in user [::user/vimsae 0] assoc ::vims/snapshots snapshots))

;;
;; * Sync
;;

(def deltas-by-branch-uid
  {(uuid ::master)
   (select-keys (last deltas) [:uid :prev-uid :branch-uid])})
