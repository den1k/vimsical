(ns vimsical.backend.adapters.cassandra.protocol
  (:require
   [clojure.core.async :as async]))

(defprotocol ICassandra
  (create-schema! [_ schema])
  (prepare-queries [_ queries]))

(defprotocol ICassandraAsync
  (execute-async
    [this executable success error]
    [this executable success error opts])
  (execute-batch-async
    [this commands success error]
    [this commands batch-type success error]
    [this commands batch-type success error opts]))

(defprotocol ICassandraChan
  (execute-chan
    [this executable]
    [this executable {:keys [channel] :as opts}])
  (execute-batch-chan
    [this commands]
    [this commands batch-type]
    [this commands batch-type {:keys [channel] :as opts}]))

(defn exception?
  [x]
  (and x (instance? clojure.lang.ExceptionInfo x)))

(defn <? [chan]
  (when-some [x (async/<! chan)]
    (if (exception? x) (throw x) x)))

(defn <?? [chan]
  (when-some [x (async/<!! chan)]
    (if (exception? x) (throw x) x)))
