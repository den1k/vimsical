(ns vimsical.backend.components.snapshot-store.protocol)

(defprotocol ISnapshotStoreChan
  (select-user-snapshots-chan [_ user-uid] [_ user-uid options])
  (select-vims-snapshots-chan [_ user-uid vims-uid] [_ user-uid vims-uid options])
  (insert-snapshots-chan      [_ snapshots] [_ snapshots options]))
