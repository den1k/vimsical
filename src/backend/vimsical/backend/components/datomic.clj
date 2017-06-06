(ns vimsical.backend.components.datomic
  (:require
   [clojure.spec.alpha :as s]
   [clojure.java.io :as io]
   [com.stuartsierra.component :as cp]
   [datomic.api :as d]
   [vimsical.common.env :as env]
   [vimsical.backend.util.async :as async])
  (:import
   (datomic Util)
   (datomic.peer Connection LocalConnection)))

;;
;; * Specs
;;

;;
;; ** Config
;;

(s/def ::protocol #{:dev :mem :ddb})
(s/def ::host string?)
(s/def ::port nat-int?)
(s/def ::name string?)

(s/def ::ddb-table string?)
(s/def ::aws-region string?)
(s/def ::aws-access-key-id string?)
(s/def ::aws-secret-key string?)

(defmulti ^:private conf-spec ::protocol)

(defmethod conf-spec :mem [_]
  (s/keys :req [::protocol ::name]))

(defmethod conf-spec :dev [_]
  (s/keys :req [::protocol ::host ::port ::name] :opt [::ddb-table ::aws-region ::aws-access-key-id ::aws-secret-key]))

(defmethod conf-spec :ddb [_]
  (s/keys :req [::protocol ::ddb-table ::name ::aws-region ::aws-access-key-id ::aws-secret-key]))

(s/def ::conf (s/multi-spec conf-spec ::protocol))

;;
;; ** Runtime
;;

(s/def ::uri string?)
(s/def ::conn #(and % (or (instance? Connection %) (instance? LocalConnection %))))
(s/def ::datomic (s/keys :req-un [::conn ::uri]))

;;
;; * Proxy
;;

(defprotocol IDatomicProxy
  (-transact [_  entity-or-txs])
  (-pull [_ query eid])
  (-q [_ query inputs]))

;;
;; * API
;;

(defn transact [component entity-or-txs] (-transact component entity-or-txs))
(defn pull [component query eid] (-pull component query eid))
(defn q [component query & inputs] (-q component query inputs))

;;
;; * Async API
;;

(defn transact-chan [component entity-or-txs]
  (async/thread-try
   (do (deref (-transact component entity-or-txs)) nil)))

(defn pull-chan [component query eid]
  (async/thread-try
   (-pull component query eid)))

(defn q-chan [component query & inputs]
  (async/thread-try
   (-q component query inputs)))

;;
;; * Component
;;

(declare create-schema!)

(defrecord Datomic
    [uri conn]
  cp/Lifecycle
  (start [this]
    (do
      (d/create-database uri)
      (let [conn  (d/connect uri)
            this' (assoc this :conn conn)]
        (if (s/valid? ::datomic this')
          (doto this' create-schema!)
          (throw (ex-info (s/explain-str ::datomic this') {}))))))
  (stop [this]
    (update this :conn d/release))

  IDatomicProxy
  (-transact [_ entity-or-txs]
    (let [txs (if (map? entity-or-txs) [entity-or-txs] entity-or-txs)]
      (d/transact conn txs)))
  (-pull [_ query eid]
    (let [db (d/db conn)]
      (d/pull db query eid)))
  (-q [_ query inputs]
    (let [db (d/db conn)]
      (apply d/q query db inputs))))

;;
;; * Constructor
;;

(defmulti ^:private conf-uri ::protocol)

(defmethod conf-uri :mem [{::keys [name]}]
  (format "datomic:mem://%s" name))

(defmethod conf-uri :dev
  [{::keys [protocol host port name]}]
  (format "datomic:dev://%s:%s/%s" host port name))

(defmethod conf-uri :ddb
  [{::keys [protocol ddb-table name aws-region aws-access-key-id aws-secret-key]}]
  (format "datomic:ddb://%s/%s/%s?aws_access_key_id=%s&aws_secret_key=%s"
          aws-region ddb-table name aws-access-key-id aws-secret-key))

(defn env-conf
  []
  (case (env/required :datomic-protocol ::env/keyword)
    :ddb {::protocol          :ddb
          ::aws-region        (env/required :aws-region ::env/string)
          ::ddb-table         (env/required :datomic-ddb-table ::env/string)
          ::name              (env/required :datomic-name ::env/string)
          ::aws-access-key-id (env/required :aws-access-key-id ::env/string)
          ::aws-secret-key    (env/required :aws-secret-access-key ::env/string)}
    :dev {::protocol :dev
          ::host     (env/required :datomic-host ::env/string)
          ::port     (env/required :datomic-port ::env/int)
          ::name     (env/required :datomic-name ::env/string)}
    :mem {::protocol :mem
          ::name     (env/required :datomic-name ::env/string)}))

(defn ->datomic
  [conf]
  (if (s/valid? ::conf conf)
    (->Datomic (conf-uri conf) nil)
    (throw (ex-info (s/explain ::conf conf) {}))))

(s/def ::datomic (fn [x] (and x (instance? Datomic x))))

;;
;; * Schema Api
;;

(defn- read-transactions
  [file]
  (Util/readAll (io/reader file)))

(defn run-transactions
  [conn transactions]
  (loop [n 0 [tx & more] transactions]
    (if tx
      (recur
       (+ n (count (:tx-data  @(d/transact conn tx))))
       more)
      {:datoms n})))

(defn create-schema!
  ([datomic] (create-schema! datomic (io/resource "datomic/schema.edn")))
  ([{:keys [conn] :as datomic} file]
   (run-transactions conn (read-transactions file))))

(defn delete-database! [{:keys [uri]}] (d/delete-database uri))
