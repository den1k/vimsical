(ns vimsical.vcs.examples
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.state.vims.branches.delta-index :as branches.delta-index]
   [vimsical.vcs.state.vims.branches.tree :as branches.tree]
   [vimsical.common.test :refer [uuid]]))


;; * UUIDs

(def master-id (uuid :master))
(def child-id (uuid :child))
(def gchild-id (uuid :gchild))
(def file1-id (uuid :file1))
(def file2-id (uuid :file2))

(def id0 (uuid :delta-0))
(def id1 (uuid :delta-1))
(def id2 (uuid :delta-2))
(def id3 (uuid :delta-3))
(def id4 (uuid :delta-4))
(def id5 (uuid :delta-5))
(def id6 (uuid :delta-6))
(def id7 (uuid :delta-7))
(def id8 (uuid :delta-8))


;; * Branches

(def master {:db/id master-id})
(def child  {:db/id child-id  ::branch/parent master ::branch/entry-delta-id id1 ::branch/start-deltas-id id2})
(def gchild {:db/id gchild-id ::branch/parent child  ::branch/entry-delta-id id3 ::branch/start-deltas-id id4})

(def branches [master child gchild])

(s/assert* (s/coll-of ::branch/branch) branches)


;; * Deltas

(def d0 {:branch-id master-id, :file-id file1-id, :id id0 :prev-id nil, :op [:str/ins nil "h"], :pad 0,   :meta {:timestamp 1, :version 1.0}})
(def d1 {:branch-id master-id, :file-id file1-id, :id id1 :prev-id id0, :op [:crsr/mv 1],       :pad 1,   :meta {:timestamp 1, :version 1.0}})
(def d2 {:branch-id child-id,  :file-id file1-id, :id id2 :prev-id id1, :op [:str/ins 0   "i"], :pad 100, :meta {:timestamp 1, :version 1.0}})
(def d3 {:branch-id child-id,  :file-id file1-id, :id id3 :prev-id id2, :op [:crsr/mv 1],       :pad 1,   :meta {:timestamp 1, :version 1.0}})
(def d4 {:branch-id gchild-id, :file-id file1-id, :id id4 :prev-id id3, :op [:str/ins 1   "!"], :pad 100, :meta {:timestamp 1, :version 1.0}})
(def d5 {:branch-id gchild-id, :file-id file1-id, :id id5 :prev-id id4, :op [:crsr/mv 1],       :pad 1,   :meta {:timestamp 1, :version 1.0}})
(def d6 {:branch-id child-id,  :file-id file2-id, :id id6 :prev-id id5, :op [:crsr/mv 1],       :pad 1,   :meta {:timestamp 1, :version 1.0}})
(def d7 {:branch-id gchild-id, :file-id file2-id, :id id7 :prev-id id6, :op [:crsr/mv 1],       :pad 1,   :meta {:timestamp 1, :version 1.0}})
(def d8 {:branch-id master-id, :file-id file1-id, :id id8 :prev-id id1, :op [:crsr/mv 1],       :pad 1,   :meta {:timestamp 1, :version 1.0}})

(def deltas [d0 d1 d2 d3 d4 d5 d6 d7 d8])

(s/assert* (s/coll-of ::delta/delta) deltas)


;; * Delta index

(def delta-index
  {master-id (#'branches.delta-index/new-index [d0 d1 d8])
   child-id  (#'branches.delta-index/new-index [d2 d3 d6])
   gchild-id (#'branches.delta-index/new-index [d4 d5 d7])})

(s/assert* ::branches.delta-index/delta-index delta-index)


;; * Branch tree

(def branch-tree
  (assoc master ::branch/children [(assoc child ::branch/children [gchild])]))

(s/assert* ::branches.tree/tree branch-tree)
