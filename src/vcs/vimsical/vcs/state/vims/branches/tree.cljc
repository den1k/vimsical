(ns vimsical.vcs.state.vims.branches.tree
  (:require
   [clojure.spec :as s]
   [vimsical.common.coll :refer [index-by]]
   [vimsical.vcs.branch :as branch]))


;; * Spec

(s/def ::branch/children (s/every ::tree))
(s/def ::tree (s/merge ::branch/branch (s/keys :opt [::branch/children])))


;; * Internal

(defn- assoc-maybe [m k v] (cond-> m (some? v) (assoc k v)))

(defn- branch-tree*
  [branches-by-parent-id {:keys [db/id] :as branch}]
  (letfn [(recur-children [children]
            (cond->> children
              (seq children)
              (mapv (fn [child] (branch-tree* branches-by-parent-id child)))))]
    (assoc-maybe
     branch ::branch/children
     (recur-children
      (get branches-by-parent-id id)))))

(defn- parent-id [branch] (-> branch ::branch/parent :db/id))


;; * API

(s/fdef branch-tree
        :args (s/every ::branch/branch)
        :ret  ::tree)

(defn branch-tree
  [branches]
  (let [branches-by-parent-id (group-by parent-id branches)
        [master & error]      (get branches-by-parent-id nil)]
    (if error
      (throw (ex-info "Found more than one branch with no parent:" {:error error :master master}))
      (branch-tree* branches-by-parent-id master))))
