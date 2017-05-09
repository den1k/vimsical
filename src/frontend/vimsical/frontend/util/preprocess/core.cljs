(ns vimsical.frontend.util.preprocess.core
  (:require [vimsical.vcs.file :as file]
            [vimsical.vcs.compiler :as compiler]
            [cljsjs.babel-standalone]
            [clojure.string :as string]))

(defmulti preprocess
  (fn [{::file/keys [compiler]} string]
    {:pre [(string? string)]}
    (::compiler/sub-type compiler)))

(defmethod preprocess :default
  [_ string]
  {::string string})

(defn format-result [res]
  {::string     (.-code res)
   ::ast        (.-ast res)
   ::source-map (.-map res)})

(defn- format-error-msg [msg]
  (-> (subs msg 9)                      ; remove "unknown "
      (string/split #"\s\(\d*:\d*\d\)") ; remove line numbers and annotated code
      first))

(defn- handle-compile-error [e]
  (let [pos     {:line (.. e -loc -line)
                 :col  (.. e -loc -column)}
        message (format-error-msg (.-message e))]
    {:type :compile-error
     :pos  pos
     :msg  message}))

(defmethod preprocess :babel
  [_ string]
  (try
    (->
     (js/Babel.transform string
                         (clj->js {:ast        false
                                   :sourceMaps false
                                   ;; plugins, e.g. jsx, flow
                                   ;; https://github.com/babel/babylon#plugins
                                   :plugins    []}))
     (format-result))
    (catch js/Error e
      {::error (handle-compile-error e)})))