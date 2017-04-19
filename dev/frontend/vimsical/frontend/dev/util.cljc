(ns vimsical.frontend.dev.util
  (:require [clojure.string :as string]))

(def lorem-ipsum-text
  "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent ut leo sit amet mauris tempus luctus. Nullam placerat metus leo, et faucibus odio mattis a. Nulla ac quam magna. Aliquam vestibulum enim dolor, sit amet volutpat arcu egestas nec. Nullam bibendum volutpat ultricies. Nunc nec vehicula ante. Nam est massa, sollicitudin sit amet felis a, vestibulum viverra nunc. Mauris et tristique turpis, non eleifend nulla. Mauris commodo blandit justo id imperdiet. Phasellus varius eget turpis interdum sodales. Vestibulum ac efficitur metus.")

(defn lorem-ipsum [n-paragraphs]
  (string/join "\n\n" (repeat n-paragraphs lorem-ipsum-text)))
