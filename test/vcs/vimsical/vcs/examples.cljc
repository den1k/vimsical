(ns vimsical.vcs.examples
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.common.test :refer [uuid]]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.data.indexed.vector :as indexed]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.state.branch-pointers :as state.branch-pointers]
   [vimsical.vcs.state.branches :as state.branches]))

;;
;; * UUIDs
;;

(def master-uid (uuid :master))
(def child-uid  (uuid :child))
(def gchild-uid (uuid :gchild))
(def file1-uid  (uuid :file1))
(def file2-uid  (uuid :file2))

(def id0 (uuid :uid0))
(def id1 (uuid :uid1))
(def id2 (uuid :uid2))
(def id3 (uuid :uid3))
(def id4 (uuid :uid4))
(def id5 (uuid :uid5))
(def id6 (uuid :uid6))
(def id7 (uuid :uid7))
(def id8 (uuid :uid8))

;;
;; * Branches
;;

(def master {:db/uid master-uid})
(def child  {:db/uid child-uid, ::branch/parent master ::branch/branch-off-delta-uid id1 ::branch/start-deltas-uid id2})
(def gchild {:db/uid gchild-uid ::branch/parent child, ::branch/branch-off-delta-uid id3 ::branch/start-deltas-uid id4})

(def branches [master child gchild])

(s/assert* (s/coll-of ::branch/branch) branches)

;;
;; * Deltas
;;

(def d0 {:branch-uid master-uid, :uid id0 :prev-uid nil, :file-uid file1-uid, :op [:str/ins nil "h"],, :pad 0,,, :meta {:timestamp 1, :version 1.0}})
(def d1 {:branch-uid master-uid, :uid id1 :prev-uid id0, :file-uid file1-uid, :op [:crsr/mv id0],,,,,, :pad 1,,, :meta {:timestamp 1, :version 1.0}})
(def d2 {:branch-uid child-uid,, :uid id2 :prev-uid id1, :file-uid file1-uid, :op [:str/ins id0 "i"],, :pad 100, :meta {:timestamp 1, :version 1.0}})
(def d3 {:branch-uid child-uid,, :uid id3 :prev-uid id2, :file-uid file1-uid, :op [:crsr/mv id2],,,,,, :pad 1,,, :meta {:timestamp 1, :version 1.0}})
(def d4 {:branch-uid gchild-uid, :uid id4 :prev-uid id3, :file-uid file1-uid, :op [:str/ins id2 "!"],, :pad 100, :meta {:timestamp 1, :version 1.0}})
(def d5 {:branch-uid gchild-uid, :uid id5 :prev-uid id4, :file-uid file1-uid, :op [:crsr/mv id4],,,,,, :pad 1,,, :meta {:timestamp 1, :version 1.0}})
(def d6 {:branch-uid gchild-uid, :uid id6 :prev-uid id5, :file-uid file1-uid, :op [:str/rem id4 1],,,, :pad 1,,, :meta {:timestamp 1, :version 1.0}})
(def d7 {:branch-uid child-uid,, :uid id7 :prev-uid id3, :file-uid file1-uid, :op [:str/rem id2 1],,,, :pad 1,,, :meta {:timestamp 1, :version 1.0}})
(def d8 {:branch-uid master-uid, :uid id8 :prev-uid id1, :file-uid file1-uid, :op [:str/ins id0 "ey"], :pad 1,,, :meta {:timestamp 1, :version 1.0}})

(def deltas [d0 d1 d2 d3 d4 d5 d6 d7 d8])

(s/assert* (s/coll-of ::delta/delta) deltas)

;;
;; * Latest file deltas
;;

(def latest-file-deltas
  {master-uid {file1-uid d8}
   child-uid  {file1-uid d3 file2-uid d7}
   gchild-uid {file1-uid d5 file2-uid d6}})

;;
;; * Branches
;;

(def deltas-by-branch-uid
  {master-uid (indexed/vec-by :uid [d0 d1 d8])
   child-uid  (indexed/vec-by :uid [d2 d3 d7])
   gchild-uid (indexed/vec-by :uid [d4 d5 d6])})

(s/assert* ::state.branches/deltas-by-branch-uid deltas-by-branch-uid)

(def branch-pointers-by-branch-uid
  {master-uid {::state.branch-pointers/start (:uid d0) ::state.branch-pointers/end (:uid d8)}
   child-uid  {::state.branch-pointers/start (:uid d2) ::state.branch-pointers/end (:uid d7)}
   gchild-uid {::state.branch-pointers/start (:uid d4) ::state.branch-pointers/end (:uid d6)}})

(s/assert* ::state.branch-pointers/branch-pointers-by-branch-uid branch-pointers-by-branch-uid)

;;
;; * Branch tree
;;

(def branch-tree
  (assoc master ::branch/children [(assoc child ::branch/children [gchild])]))
