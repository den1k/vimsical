(ns vimsical.vcs.branch
  (:refer-clojure :exclude [ancestors common-ancestors])
  (:require
   [clojure.spec :as s]
   [vimsical.common.core :as common :refer [=by some-val]]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.lib :as lib]))

;;
;; * Spec
;;

(s/def ::branch
  (s/keys :req [:db/uid] :opt [::start-delta-uid ::entry-delta-uid ::parent ::files ::name]))

;;
;; ** Attributes
;;

(s/def ::uid uuid?)
(s/def :db/uid ::uid)
(s/def ::name string?)
(s/def ::start-delta-uid (s/nilable ::delta/uid))
(s/def ::entry-delta-uid (s/nilable ::delta/uid))
(s/def ::branch-off-delta-uid (s/nilable ::delta/uid))
(s/def ::created-at nat-int?)

;;
;; ** Relations
;;

(s/def ::parent (s/nilable ::branch))
(s/def ::files (s/coll-of ::file/file))
(s/def ::libs (s/coll-of ::lib/lib))

;;
;; * Lineage
;;

(defn master? [{::keys [parent]}] (nil? parent))

(defn parent?
  [parent child]
  (=by :db/uid (comp :db/uid ::parent) parent child))

(defn master
  [branches]
  (some-val master? branches))

(defn lineage
  "Return a seq of that `branch`'s lineage, starting with itself,
  then its direct parent, etc all the way to master.

  Expects branch to be *fully denormalized*, i.e. each branch contains its
  parent recursively."
  ([branch] (lineage [branch] branch))
  ([acc {::keys [parent] :as branch}]
   (if (nil? parent)
     acc
     (recur (conj acc parent) parent))))

(defn ancestors
  "Return a seq of that `branch`'s ancestors, starting with parent.

  Expects branch to be *fully denormalized*, i.e. each branch contains its
  parent recursively."
  [branch] (not-empty (lineage [] branch)))

(defn common-ancestor
  "Return the common ancestor of the given branches if any."
  [branch-a branch-b]
  (let [ancestors-a (ancestors branch-a)
        ancestors-b (ancestors branch-b)]
    (some-val
     (fn [ancestor-a]
       (some-val (=by :db/uid ancestor-a) ancestors-b))
     ancestors-a)))

(defn depth
  "Return the count of ancestors "
  ^long [{::keys [parent] :as branch}]
  (if (nil? parent)
    0
    (count (ancestors branch))))

(defn relative-depth
  "Return the count of ancestors between `base` and `child`. Returns 0 is base
  and child are equal, nil if they have no common ancestor."
  ([base child]
   (let [pred (=by :db/uid base)]
     (if (pred child)
       0
       (some
        (fn [[i branch]]
          (when (pred branch) i))
        ;; Vectors of [n ancestor] starting at [1 parent]
        (next (map-indexed vector (lineage child))))))))

(defn in-lineage?
  "Return true if `other` is part of `branch`'s lineage, either its direct
  parent or an ancestor."
  ([branch other]
   (in-lineage? (lineage branch) branch other))
  ([lineage branch other]
   (boolean
    (some
     (=by :db/uid other)
     lineage))))

;;
;; * Tree
;;

(s/def ::children (s/every ::tree))
(s/def ::tree (s/merge ::branch (s/keys :opt [::children])))

;;
;; ** Internal
;;

(defn- tree*
  [branches-by-parent-uid {:keys [db/uid] :as branch}]
  (letfn [(assoc-maybe [m k v] (cond-> m (some? v) (assoc k v)))
          (recur-children [children]
            (cond->> children
              (seq children)
              (mapv (fn [child] (tree* branches-by-parent-uid child)))))]
    (assoc-maybe
     branch ::children
     (recur-children
      (get branches-by-parent-uid uid)))))

;;
;; ** API
;;

(s/fdef branch-tree
        :args (s/cat :branches (s/every ::branch))
        :ret ::tree)

(defn branch-tree
  [branches]
  (letfn [(parent-uid [branch] (-> branch ::parent :db/uid))]
    (let [branches-by-parent-uid (group-by parent-uid branches)
          [master & error]       (get branches-by-parent-uid nil)]
      (if error
        (throw (ex-info "Found more than one branch with no parent:" {:error error :master master}))
        (tree* branches-by-parent-uid master)))))
