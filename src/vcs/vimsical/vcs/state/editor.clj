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

(s/def ::state (s/keys :req [::delta-id ::file-id ::branch-id]))
(s/def ::effects (s/keys :req [::uuid-fn ::pad-fn ::timestamp-fn]))

(s/def ::delta-id ::delta/prev-id)
(s/def ::file-id ::file/id)
(s/def ::branch-id ::branch/id)
;; Fns of the edit event for now
(s/def ::uuid-fn ifn?)
(s/def ::pad-fn ifn?)
(s/def ::timestamp-fn ifn?)

;; * Position mapping

(defn edit-event->delta-id
  [files-states delta-id file-id {::edit-event/keys [idx] :as edit-event}]
  (when-let [cache (get-in files-states [delta-id file-id])]
    (files/op-idx->op-id cache idx)))


;; * Edit event -> deltas

(s/def ::edit-event (s/nilable ::edit-event/edit-event))
(s/def ::files-states ::files/states)
(s/def ::deltas (s/every ::delta/delta))
(s/def ::update-state (s/keys :req [::delta-id ::edit-event]))

(s/fdef edit-event->deltas
        :args (s/cat :files-state ::files/states
                     :editor-state ::state
                     :editor-effects ::effects
                     :edit-event ::edit-event/edit-event)
        :ret  ::update-state)

(defmulti edit-event->deltas
  (fn [files-states state effects edit-event]
    (-> edit-event ::edit-event/op)))

(defmethod edit-event->deltas :str/ins
  [files-states
   {:as state ::keys [branch-id file-id delta-id]}
   {:as effects ::keys [pad-fn uuid-fn timestamp-fn]}
   {:as edit-event ::edit-event/keys [diff]}]
  (reduce
   (fn edit-event->deltas-str-ins
     [{:as acc ::keys [delta-id files-states edit-event deltas]} diff]
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
       {::delta-id     delta-id'
        ::edit-event   (update edit-event ::edit-event/idx inc)
        ::files-states (files/add-delta files-states delta)
        ::deltas       (conj deltas delta)}))
   {::delta-id     delta-id
    ::edit-event   edit-event
    ::files-states files-states}
   (map str diff)))

(comment
  (edit-event->deltas
   (uuid :branch)
   (uuid :file)
   nil
   (fn pad [_] 1)
   (fn uuid-gen [{::edit-event/keys [op idx]}] (uuid [op idx]))
   (fn ts [_] 123)
   (new-delta-index)
   #:vimsical.vcs.edit-event{:op :str/ins, :idx 0, :diff ""}))

(defmethod edit-event->deltas :str/rem
  [files-states
   {:as state ::keys [branch-id file-id delta-id]}
   {:as effects ::keys [pad-fn uuid-fn timestamp-fn]}
   {:as edit-event ::edit-event/keys [amt]}]
  (reduce
   (fn edit-event->deltas-str-rem
     [{::keys [deltas delta-id files-states edit-event]} _]
     (let [op-id     (edit-event->delta-id files-states delta-id file-id edit-event)
           op        [:str/rem op-id]
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
       {::delta-id     delta-id'
        ::edit-event   (update edit-event ::edit-event/idx dec)
        ::files-states (files/add-delta files-states delta)
        ::deltas       (conj deltas delta)}))
   {::delta-id     delta-id
    ::edit-event   edit-event
    ::files-states files-states
    ::deltas       []}
   (range amt)))

;; (let [{::keys [delta-id files-states] :as state}
;;       (edit-event->deltas
;;        (uuid :branch)
;;        (uuid :file)
;;        nil
;;        (fn pad [_] 1)
;;        (fn uuid-gen [{::edit-event/keys [op idx]}] (uuid [op idx]))
;;        (fn ts [_] 123)
;;        (new-delta-index)
;;        #:vimsical.vcs.edit-event{:op :str/ins, :idx 0, :diff "abd"})]
;;   (assert delta-id)
;;   (assert files-states)
;;   (edit-event->deltas
;;    (uuid :branch)
;;    (uuid :file)
;;    delta-id
;;    (fn pad [_] 1)
;;    (fn uuid-gen [{::edit-event/keys [op idx]}] (uuid [op idx]))
;;    (fn ts [_] 123)
;;    files-states
;;    #:vimsical.vcs.edit-event{:op :str/rem, :idx 3, :amt 2}))

(defmethod edit-event->deltas :crsr/mv
  [files-states
   {:as state ::keys [deltas branch-id file-id delta-id]}
   {:as effects ::keys [pad-fn uuid-fn timestamp-fn]}
   {:as edit-event ::edit-event/keys [amt]}]
  (let [op-id     (edit-event->delta-id files-states delta-id file-id edit-event)
        delta-id' (uuid-fn edit-event)
        op        [:crsr/mv op-id amt]
        delta     (delta/new-delta
                   {:branch-id branch-id
                    :file-id   file-id
                    :prev-id   op-id
                    :id        delta-id'
                    :op        op
                    :pad       (pad-fn edit-event)
                    :timestamp (timestamp-fn edit-event)})]

    {::delta-id     delta-id'
     ::edit-event   edit-event
     ::files-states (files/add-delta files-states delta)
     ::deltas       (conj deltas delta)}))

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
  [files-states
   {:as state ::keys [delta-id]}
   {:as effects ::keys [pad-fn uuid-fn timestamp-fn]}
   edit-events]
  (reduce
   (fn [{::keys [delta-id files-states]} edit-event]
     (let [state' (assoc state ::delta-id delta-id)]
       (edit-event->deltas files-states state' effects edit-event)))
   {::delta-id     delta-id
    ::files-states files-states
    ::deltas       []} edit-events))

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
