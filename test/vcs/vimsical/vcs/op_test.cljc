(ns vimsical.vcs.op-test
  (:require
   [clojure.spec :as s]
   [diffit.vec :as diffit]
   [vimsical.common.test :refer [uuid]]
   [vimsical.vcs.op :as op]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.edit-event :as edit-event]
   [clojure.test :as t :refer [deftest is are]]))


;; * Edit events
;; ** 1 edit-event -> * edit-events (flattening?)

(defmulti edit-event->edit-events ::edit-event/op)

(defmethod edit-event->edit-events :str/ins
  [{::edit-event/keys [op idx diff]}]
  (let [idxs  (range idx (+ idx (count diff)))
        chars (seq diff)]
    (mapv
     (fn [[idx char]]
       {::edit-event/op   op
        ::edit-event/idx  idx
        ::edit-event/diff (str char)})
     (map vector idxs chars))))

(defmethod edit-event->edit-events :str/rem
  [{::edit-event/keys [op idx amt] :as evt}]
  (mapv
   (fn [amt]
     {::edit-event/op   op
      ::edit-event/idx  (- idx amt)
      ::edit-event/amt 1})
   (range amt)))

(comment
  (vec
   (edit-event->edit-events
    {::edit-event/op :str/ins ::edit-event/idx 0 ::edit-event/diff "abc"}))
  [#:vimsical.vcs.edit-event{:op :str/ins, :idx 0, :diff "a"}
   #:vimsical.vcs.edit-event{:op :str/ins, :idx 1, :diff "b"}
   #:vimsical.vcs.edit-event{:op :str/ins, :idx 2, :diff "c"}]

  (vec
   (edit-event->edit-events
    {::edit-event/op :str/rem ::edit-event/idx 4 ::edit-event/amt 4}))
  [#:vimsical.vcs.edit-event{:op :str/rem, :idx 4, :amt 1}
   #:vimsical.vcs.edit-event{:op :str/rem, :idx 3, :amt 1}
   #:vimsical.vcs.edit-event{:op :str/rem, :idx 2, :amt 1}
   #:vimsical.vcs.edit-event{:op :str/rem, :idx 1, :amt 1}])


;; ** Cursor movements

(defmulti move ::edit-event/op)

(defmethod move :str/ins
  [{::edit-event/keys [idx] :as evt}]
  [evt {::edit-event/op :crsr/mv ::edit-event/idx (inc idx)}])

(defmethod move :str/rem
  [{::edit-event/keys [idx amt] :as evt}]
  [evt {::edit-event/op :crsr/mv ::edit-event/idx (- idx amt)}])

(defn interpose-moves
  [edit-events]
  (reduce
   (fn [acc to]
     (into acc (move to)))
   [] edit-events))


(interpose-moves
 [#:vimsical.vcs.edit-event{:op :str/ins, :idx 0, :diff "a"}
  #:vimsical.vcs.edit-event{:op :str/ins, :idx 1, :diff "b"}
  #:vimsical.vcs.edit-event{:op :str/ins, :idx 2, :diff "c"}
  #:vimsical.vcs.edit-event{:op :str/rem, :idx 2, :amt 2}])


;; * Diffit -> edit-event

(s/def ::ins (s/cat :op #{:+} :index pos-int? :chars vector?))
(s/def ::rem (s/cat :op #{:-} :index pos-int? :amt pos-int?))
(s/def ::edit (s/or :ins ::ins :rem ::rem))
(s/def ::edit-script (s/spec (s/* ::edit)))
(s/def ::diff (s/cat :edit-distance number? :edit-script ::edit-script ))


;; ** 1 diff -> 1 edit-event

(defmulti diffit-edit->edit-event first)

(defmethod diffit-edit->edit-event :+
  [[_ idx chars]]
  {::edit-event/op :str/ins ::edit-event/idx idx ::edit-event/diff (apply str chars)})

(defmethod diffit-edit->edit-event :-
  [[_ idx amt]]
  {::edit-event/op :str/rem ::edit-event/idx idx ::edit-event/amt amt})

(defn edit-script->edit-events
  "Retun a seq of edit-events for an edit script. Single events may contain
  insertions and deltions for multiple characters."
  [[_ edits]]
  (into [] (map diffit-edit->edit-event) edits))

(comment
  (edit-script->edit-events
   (diffit/diff "ac" "caaaab"))
  [#:vimsical.vcs.edit-event{:op :str/ins, :idx 0, :diff "c"}
   #:vimsical.vcs.edit-event{:op :str/ins, :idx 2, :diff "aaab"}
   #:vimsical.vcs.edit-event{:op :str/rem, :idx 6, :amt 1}])

;; ** 1 diff -> * edit-events

(defmulti diffit-edit->edit-events first)

(defmethod diffit-edit->edit-events :+
  [[op idx chars]]
  (::events
   (reduce
    (fn [{::keys [idx] :as acc} char]
      (-> acc
          (update ::idx inc)
          (update ::events conj (diffit-edit->edit-event [op idx [char]]))))
    {::idx idx ::events []} chars)))

(defmethod diffit-edit->edit-events :-
  [[op idx amt]]
  (::events
   (reduce
    (fn [{::keys [idx] :as acc} _]
      (-> acc
          (update ::idx inc)
          (update ::events conj (diffit-edit->edit-event [op idx 1]))))
    {::idx idx ::events []} (range amt))))

(defn edit-script->all-edit-events
  [[_ edits]]
  (into [] (comp (map diffit-edit->edit-events) cat) edits))

(comment
  (edit-script->all-edit-events
   (diffit/diff "ac" "caaaab"))
  [#:vimsical.vcs.edit-event{:op :str/ins, :idx 0, :diff "c"}
   #:vimsical.vcs.edit-event{:op :str/ins, :idx 2, :diff "a"}
   #:vimsical.vcs.edit-event{:op :str/ins, :idx 3, :diff "a"}
   #:vimsical.vcs.edit-event{:op :str/ins, :idx 4, :diff "a"}
   #:vimsical.vcs.edit-event{:op :str/ins, :idx 5, :diff "b"}
   #:vimsical.vcs.edit-event{:op :str/rem, :idx 6, :amt 1}])

;; * str -> edit events

(defn str->edit-events
  "Return the :str/ins edit events for `s`."
  [s]
  (interpose-moves
   (::edit-events
    (reduce
     (fn [{::keys [s edit-events]} char]
       (let [s'           (str s char)
             edit-script  (diffit/diff s s')
             edit-events' (edit-script->edit-events edit-script)]
         {::s s' ::edit-events (into edit-events edit-events')}))
     {::s "" ::edit-events []} s))))

(comment
  (str->edit-events
   "function sum (a, b) { return a + b };"))

(defn str-iterations
  [s]
  (vec
   (transduce
    (comp
     (take-while seq)
     (map (partial apply str)))
    conj
    (list)
    (iterate butlast s))))

(comment
  (str-iterations "abcde")
  ("a" "ab" "abc" "abcd" "abcde"))


(defn strs->edit-events
  [& [s & strs]]
  (interpose-moves
   (::edit-events
    (reduce
     (fn [{::keys [s edit-events]} s']
       (let [
             edit-script  (diffit/diff s s')
             edit-events' (edit-script->edit-events edit-script)]
         {::s s' ::edit-events (into edit-events edit-events')}))
     {::s "" ::edit-events []} (into (str-iterations s) strs)))))

(strs->edit-events
 "function sum (a, b) { return a + b };"
 "function add (a, b) { return a + b };")

;; * single edit event -> deltas
;; TODO intergrate with file state

(require '[vimsical.vcs.state.vims.files.delta :as files.delta])

(defn edit-event->delta-id
  [file-state currrent-delta-id file-id {::edit-event/keys [idx] :as edit-event}]
  (let [cache (get-in file-state [currrent-delta-id file-id])]
    (files.delta/op-idx->op-id cache idx)))

(s/fdef edit-event->deltas
        :args any?
        :ret  (s/every ::delta/op))

(defmulti edit-event->deltas
  (fn [branch-id
       file-id
       current-delta-id
       pad-fn
       uuid-fn
       timestamp-fn
       file-state
       edit-event]
    (::edit-event/op edit-event)))

(defmethod edit-event->deltas :str/ins
  [branch-id
   file-id
   current-delta-id
   pad-fn
   uuid-fn
   timestamp-fn
   file-state
   {::edit-event/keys [diff] :as edit-event}]
  (reduce
   (fn edit-event->deltas-str-in
     [{::keys [current-delta-id file-state edit-event]} diff]
     (let [op-id     (edit-event->delta-id file-state current-delta-id file-id edit-event)
           delta-id  (uuid-fn edit-event)
           op        [:str/ins op-id diff]
           pad       (pad-fn edit-event)
           timestamp (timestamp-fn edit-event)]
       {::current-delta-id delta-id
        ::edit-event       (update edit-event ::edit-event/idx inc)
        ::file-state       (files.delta/add-delta
                            file-state
                            (delta/new-delta
                             {:branch-id branch-id
                              :file-id   file-id
                              :prev-id   current-delta-id
                              :id        delta-id
                              :op        op
                              :pad       pad
                              :timestamp timestamp}))}))
   {::current-delta-id current-delta-id
    ::edit-event       edit-event
    ::file-state       file-state}
   (map str diff)))

(comment
  (edit-event->deltas
   (uuid :branch)
   (uuid :file)
   nil
   (fn pad [_] 1)
   (fn uuid-gen [{::edit-event/keys [op idx]}] (uuid [op idx]))
   (fn ts [_] 123)
   (files.delta/new-delta-index)
   #:vimsical.vcs.edit-event{:op :str/ins, :idx 0, :diff ""}))

(defmethod edit-event->deltas :str/rem
  [branch-id
   file-id
   current-delta-id
   pad-fn
   uuid-fn
   timestamp-fn
   file-state
   {::edit-event/keys [amt] :as edit-event}]
  (reduce
   (fn edit-event->deltas-str-rem
     [{::keys [current-delta-id file-state edit-event]} _]
     (let [op-id    (edit-event->delta-id file-state current-delta-id file-id edit-event)
           delta-id (uuid-fn edit-event)
           op       [:str/rem op-id]]
       {::current-delta-id delta-id
        ::edit-event       (update edit-event ::edit-event/idx dec)
        ::file-state       (files.delta/add-delta
                            file-state
                            (delta/new-delta
                             {:branch-id branch-id
                              :file-id   file-id
                              :prev-id   current-delta-id
                              :id        delta-id
                              :op        op
                              :pad       (pad-fn edit-event)
                              :timestamp (timestamp-fn edit-event)}))}))
   {::current-delta-id current-delta-id
    ::edit-event       edit-event
    ::file-state       file-state}
   (range amt)))

(let [{::keys [current-delta-id file-state] :as state}
      (edit-event->deltas
       (uuid :branch)
       (uuid :file)
       nil
       (fn pad [_] 1)
       (fn uuid-gen [{::edit-event/keys [op idx]}] (uuid [op idx]))
       (fn ts [_] 123)
       (files.delta/new-delta-index)
       #:vimsical.vcs.edit-event{:op :str/ins, :idx 0, :diff "abd"})]
  (assert current-delta-id)
  (assert file-state)
  (edit-event->deltas
   (uuid :branch)
   (uuid :file)
   current-delta-id
   (fn pad [_] 1)
   (fn uuid-gen [{::edit-event/keys [op idx]}] (uuid [op idx]))
   (fn ts [_] 123)
   file-state
   #:vimsical.vcs.edit-event{:op :str/rem, :idx 3, :amt 2}))

(defmethod edit-event->deltas :crsr/mv
  [branch-id
   file-id
   current-delta-id
   pad-fn
   uuid-fn
   timestamp-fn
   file-state
   {::edit-event/keys [amt] :as edit-event}]
  (let [id       (edit-event->delta-id file-state current-delta-id file-id edit-event)
        delta-id (uuid-fn edit-event)
        op       [:crsr/mv id amt]]
    [(delta/new-delta
      {:branch-id branch-id
       :file-id   file-id
       :prev-id   id
       :id        delta-id
       :op        op
       :pad       (pad-fn edit-event)
       :timestamp (timestamp-fn edit-event)})]))

(comment
  (edit-event->deltas
   (uuid :branch)
   (uuid :file)
   nil
   (fn pad [_] 1)
   (fn uuid-gen [{::edit-event/keys [op idx]}] (uuid [op idx]))
   (fn ts [_] 123)
   []
   #:vimsical.vcs.edit-event{:op :str/ins, :idx 0, :diff "abc"}))

(defn edit-events->deltas
  [branch-id
   file-id
   current-delta-id
   pad-fn
   uuid-fn
   timestamp-fn
   file-state
   edit-events]
  (reduce
   (fn [{::keys [current-delta-id file-state]} edit-event]
     (let [delta-id (uuid-fn edit-event)
           delta    (edit-event->deltas
                     branch-id file-id current-delta-id
                     pad-fn uuid-fn timestamp-fn
                     file-state edit-event)]
       {::current-delta-id delta-id
        ::file-state       (files.delta/add-delta file-state delta)}))
   {::current-delta-id current-delta-id
    ::file-state       (files.delta/new-delta-index)} edit-events))


(comment
  (edit-events->deltas
   (uuid :branch)
   (uuid :file)
   nil
   (fn pad [_] 1)
   (fn uuid-gen [{::edit-event/keys [op idx]}] (uuid [op idx]))
   (fn ts [_] 123)
   []
   [#:vimsical.vcs.edit-event{:op :str/ins, :idx 0, :diff "s"}
    #:vimsical.vcs.edit-event{:op :str/ins, :idx 1, :diff "t"}
    #:vimsical.vcs.edit-event{:op :str/ins, :idx 2, :diff "r"}
    #:vimsical.vcs.edit-event{:op :str/ins, :idx 3, :diff "i"}
    #:vimsical.vcs.edit-event{:op :str/ins, :idx 4, :diff "n"}
    #:vimsical.vcs.edit-event{:op :str/ins, :idx 5, :diff "g"}])


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
  ;;                          :file-id #uuid :file,
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
