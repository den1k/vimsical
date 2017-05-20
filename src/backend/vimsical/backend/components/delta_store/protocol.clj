(ns vimsical.backend.components.delta-store.protocol)

(defprotocol IDeltaStoreAsync
  (select-deltas-async [_ vims-uid success error])
  (insert-deltas-async [_ vims-uid deltas success error]))

(defprotocol IDeltaStoreChan
  (select-deltas-chan [_ vims-uid])
  (insert-deltas-chan [_ vims-uid deltas]))
