(ns invite
  (:require
   [com.stuartsierra.component :as cp]
   [vimsical.backend.core :refer [system]]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.handlers.auth.commands :refer [create-invite!]]))

(defn token->invite-url
  [token]
  (str "https://vimsical.com/invite/" token))

(defn invite! [first-name last-name]
  (let [{:keys [datomic]} system
        token             (create-invite! datomic first-name last-name)
        url               (token->invite-url token)]
    (println {:token token :url url})
    url))

(comment
  (invite! "Julien" "Fantin"))
