(ns vimsical.frontend.util.preprocess.core
  (:require [vimsical.vcs.file :as file]
            [cljsjs.babel-standalone]))

(defmulti preprocess
  (fn [{::file/keys [compiler]} string]
    {:pre [(string? string)]}
    compiler))

(defmethod preprocess :default
  [_ string]
  {::string string})

(defn format-result [res]
  {::string     (.-code res)
   ::ast        (.-ast res)
   ::source-map (.-map res)})

(defn handle-compile-error [e]
  (let [pos     {:line (.. e -loc -line)
                 :col  (.. e -loc -column)}
        message (.-message e)]
    {::pos     pos
     ::message message}))

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
      {::compile-error (handle-compile-error e)})))