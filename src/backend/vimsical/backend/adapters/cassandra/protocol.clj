(ns vimsical.backend.adapters.cassandra.protocol)

(defprotocol ICassandra
  (create-schema! [_ schema])
  (prepare-queries [_ queries]))

(defprotocol ICassandraChan
  (execute-chan
    [this executable]
    [this executable {:keys [channel] :as opts}])
  (execute-batch-chan
    [this commands]
    [this commands batch-type]
    [this commands batch-type {:keys [channel] :as opts}]))
