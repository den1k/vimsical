(ns vimsical.backend.components.delta-store.protocol)

(defprotocol IDeltaStoreChan
  (select-deltas-chan [_ vims-uid])
  (insert-deltas-chan [_ vims-uid user-uid vims-session deltas])
  (select-vims-session-chan [_ vims-uid user-uid]))
