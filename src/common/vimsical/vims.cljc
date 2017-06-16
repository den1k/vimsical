(ns vimsical.vims
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.vcs.branch :as vcs.branch]
   [vimsical.vcs.snapshot :as vcs.snapshot]
   [vimsical.common.util.core :as util]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.vcs.file :as vcs.file])
  #?(:cljs (:refer-clojure :exclude [uuid])))

;;
;; * Spec
;;

(s/def ::uid uuid?)
(s/def :db/uid ::uid)
(s/def ::title string?)
(s/def ::cast boolean?)
;; NOTE branch owner is not defined in vcs
(s/def ::owner (s/keys :req [:db/uid]))
(s/def ::vcs.branch/owner ::owner)
(s/def ::branch (s/merge ::vcs.branch/branch (s/keys :req [::vcs.branch/owner])))
(s/def ::branches (s/every ::branch))
(s/def ::snapshots (s/every ::vcs.snapshot/frontend-snapshot))
(s/def ::vims (s/keys :req [:db/uid ::owner ::branches] :opt [::title ::cast ::snapshots]))
(s/def ::create-at inst?)
;;
;; * Helpers
;;

(defn master-branch [vims] (-> vims ::branches vcs.branch/master))

;;
;; * Constructor
;;

(s/def ::branch-uid ::uid)
(s/def ::new-vims-opts (s/keys :opt-un [::uid ::branch-uid ::vcs.branch/created-at]))

(defn default-files
  ([] (default-files (uuid) (uuid) (uuid)))
  ([html-uid css-uid javascript-uid]
   [(vcs.file/new-file html-uid       :text :html)
    (vcs.file/new-file css-uid        :text :css)
    (vcs.file/new-file javascript-uid :text :javascript)]))

(s/fdef new-vims
        :args (s/cat :owner ::owner
                     :opts  (s/? (s/cat :title (s/nilable ::title)
                                        :files ::vcs.branch/files
                                        :opts (s/nilable ::new-vims-opts)))))


(defn new-vims
  ([owner] (new-vims owner nil (default-files) nil))
  ([owner title files {:keys [uid branch-uid created-at]
                       :or   {uid        (uuid)
                              branch-uid (uuid)
                              created-at (util/now)}}]
   ;; NOTE the vcs doesn't know of ::branch/owner, so we manually add it here,
   ;; might want to move that concern down there?
   (letfn [(new-branches [owner]
             (-> (vcs.branch/new-branch branch-uid created-at files)
                 (assoc ::vcs.branch/owner owner)
                 (vector)))]
     (let [branches (new-branches owner)]
       (-> {:db/uid      uid
            ::owner      owner
            ::branches   branches
            ::created-at created-at}
           (util/assoc-some ::title title))))))
