(ns vimsical.frontend.router.routes
  #?@(:clj
      [(:require
        [clojure.spec.alpha :as s]
        [vimsical.common.util.core :as util])]
      :cljs
      [(:require
        [bidi.bidi :as bidi]
        [clojure.spec.alpha :as s]
        [vimsical.common.util.core :as util])]))

;;
;; * Spec
;;

(s/def ::route-handler keyword?)
(s/def ::args map?)
(s/def ::route (s/keys :req [::route-handler] :opt [::args]))

(def routes
  ["/"
   {["vims/" :db/uid]  ::vims
    "signup"           ::signup
    ["invite/" :token] ::invite
    "emoji"            ::emoji
    true               ::landing}])

;;
;; * Coercion
;;

(defn uuid->str [uuid]
  #?(:clj (assert false "Not implemented")
     :cljs (str uuid)))

(defn str->uuid [s]
  #?(:clj  (assert false "Not implemented")
     :cljs (if (string? s) (cljs.core/uuid s) s)))

;;
;; * Codecs
;;

;; NOTE might decode a previously decoded route

(defmulti  encode-route ::route-handler)
(defmethod encode-route :default [route] route)
(defmethod encode-route ::vims   [route] (update-in route [::args :db/uid] str))


(defmulti  decode-route ::route-handler)
(defmethod decode-route :default [route] route)
(defmethod decode-route ::vims   [route] (update-in route [::args :db/uid] str->uuid))

;;
;; * Ctor
;;

(s/fdef new-route :ret ::route)

(defn new-route
  ([route-handler] {::route-handler route-handler})
  ([route-handler args]
   (cond-> {::route-handler route-handler}
     (some? args) (assoc ::args args))))

;;
;; * Equality
;;

;; NOTE running into weird behavior with cljs.core/uuid where two values
;; constructed from the same string don't test =

(defn args=
  [{uida :db/uid :as a}
   {uidb :db/uid :as b}]
  #?(:clj (= a b)
     :cljs
     (if (and uida uidb)
       (util/=by uuid->str uida uidb)
       (= a b))))

(defn route-name= [route route-name]
  (= (-> route ::route-handler name keyword) route-name))

(defn route=
  [a b]
  (and
   (util/=by ::route-handler a b)
   (args= (::args a) (::args b))))

;;
;; * Accessors
;;

(defn get-arg [route arg] (get-in route [::args arg]))

;;
;; * URI
;;

(defn uri-for
  [route-handler & args]
  #?(:clj (assert false "Not implemented, use env...")
     :cljs (let [origin (.-origin (.-location js/window))
                 path   (apply bidi/path-for routes route-handler args)]
             (str origin path))))

(defn vims-uri [{:keys [db/uid] :as vims}]
  (uri-for ::vims :db/uid uid))
