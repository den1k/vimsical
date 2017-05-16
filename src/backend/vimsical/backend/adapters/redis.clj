(ns vimsical.backend.adapters.redis
  (:require
   [clojure.spec :as s]
   [com.stuartsierra.component :as cp]
   [taoensso.carmine :as car :refer [wcar]]))

;;
;; * Helpers
;;

(defn flushall! [redis]
  (car/wcar
   redis
   (car/flushall)))

;;
;; * Component
;;

(defrecord Redis [host port spec]
  cp/Lifecycle
  (start [this]
    (let [spec {:host host :port port}]
      (doto (assoc this :pool nil :spec spec)
        (wcar
         (car/ping)))))
  (stop [this]
    (-> this
        (dissoc :host :port :spec))))

(s/def ::host string?)
(s/def ::port nat-int?)

(s/def ::opts
  (s/keys :req-un [::host ::port]))

(s/fdef ->redis
        :args (s/cat :opts ::opts)
        :ret #(and % (instance? Redis %)))

(defn ->redis
  [opts]
  (map->Redis opts))

(s/def ::redis (fn [x] (and x (instance? Redis x))))
