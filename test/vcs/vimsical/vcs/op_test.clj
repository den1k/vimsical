(ns vimsical.vcs.op-test
  (:require
   [clojure.spec :as s]
   [diffit.vec :as diffit]
   [vimsical.common.test :refer [uuid]]
   [vimsical.vcs.op :as op]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.edit-event :as edit-event]
   [clojure.test :as t :refer [deftest is are]]))


;; * single edit event -> deltas
;; TODO intergrate with file state

(require '[vimsical.vcs.state.vims.files.delta :as files.delta])



;; * str -> deltas

(defn str->deltas
  [branch-id file-id current-delta-id pad-fn uuid-fn timestamp-fn file-state s]
  (->> s
       (str->edit-events)
       (edit-events->deltas branch-id file-id current-delta-id pad-fn uuid-fn timestamp-fn file-state)))

(comment
  (str->deltas
   (uuid :branch)
   (uuid :file)
   nil
   (fn pad [_] 1)
   (fn uuid-gen [{::edit-event/keys [op idx]}] (uuid [op idx]))
   (fn ts [_] 123)
   []
   "string")

  ;; #:vimsical.vcs.op-test{:current-delta-id #uuid [:str/ins 5],
  ;;                        :deltas
  ;;                        [{:id #uuid [:str/ins 0],
  ;;                          :prev-id nil,
  ;;                          :pad 1,
  ;;                          :file-id #uuid :file,
  ;;                          :branch-id #uuid :branch,
  ;;                          :meta {:timestamp 123, :version 0.3}}
  ;;                         {:id #uuid [:str/ins 1],
  ;;                          :prev-id nil,
  ;;                          :pad 1,
  ppp  ;;                          :file-id #uuid :file,
  ;;                          :branch-id #uuid :branch,
  ;;                          :meta {:timestamp 123, :version 0.3}}
  ;;                         {:id #uuid [:str/ins 2],
  ;;                          :prev-id nil,
  ;;                          :pad 1,
  ;;                          :file-id #uuid :file,
  ;;                          :branch-id #uuid :branch,
  ;;                          :meta {:timestamp 123, :version 0.3}}
  ;;                         {:id #uuid [:str/ins 3],
  ;;                          :prev-id nil,
  ;;                          :pad 1,
  ;;                          :file-id #uuid :file,
  ;;                          :branch-id #uuid :branch,
  ;;                          :meta {:timestamp 123, :version 0.3}}
  ;;                         {:id #uuid [:str/ins 4],
  ;;                          :prev-id nil,
  ;;                          :pad 1,
  ;;                          :file-id #uuid :file,
  ;;                          :branch-id #uuid :branch,
  ;;                          :meta {:timestamp 123, :version 0.3}}
  ;;                         {:id #uuid [:str/ins 5],
  ;;                          :prev-id nil,
  ;;                          :pad 1,
  ;;                          :file-id #uuid :file,
  ;;                          :branch-id #uuid :branch,
  ;;                          :meta {:timestamp 123, :version 0.3}}]}
  )


;; * strs -> deltas

(defn strs->deltas
  [branch-id file-id pad-fn uuid-fn timestamp-fn & strs]
  (letfn [(str-diff->deltas [{::keys [current-delta-id file-state] :as acc} str-a str-b]
            (let [edit-script (diffit/diff str-a str-b)
                  edit-events (edit-script->edit-events edit-script)]
              (edit-events->deltas branch-id file-id current-delta-id pad-fn uuid-fn timestamp-fn file-state edit-events)))]
    (reduce
     (fn [{::keys [s file-state current-delta-id] :as acc} s']
       (when acc
         (assert (string? s))
         (assert (some? file-state))
         (assert (some? current-delta-id)))
       (if (nil? acc)
         (-> (str->deltas branch-id file-id current-delta-id pad-fn uuid-fn timestamp-fn file-state s')
             (assoc ::s s'))
         (-> (str-diff->deltas acc s s')
             (assoc ::s s'))))
     nil strs)))

(comment
  (strs->deltas
   (uuid :branch)
   (uuid :file)
   (fn pad [_] 1)
   (fn uuid-gen [{::edit-event/keys [op idx]}] (uuid [op idx]))
   (fn ts [_] 123)
   "abc" "zxy"))

;; (strs->deltas
;;  (uuid :branch)
;;  (uuid :file)
;;  (fn pad [_] 1)
;;  (fn uuid-gen [{::edit-event/keys [op idx]}] (uuid [op idx]))
;;  (fn ts [_] 123)
;;  "function sum (a, b) { return a + b };"
;;  "function add (a, b) { return a + b };")
