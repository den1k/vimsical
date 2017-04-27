(ns vimsical.vcs.bench)


;; (require '[criterium.core :refer [quick-bench]])
;; (require '[clojure.core.rrb-vector :as rrb])
;; ;;
;; ;; Vector insert RRB vs. PersistentVector
;; ;;
;; (let [n    1e5
;;       coll (range n)
;;       v    (vec coll)
;;       r    (rrb/vec coll)]
;;   (letfn [(insert [v v-insert subvec-fn cat-fn]
;;             (let [split (rand-int n)
;;                   l     (subvec-fn v 0 split)
;;                   r     (subvec-fn v split n)]
;;               (cat-fn l v-insert r)))]
;;     (println "RRB cat")
;;     ;; Execution time mean : 8.705160 µs
;;     (quick-bench
;;      (insert r (rrb/vec [:foo]) rrb/subvec rrb/catvec))


;;     (println "Vector concatvec")
;;     ;; Execution time mean : 15.160789 ms
;;     (quick-bench
;;      (insert v [:foo] subvec (comp vec concat)))


;;     (println "Vector into")
;;     ;; Execution time mean : 5.847643 ms
;;     (quick-bench
;;      (insert v [:foo] subvec (fn [l m r] (into (into l m) r))))))
