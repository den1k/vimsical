(ns vimsical.vcs.data.editable)

(defprotocol IEditable
  (insert [_ idx x])
  (remove [_ idx amt]))

;; Compare:
;; String vs rope
;; Vec vs rrb
