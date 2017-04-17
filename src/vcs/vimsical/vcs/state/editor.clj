(ns vimsical.vcs.state.editor
  "TODO

  - enforce branching
  can't allow to branch-off, client needs to pass a new branch or we throw."
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.edit-event :as edit-event]
   [vimsical.vcs.state.files :as files]))


;; * Spec

;; State required for the editor to add events to the vcs

(s/def ::files-states ::files/states)
(s/def ::deltas (s/every ::delta/delta))
(s/def ::state (s/keys :req [::files-states ::deltas ::delta-id ::file-id ::branch-id]))
(s/def ::effects (s/keys :req [::uuid-fn ::pad-fn ::timestamp-fn]))

(s/def ::delta-id ::delta/prev-id)
(s/def ::file-id ::file/id)
(s/def ::branch-id ::branch/id)
;; Fns of the edit event for now
(s/def ::uuid-fn ifn?)
(s/def ::pad-fn ifn?)
(s/def ::timestamp-fn ifn?)


;; * Position mapping

;; TODO replace this
;;
;; If we keep track of the current position event through mv events,

(defn edit-event->delta-id
  [files-states delta-id file-id {::edit-event/keys [idx] :as edit-event}]
  (when-let [cache (get-in files-states [delta-id file-id])]
    (files/op-idx->op-id cache idx)))


;; * Edit event -> deltas

(s/def ::update-state (s/keys :req [::delta-id ::files-states ::deltas]))

;; ** Splicing: 1 edit-event -> * edit-events

;; We don't want to always have that because the vcs should splice deltas for
;; macro events, but in some test cases it is useful to generate every single
;; insert for a given diff

(defmulti splice-edit-event ::edit-event/op)

(defmethod splice-edit-event :default [e] e)

(defmethod splice-edit-event :str/ins
  [{::edit-event/keys [op idx diff]}]
  (let [idxs  (range idx (+ idx (count diff)))
        chars (seq diff)]
    (mapv
     (fn [[idx char]]
       {::edit-event/op   op
        ::edit-event/idx  idx
        ::edit-event/diff (str char)})
     (map vector idxs chars))))

(defmethod splice-edit-event :str/rem
  [{::edit-event/keys [op idx amt] :as evt}]
  (mapv
   (fn [amt]
     {::edit-event/op   op
      ::edit-event/idx (max 0 (- idx amt))
      ::edit-event/amt 1})
   (range amt)))


(s/fdef edit-event->deltas
        :args (s/cat :state ::state
                     :editor-effects ::effects
                     :edit-event ::edit-event/edit-event)
        :ret  ::update-state)

(defmulti edit-event->deltas
  (fn [state effects edit-event]
    (-> edit-event ::edit-event/op)))

(defmethod edit-event->deltas :str/ins
  [{:as state ::keys [files-states branch-id file-id delta-id]}
   {:as effects ::keys [pad-fn uuid-fn timestamp-fn]}
   edit-event]
  (reduce
   (fn edit-event->deltas-str-ins
     [{:as state ::keys [files-states delta-id]}
      {:as edit-event ::edit-event/keys [diff]}]
     (let [op-id     (edit-event->delta-id files-states delta-id file-id edit-event)
           op        [:str/ins op-id diff]
           delta-id' (uuid-fn edit-event)
           pad       (pad-fn edit-event)
           timestamp (timestamp-fn edit-event)
           delta     (delta/new-delta
                      {:branch-id branch-id
                       :file-id   file-id
                       :prev-id   delta-id
                       :id        delta-id'
                       :op        op
                       :pad       pad
                       :timestamp timestamp})]
       (-> state
           (assoc ::delta-id delta-id')
           (update ::files-states files/add-delta delta)
           (update ::deltas conj delta))))
   state (splice-edit-event edit-event)))

(defmethod edit-event->deltas :str/rem
  [{:as state ::keys [files-states branch-id file-id delta-id]}
   {:as effects ::keys [pad-fn uuid-fn timestamp-fn]}
   edit-event]
  (reduce
   (fn edit-event->deltas-str-rem
     [{::keys [deltas delta-id files-states]}
      {:as edit-event ::edit-event/keys [amt]}]
     (let [op-id     (edit-event->delta-id files-states delta-id file-id edit-event)
           op        [:str/rem op-id amt]
           delta-id' (uuid-fn edit-event)
           pad       (pad-fn edit-event)
           timestamp (timestamp-fn edit-event)
           delta     (delta/new-delta
                      {:branch-id branch-id
                       :file-id   file-id
                       :prev-id   delta-id
                       :id        delta-id'
                       :op        op
                       :pad       pad
                       :timestamp timestamp})]
       (-> state
           (assoc ::delta-id delta-id')
           (update ::files-states files/add-delta delta)
           (update ::deltas conj delta))))
   state (splice-edit-event edit-event)))

(defmethod edit-event->deltas :crsr/mv
  [{:as state ::keys [files-states branch-id file-id delta-id]}
   {:as effects ::keys [pad-fn uuid-fn timestamp-fn]}
   edit-event]
  (let [op-id     (edit-event->delta-id files-states delta-id file-id edit-event)
        delta-id' (uuid-fn edit-event)
        op        [:crsr/mv op-id]
        delta     (delta/new-delta
                   {:branch-id branch-id
                    :file-id   file-id
                    :prev-id   op-id
                    :id        delta-id'
                    :op        op
                    :pad       (pad-fn edit-event)
                    :timestamp (timestamp-fn edit-event)})]
    (-> state
        (assoc ::delta-id delta-id)
        (update ::files-states files/add-delta delta)
        (update ::deltas conj delta))))

(defn edit-events->deltas
  [files-states
   {:as state ::keys [delta-id]}
   {:as effects ::keys [pad-fn uuid-fn timestamp-fn]}
   edit-events]
  (let [acc-opts {::files-states files-states
                  ::deltas       []}
        acc (merge state acc-opts)]
    (reduce
     ;; (edit-event->deltas files-states state' effects edit-event)
     (fn [update-state edit-event]
       (edit-event->deltas update-state effects edit-event))
     acc edit-events)))
