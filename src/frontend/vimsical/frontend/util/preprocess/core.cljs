(ns vimsical.frontend.util.preprocess.core)

(defmulti preprocess (fn [preprocessor _ _] preprocessor))

