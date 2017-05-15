(ns vimsical.backend.components.delta-store.protocol)

(defprotocol IDeltaStoreAsync
  (select-deltas-async [_ user-id vims-id success error])
  (insert-deltas-async [_ user-id vims-id deltas success error]))

(defprotocol IDeltaStoreChan
  (select-deltas-chan [_ user-id vims-id])
  (insert-deltas-chan [_ user-id vims-id deltas]))
