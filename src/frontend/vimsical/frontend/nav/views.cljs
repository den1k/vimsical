(ns vimsical.frontend.nav.views
  (:require
   [re-frame.core :as re-frame]
   [vimsical.frontend.views.icons :as icons]))

(defn nav
  []
  (let [{:user/keys
         [first-name
          last-name] :as res} (deref
                               (re-frame/subscribe
                                [:q*
                                 [:db/id
                                  :user/first-name
                                  :user/last-name]
                                 :app/user])
                               ;; NOTE see db.cljc
                               ;; Could also use:
                               ;; (re-frame/subscribe
                               ;;  [:q
                               ;;   {[:app/user ']
                               ;;    [:db/id
                               ;;     :user/first-name
                               ;;     :user/last-name]}])
                               ;; But the result would be nested under
                               ;; {:app/user ...}
                               )]
    (println "PULLED:" res)
    [:div.nav
     [:div.logo-and-type
      (str first-name " " last-name)
      [:span.logo icons/vimsical-logo]
      [:span.type icons/vimsical-type]]]))
