(ns vimsical.vcs.data.gen.diff
  "This ns provides the building blocks for programatically generating edit
  events by diffing strings."
  (:require
   [clojure.spec :as s]
   [diffit.vec :as diffit]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.core :as vcs]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.edit-event :as edit-event]
   [vimsical.vcs.editor :as editor]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.state.files :as state.files]))

;;
;; * Edit events
;;

;;
;; ** Cursor
;;

;;
;; *** Moving after str events
;;

;; Add the crsr/mv events after str/* edit-events
;;

(s/fdef move-past-edit-event
        :args (s/cat :e ::edit-event/edit-event)
        :ret  (s/every ::edit-event/edit-event))

(defmulti ^:private move-past-edit-event ::edit-event/op)

(defmethod move-past-edit-event :str/ins
  [{::edit-event/keys [idx diff] :as evt}]
  [evt {::edit-event/op :crsr/mv ::edit-event/idx (+ (count diff) idx)}])

(defmethod move-past-edit-event :str/rem
  [{::edit-event/keys [idx amt] :as evt}]
  (let [idx (max 0 (- idx amt))]
    (cond-> [evt]
      (pos? idx) (conj {::edit-event/op :crsr/mv ::edit-event/idx idx}))))

(defn- move-past-edit-events-xf
  []
  (comp (map move-past-edit-event) cat))


;;
;; *** Moving between diff events
;;

;; When the diff generates, say an insert at X and a delete at Y, we want to add
;; the cursor movements going from (+ X 1) do Y. There is some complexity in the
;; implementation because these fns usually kick in after we've added the
;; `move-past-edit-event`

(defmulti ^:private post-event-index
  "Return what should be the index after the event has been applied. Say if we
  insert 3 chars at pos n then the post-event-index is n+3"
  ::edit-event/op)

(defmethod post-event-index :str/ins
  [{::edit-event/keys [idx diff]}]
  (+ idx (count diff)))

(defmethod post-event-index :str/rem
  [{::edit-event/keys [idx amt]}]
  (max 0 (- idx amt)))

(defmethod post-event-index :crsr/mv
  [{::edit-event/keys [idx]}]
  idx)

(defn- move-to-next-event
  [[{:as left idx1 ::edit-event/idx}
    {:as right idx2 ::edit-event/idx op-right ::edit-event/op}]]
  (letfn [(drange [from to]
            (if (< to from)
              (reverse (range to from))
              (range (inc from) (inc to))))]
    (if (nil? right)
      [left]
      (let [distance (- idx2 (post-event-index left))
            init     [left]
            idxs     (if-not (zero? distance) (drange idx1 idx2))]
        (->> idxs
             (mapv (fn [idx] {::edit-event/op :crsr/mv ::edit-event/idx idx}))
             (into init))))))

(defn- move-to-next-events
  [events]
  (mapcat move-to-next-event (partition-all 2 1 events)))

(comment
  ;; Can't get the xf version to work
  (defn partition-events-xf
    []
    (let [prev-input (volatile! nil)
          cnt        (volatile! -1)]
      (fn [rf]
        (fn
          ([] (rf))
          ([result]
           (if (or (== 1 @cnt) (even? @cnt))
             (rf (rf result [@prev-input nil]))
             (rf result)))
          ([result input]
           (if (pos? (vswap! cnt inc))
             (rf result [@prev-input (vreset! prev-input input)])
             (do (vreset! prev-input input) result)))))))
  (defn move-to-next-events-xf []
    (comp
     (partition-events-xf)
     (map  move-to-next-event)
     cat)))


(defn- splice-edit-events-xf [] (comp (map #'state.files/splice-edit-event) cat))


;;
;; * String diff to edit events
;;

;;
;; ** str -> diff
;;

(s/def ::ins (s/cat :op #{:+} :index nat-int? :chars vector?))
(s/def ::rem (s/cat :op #{:-} :index nat-int? :amt pos-int?))
(s/def ::edit (s/or :ins ::ins :rem ::rem))
(s/def ::edit-script (s/spec (s/* ::edit)))
(s/def ::diff (s/cat :edit-distance number? :edit-script ::edit-script ))

;;
;; ** diff -> edit-event
;;

(defmulti ^:private diffit-edit->edit-event first)

(defmethod diffit-edit->edit-event :+
  [[_ idx chars]]
  {::edit-event/op :str/ins ::edit-event/idx idx ::edit-event/diff (apply str chars)})

(defmethod diffit-edit->edit-event :-
  [[_ idx amt]]
  {::edit-event/op :str/rem ::edit-event/idx idx ::edit-event/amt amt})

(defn- edit-script->edit-events
  "Retun a seq of edit-events for an edit script. Single events may contain
  insertions and deltions for multiple characters."
  [[_ edits]]
  (into [] (map diffit-edit->edit-event) edits))

;;
;; * API
;;

(s/def ::splice-string (s/tuple string?))
(s/def ::string string?)
(s/def ::splice-or-string (s/or :splice ::splice-string :s ::string))

(def ^:private diff->edit-events (comp edit-script->edit-events diffit/diff))

(s/fdef diffs->edit-events
        :args (s/every ::splice-or-string)
        :ret  (s/every ::edit-event/edit-event))

(defn diffs->edit-events
  "Take a list of string as varargs and generates a sequence of edit-events with
  corresponding cursor movements. Each string can be wrapped in a vector which
  will cause its diff's edit-events to be spliced into the final seq.

  For example:

  (diffs->edit-events
  \"\"
  \"function sum (a, b) { return a + b } ;\")

  Will return a single :str/ins event with the second string as a diff.

  (diffs->edit-events
  \"\"
  [\"function sum (a, b) { return a + b };\"]
  \"function add (a, b) { return a + b };\")

  Will return one str/ins event per character in the first string, and two events
  for the second one: a str/rem with an :amt of 3, and a str/ins with \"add\"
  "
  [& splices-and-strs]
  (letfn [(splice-xf [splice-or-str]
            (if (vector? splice-or-str)
              (splice-edit-events-xf)
              (map identity)))]
    (vec
     (move-to-next-events
      (::edit-events
       (reduce
        (fn [{::keys [s edit-events] :as acc} splice-or-str]
          (let [s' (if (vector? splice-or-str) (first splice-or-str) splice-or-str)
                xf (comp
                    (splice-xf splice-or-str)
                    (move-past-edit-events-xf))]
            (if (nil? acc)
              {::s s' ::edit-events []}
              {::s s' ::edit-events
               (transduce  xf conj edit-events (diff->edit-events s s'))})))
        nil splices-and-strs))))))

(s/fdef diffs->vcs
        :args (s/cat :vcs ::vcs/vcs
                     :effects ::editor/effects
                     :file-uid ::file/uid
                     :branch-uid ::branch/uid
                     :delta-uid ::delta/prev-uid
                     :strs (s/* ::splice-or-string))
        :ret (s/tuple ::vcs/vcs ::delta/uid))

(defn diffs->vcs
  [vcs effects file-uuid branch-uid delta-uid & strs]
  (assert (string? (first strs)))
  (assert (every? vector? (next strs)))
  (letfn [(splice-strs [strs] (mapv (fn [str] (if (vector? str) str [str])) strs))]
    (reduce
     (fn [[vcs delta-uid] edit-event]
       (vcs/add-edit-event vcs effects file-uuid branch-uid delta-uid edit-event))
     [vcs delta-uid] (apply diffs->edit-events (splice-strs strs)))))
