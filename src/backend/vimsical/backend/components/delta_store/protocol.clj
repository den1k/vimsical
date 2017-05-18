(ns vimsical.backend.components.delta-store.protocol)

(defprotocol IDeltaStoreAsync
  (select-deltas-async [_ user-uid vims-uid success error])
  (insert-deltas-async [_ user-uid vims-uid deltas success error]))

(defprotocol IDeltaStoreChan
  (select-deltas-chan [_ user-uid vims-uid])
  (insert-deltas-chan [_ user-uid vims-uid deltas]))
