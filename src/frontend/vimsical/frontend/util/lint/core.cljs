(ns vimsical.frontend.util.lint.core
  (:require [vimsical.vcs.file :as file]
            [jshint]))

(defmulti lint
  (fn [{::file/keys [sub-type lang-version compiler]} string]
    {:pre [(string? string)]}
    ;; Don't lint when compiler will do it's own parsing and error reporting
    (when (and (nil? compiler) lang-version)
      sub-type)))

(defmethod lint :default [_ _] nil)

(defn format-results [results]
  (vec
   (for [r results]
     {:type :lint-error
      :pos  {:line (.-line r)
             :col  (.-character r)}
      :msg  (.-reason r)})))

(defn valid?
  "http://jshint.com/docs/options/"
  [string opts]
  (js/JSHINT string (clj->js opts)))

(defmethod lint :javascript
  [{::file/keys [lang-version]} string]
  (assert (contains? #{"5" "6"} lang-version))
  ;; returns boolean
  (when-not (valid? string {:esversion  (js/parseInt lang-version)
                            :asi        true ; ignore semicolons
                            :browser    true
                            :devel      true ; dev vars
                            :debug      true ; allow `debugger`
                            ;; TODO additional config based on libs, i.e. jquery
                            :jquery     true
                            :browserify true
                            })
    {::errors (format-results (.-errors js/JSHINT))}))