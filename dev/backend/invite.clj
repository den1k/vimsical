(ns invite
  (:require
   [com.stuartsierra.component :as cp]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.handlers.auth.commands :refer [create-invite!]]))

(def datomic
  (cp/start (datomic/->datomic (datomic/env-conf))))

(defn token->invite-url
  [token]
  (str "https://vimsical.com/invite/" token))

(comment
  (let [token (create-invite! datomic "Julien" "Fantin")
        url   (token->invite-url token)]
    (println {:token token :url url})
    url))
