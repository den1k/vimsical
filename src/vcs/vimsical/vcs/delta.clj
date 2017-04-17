(ns vimsical.vcs.delta
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.op :as op]))

;; * Spec

(def current-version 0.3)

(s/def ::id uuid?)
(s/def ::prev-id (s/nilable ::id))
(s/def ::pad nat-int?)
(s/def ::branch-id uuid?)
(s/def ::file-id uuid?)
(s/def ::version number?)
(s/def ::timestamp nat-int?)
(s/def ::meta (s/keys :req-un [::timestamp ::version]))

(s/def ::delta
  (s/keys :req-un [::branch-id ::file-id ::prev-id ::id ::op/op ::pad]
          :opt-un [::meta]))

(s/def ::new-delta
  (s/keys :req-un [::branch-id ::file-id ::prev-id ::id ::op/op ::pad ::timestamp]))

(s/fdef new-delta
        :args (s/cat :new-delta ::new-delta)
        :ret  ::delta)

(defn new-delta
  [{:keys [branch-id file-id prev-id id op pad timestamp]}]
  {:branch-id branch-id
   :file-id   file-id
   :prev-id   prev-id
   :id        id
   :op        op
   :pad       pad
   :meta      {:timestamp timestamp :version current-version}})


(defn op-id [{[_ id] :op}] id)
(defn op-type [{[op] :op}] (assert op) op)
(defn op-diff [{[_ _ diff] :op}] (assert diff "No diff on this delta") diff)
(defn op-amt  [{[_ _ amt] :op}] (assert amt "No amt on this delta") amt)
