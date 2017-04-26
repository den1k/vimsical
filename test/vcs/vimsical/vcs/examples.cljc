(ns vimsical.vcs.examples
  (:require
   [clojure.spec :as s]
   [vimsical.common.test :refer [uuid]]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.data.indexed.vector :as indexed]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.state.branch-pointers :as state.branch-pointers]
   [vimsical.vcs.state.branches :as state.branches]))

;; * UUIDs

(def master-id (uuid :master))
(def child-id (uuid :child))
(def gchild-id (uuid :gchild))
(def file1-id (uuid :file1))
(def file2-id (uuid :file2))

(def id0 (uuid :id0))
(def id1 (uuid :id1))
(def id2 (uuid :id2))
(def id3 (uuid :id3))
(def id4 (uuid :id4))
(def id5 (uuid :id5))
(def id6 (uuid :id6))
(def id7 (uuid :id7))
(def id8 (uuid :id8))


;; * Branches

(def master {:db/id master-id})
(def child  {:db/id child-id  ::branch/parent master ::branch/entry-delta-id id1 ::branch/start-deltas-id id2})
(def gchild {:db/id gchild-id ::branch/parent child  ::branch/entry-delta-id id3 ::branch/start-deltas-id id4})

(def branches [master child gchild])

#_(s/assert* (s/coll-of ::branch/branch) branches)


;; * Deltas

(def d0 {:branch-id master-id, :id id0 :prev-id nil, :file-id file1-id, :op [:str/ins nil "h"], :pad 0,   :meta {:timestamp 1, :version 1.0}})
(def d1 {:branch-id master-id, :id id1 :prev-id id0, :file-id file1-id, :op [:crsr/mv id0],     :pad 1,   :meta {:timestamp 1, :version 1.0}})
(def d2 {:branch-id child-id,  :id id2 :prev-id id1, :file-id file1-id, :op [:str/ins id0 "i"], :pad 100, :meta {:timestamp 1, :version 1.0}})
(def d3 {:branch-id child-id,  :id id3 :prev-id id2, :file-id file1-id, :op [:crsr/mv id2],     :pad 1,   :meta {:timestamp 1, :version 1.0}})
(def d4 {:branch-id gchild-id, :id id4 :prev-id id3, :file-id file1-id, :op [:str/ins id2 "!"], :pad 100, :meta {:timestamp 1, :version 1.0}})
(def d5 {:branch-id gchild-id, :id id5 :prev-id id4, :file-id file1-id, :op [:crsr/mv id4],     :pad 1,   :meta {:timestamp 1, :version 1.0}})
(def d6 {:branch-id gchild-id, :id id6 :prev-id id5, :file-id file1-id, :op [:str/rem id4 1],   :pad 1,   :meta {:timestamp 1, :version 1.0}})
(def d7 {:branch-id child-id,  :id id7 :prev-id id3, :file-id file1-id, :op [:str/rem id2 1],   :pad 1,   :meta {:timestamp 1, :version 1.0}})
(def d8 {:branch-id master-id, :id id8 :prev-id id1, :file-id file1-id, :op [:str/ins id0 "ey"],:pad 1,   :meta {:timestamp 1, :version 1.0}})

(def deltas [d0 d1 d2 d3 d4 d5 d6 d7 d8])

#_(s/assert* (s/coll-of ::delta/delta) deltas)


;; * Latest file deltas

(def latest-file-deltas
  {master-id {file1-id d8}
   child-id  {file1-id d3 file2-id d7}
   gchild-id {file1-id d5 file2-id d6}})


;; * Branches

(def deltas-by-branch-id
  {master-id (indexed/vec-by :id [d0 d1 d8])
   child-id  (indexed/vec-by :id [d2 d3 d7])
   gchild-id (indexed/vec-by :id [d4 d5 d6])})

#_(s/assert* ::state.branches/deltas-by-branch-id deltas-by-branch-id)

(def branch-pointers-by-branch-id
  {master-id {::state.branch-pointers/start (:id d0) ::state.branch-pointers/end (:id d8)}
   child-id  {::state.branch-pointers/start (:id d2) ::state.branch-pointers/end (:id d7)}
   gchild-id {::state.branch-pointers/start (:id d4) ::state.branch-pointers/end (:id d6)}})

(s/assert* ::state.branch-pointers/branch-pointers-by-branch-id branch-pointers-by-branch-id)


;; * Branch tree

(def branch-tree
  (assoc master ::branch/children [(assoc child ::branch/children [gchild])]))
