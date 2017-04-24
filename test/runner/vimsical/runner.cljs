(ns vimsical.runner
  "Runner for cljs tests.

  Test namespaces have to be required in this file in order to run.

  Namespaces are filtered and their name will have to match the regex passed to
  `doo-all-tests`."
  (:require
   [cljs.test :as test]
   [doo.runner :refer-macros [doo-all-tests doo-tests]]
   [vimsical.common.core-test]
   [vimsical.frontend.core-test]
   ;; TODO cljc conversion
   ;; [vimsical.vcs.core-test]
   ))

;;
;; Filter namespaces that start with "vimsical" and end with "-test"
;;

(doo-all-tests #"vimsical.+\-test$")
