(ns vimsical.vcs.snapshot
  "NOTE delta-uid should be nil for now because we want to keep a single
  snapshot per file. Later we can fill in the current delta-id to keep multiple
  versions."
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.file :as file]
   [vimsical.common.util.core :as util]))

(s/def ::user-uid uuid?)
(s/def ::vims-uid uuid?)
(s/def ::file-uid ::file/uid)
(s/def ::type ::file/type)
(s/def ::sub-type ::file/sub-type)
(s/def ::delta-uid (s/nilable ::delta/uid))
(s/def ::text string?)

(s/def ::snapshot
  (s/keys :req [::user-uid ::vims-uid ::file-uid ::text]))

(s/def :db/uid uuid?)
(s/def ::frontend-keys (s/keys :req [:db/uid ::type ::sub-type]))
(s/def ::frontend-snapshot (s/merge ::snapshot ::frontend-keys))

(s/fdef new-frontend-snapshot :ret ::frontend-snapshot)

(defn new-frontend-snapshot
  ([frontend-uid user-uid vims-uid {::file/keys [type sub-type] :keys [db/uid]} text]
   (new-frontend-snapshot frontend-uid user-uid vims-uid uid type sub-type nil text))
  ([frontend-uid user-uid vims-uid file-uid type sub-type delta-uid text]
   (-> {:db/uid    frontend-uid
        ::user-uid user-uid
        ::vims-uid vims-uid
        ::file-uid file-uid
        ::type     type
        ::sub-type sub-type
        ::text     text}
       (util/merge-some {::delta-uid delta-uid}))))

(s/fdef ->remote-snapshot
        :args (s/cat :fe ::frontend-snapshot)
        :ret ::snapshot)

(defn ->remote-snapshot
  [frontend-snapshot]
  ;; XXX can't use vimsical.queries.snapshot because of circular deps
  (dissoc frontend-snapshot :db/uid ::type ::sub-type))

(s/fdef ->frontend-snapshot
        :args (s/cat :uid uuid? :s ::snapshot :f ::file/file)
        :ret ::frontend-snapshot)

(defn ->frontend-snapshot
  [uid snapshot {::file/keys [type sub-type]}]
  {:post [(nil? (::file/type %)) (::type %)]}
  (merge snapshot {:db/uid uid ::type type ::sub-type sub-type}))
