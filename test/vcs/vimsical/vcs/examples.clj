(ns vimsical.vcs.examples
  (:require
   [clojure.spec :as s]
   [vimsical.common.test :refer [uuid]]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]
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

(s/assert* (s/coll-of ::branch/branch) branches)


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

(s/assert* (s/coll-of ::delta/delta) deltas)


;; * Latest file deltas

(def latest-file-deltas
  {master-id {file1-id d8}
   child-id  {file1-id d3 file2-id d7}
   gchild-id {file1-id d5 file2-id d6}})


;; * Delta index

(def delta-index
  {master-id (#'state.branches/new-index [d0 d1 d8])
   child-id  (#'state.branches/new-index [d2 d3 d7])
   gchild-id (#'state.branches/new-index [d4 d5 d6])})

;; (s/assert* ::state.branches/delta-index delta-index)


;; * Branch tree

(def branch-tree
  (assoc master ::branch/children [(assoc child ::branch/children [gchild])]))

;; (def state
;;   (let [[d1 d2 d3]
;;         [{:branch-id #uuid :branch, :file-id #uuid :file, :prev-id nil,                :id #uuid [:str/ins 0], :op [:str/ins nil "f"], :pad 1, :meta {:timestamp 123, :version 0.3}}
;;          {:branch-id #uuid :branch, :file-id #uuid :file, :prev-id #uuid [:str/ins 0], :id #uuid [:str/ins 1], :op [:str/ins #uuid [:str/ins 0] "u"], :pad 1, :meta {:timestamp 123, :version 0.3}}
;;          {:branch-id #uuid :branch, :file-id #uuid :file, :prev-id #uuid [:str/ins 1], :id #uuid [:str/ins 2], :op [:str/ins #uuid [:str/ins 1] "n"], :pad 1, :meta {:timestamp 123, :version 0.3}}]]
;;     {d1 {:latest-deltas    {:<branch-id> {:<file-id> d1}}
;;          :branch-tree      :<branch-tree>
;;          :deltas-by-branch {#uuid :branch [d1]}
;;          :string-by-file {}}}))
