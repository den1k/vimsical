(ns vimsical.backend.data
  (:require
   [vimsical.vims :as vims]
   [vimsical.user :as user]
   [vimsical.common.test :refer [uuid]]
   [vimsical.common.util.core :as util]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.compiler :as compiler]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.lib :as lib]
   [clojure.spec :as s]))

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
       ::branch/start-delta-uid      nil
       ::branch/branch-off-delta-uid nil
       ::branch/created-at           (util/now)
       ::branch/files                files}
      (util/assoc-some ::branch/libs libs)))

;;
;; * Entities
;;

(def files
  [(new-file (uuid ::html) :text :html)
   (new-file (uuid ::css) :text :css)
   (new-file (uuid ::js) :text :javascript "5" compilers)])

(def branches
  [(new-branch (uuid ::master) (uuid ::user) "master" files (:javascript js-libs))])

(def vims
  {:db/uid         (uuid ::vims)
   ::vims/owner   {:db/uid (uuid ::user)}
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
