(ns vimsical.frontend.remotes.fx
  "Usage:
  (re-frame/reg-event-fx ::example
    (fn [db _]
      ...
      {:db ...
       :remote
        {:id    :backend
         :event [::auth.commands/login {::user/email \"foo@bar.com\" ::user/password \"123\"}]}}"
  (:require
   [clojure.spec :as s]
   [re-frame.core :as re-frame]
   [vimsical.frontend.remotes.remote :as p]
   [vimsical.remotes.event :as event]))

;;
;; * Spec
;;

(s/def ::id keyword?)
(s/def ::state any?)
(s/def ::fx (s/keys :req-un [::id ::event/event]))

;;
;; * Registry
;;

(defonce ^:private registry (atom {}))

(defn- get-remote-state [{:keys [id] :as fx}]
  (if-some [state (get @registry id)]
    state
    (let [state (p/init! id)]
      (swap! registry assoc id state)
      state)))

;;
;; * Fx
;;

(s/fdef remote-fx :args (s/cat :fx ::fx))

(defn- remote-fx
  [{:keys [id event] :as fx}]
  (let [state (get-remote-state fx)]
    (letfn [(result-cb [result]
              (re-frame/dispatch (event/event-result event result)))
            (error-cb [error]
              (re-frame/dispatch (event/event-error event error)))]
      (when-some [state (p/send! id state event result-cb error-cb)]
        (swap! registry assoc id state)))))

(re-frame/reg-fx :remote remote-fx)
