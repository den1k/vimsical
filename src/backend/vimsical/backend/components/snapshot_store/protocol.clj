(ns vimsical.backend.components.snapshot-store.protocol)

(defprotocol ISnapshotStoreAsync
  (select-user-snapshots-async [_ user-uid success error])
  (select-vims-snapshots-async [_ user-uid vims-uid success error])
  (insert-snapshots-async      [_ snapshots success error]))

(defprotocol ISnapshotStoreChan
  (select-user-snapshots-chan [_ user-uid] [_ user-uid options])
  (select-vims-snapshots-chan [_ user-uid vims-uid] [_ user-uid vims-uid options])
  (insert-snapshots-chan      [_ snapshots] [_ snapshots options]))
