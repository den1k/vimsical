(ns vimsical.frontend.styles.media
  (:require [garden.stylesheet :refer [at-media]]))

;; http://stephen.io/mediaqueries/

(defn media-wrapper [min-device-width max-device-width]
  (fn [screen]
    (fn [?orientation & rules]
      (let [orientation? (contains? #{:landscape :portrait} ?orientation)]
        (apply at-media
               (cond-> {:screen           screen
                        :min-device-width min-device-width
                        :max-device-width max-device-width}
                 orientation? (assoc :orientation ?orientation))
               (cond-> rules
                 (not orientation?) (conj ?orientation)))))))

(def ^:private tablet
  "Portrait and landscape. Based on iPad."
  (media-wrapper :768px :1024px))

(def on-tablet (tablet :only))
(def not-on-tablet (tablet false))

(def ^:private phone
  "Portrait and landscape. Covers iPhone 5 to 6 Plus."
  (media-wrapper :330px :736px))

(def on-phone (phone :only))
(def not-on-phone (phone false))

(def ^:private mobile
  "Portrait and landscape. From iPhone 5 to iPad."
  (media-wrapper :330px :1024px))

(def on-mobile (mobile :only))
(def not-on-mobile (mobile false))
