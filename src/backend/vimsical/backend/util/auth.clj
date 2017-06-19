(ns vimsical.backend.util.auth
  (:require
   [buddy.hashers :as hashers]
   [clojure.spec.alpha :as s]
   [vimsical.user :as user]))

;;
;; * Password hashing
;;

(s/def ::hash string?)

(def hash-algorithm :bcrypt+sha512)

(s/fdef hash-password :args (s/cat :password ::user/password) :ret ::hash)

(defn hash-password
  [password]
  (hashers/derive password {:alg hash-algorithm}))

(s/fdef check-password :args (s/cat :password ::user/password :hash (s/nilable ::hash)) :ret boolean)

(defn check-password
  [password hashed-password]
  (and password hashed-password (hashers/check password hashed-password)))
