(ns vimsical.vcs.core
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.data.indexed.vector :as indexed.vector]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.file :as file]))
