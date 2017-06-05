(ns vimsical.frontend.remotes.remote)

(defmulti init!
  "Takes a remote id and returns any state required for `send!`"
  (fn [id] id))

(defmulti send!
  "Takes a remote fx, a state returned by `init!` for that remote and callbacks.

  `result-cb` takes the clojure result data, `error-cb` takes an error.

  If this function doesn't return nil, the return value will be used as the new
  remote state, and will be passed as an argument on the next invocation."
  (fn [{:keys [id] :as fx} state result-cb error-cb] id))
