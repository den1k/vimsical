(ns vimsical.runner
  "Runner for cljs tests.

  Test namespaces have to be required in this file in order to run.

  Namespaces are filtered and their name will have to match the regex passed to
  `doo-all-tests`."
  (:require
   [cljs.test :as test]
   [doo.runner :refer-macros [doo-all-tests doo-tests]]
   [vimsical.common.core-test]
   [vimsical.common.test-test]
   [vimsical.common.util.transit-test]
   [vimsical.remotes.event-test]
   [vimsical.vcs.alg.topo-test]
   [vimsical.vcs.alg.traversal-test]
   [vimsical.vcs.branch-test]
   [vimsical.vcs.core-test]
   [vimsical.vcs.data.dll-test]
   [vimsical.vcs.data.gen.diff-test]
   [vimsical.vcs.data.indexed.vector-test]
   [vimsical.vcs.data.splittable-test]
   [vimsical.vcs.delta-test]
   [vimsical.vcs.state.branch-pointers-test]
   [vimsical.vcs.state.branches-test]
   [vimsical.vcs.state.chunks-test]
   [vimsical.vcs.state.files-test]
   [vimsical.vcs.state.timeline-test]
   [vimsical.vcs.sync-test]
   [vimsical.vcs.validation-test]))

;;
;; Filter namespaces that start with "vimsical" and end with "-test"
;;

(doo-all-tests #"vimsical.+\-test$")
