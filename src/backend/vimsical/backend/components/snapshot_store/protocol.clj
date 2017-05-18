(ns vimsical.backend.components.snapshot-store.protocol)

(defprotocol ISnapshotStoreAsync
  (select-snapshots-async [_ user-uid vims-uid success error])
  (insert-snapshots-async [_ user-uid vims-uid snapshots success error]))

(defprotocol ISnapshotStoreChan
  (select-snapshots-chan [_ user-uid vims-uid])
  (insert-snapshots-chan [_ user-uid vims-uid snapshots]))
