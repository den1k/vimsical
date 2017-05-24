(ns vimsical.vims
  (:require
   [clojure.spec :as s]
   [vimsical.common.util.core :as util]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.vcs.branch :as vcs.branch]
   [vimsical.vcs.file :as vcs.file]))

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
(s/def ::vims (s/keys :req [:db/uid ::owner ::branches] :opt [::title ::cast]))

;;
;; * Helpers
;;

(defn master-branch [vims] (-> vims ::branches vcs.branch/master))

;;
;; * Constructor
;;

(s/def ::branch-uid ::uid)
(s/def ::created-at ::vcs.branch/created-at)
(s/def ::new-vims-opts (s/keys :opt-un [::uid ::branch-uid ::created-at]))

(s/fdef new-vims
        :args (s/cat :owner ::owner
                     :title (s/nilable ::title)
                     :files ::vcs.branch/files
                     :opts (s/? (s/nilable ::new-vims-opts)))
        :ret ::vims)

(defn new-vims
  ([owner title files]
   (new-vims owner title files nil))
  ([owner title files {:keys [uid
                              branch-uid created-at]
                       :or   {uid        (uuid)
                              branch-uid (uuid)
                              created-at (util/now)}}]
   ;; NOTE the vcs doesn't know of ::branch/owner, so we manually add it here,
   ;; might want to move that concern down there?
   (letfn [(new-branches [vims-owner]
             (-> (vcs.branch/new-branch branch-uid created-at files)
                 (assoc ::vcs.branch/owner owner)
                 (vector)))]
     (let [owner'   (select-keys owner [:db/uid])
           branches (new-branches owner')]
       (-> {:db/uid uid ::owner owner' ::branches branches}
           (util/assoc-some ::title title))))))

(defn default-files
  ([] (default-files (uuid) (uuid) (uuid)))
  ([html-uid css-uid javascript-uid]
   [(vcs.file/new-file html-uid       :text :html)
    (vcs.file/new-file css-uid        :text :css)
    (vcs.file/new-file javascript-uid :text :javascript)]))
