(ns vimsical.frontend.styles.media
  (:require [garden.stylesheet :refer [at-media]]))

;; http://stephen.io/mediaqueries/

(defn- tablet [screen]
  "Portrait and landscape. Based on iPad."
  (fn [& rules]
    (apply at-media {:screen           screen
                     :min-device-width :768px
                     :max-device-width :1024px}
           rules)))

(def on-tablet (tablet :only))
(def not-on-tablet (tablet false))

(defn- phone [screen]
  "Portrait and landscape. Covers iPhone 5 to 6 Plus."
  (fn [& rules]
    (apply at-media {:screen           screen
                     :min-device-width :330px
                     :max-device-width :736px}
           rules)))

(def on-phone (phone :only))
(def not-on-phone (phone false))

(defn- mobile [screen]
  "Portrait and landscape. From iPhone 5 to iPad."
  (fn [& rules]
    (apply at-media {:screen           screen
                     :min-device-width :330px
                     :max-device-width :1024px}
           rules)))

(def on-mobile (mobile :only))
(def not-on-mobile (mobile false))
