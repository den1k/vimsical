(ns vimsical.vcs.bench
  (:require
   [criterium.core :refer [quick-bench]]
   [vimsical.common.test :refer [uuid uuid-fixture uuid-gen]]
   [vimsical.vcs.core :as sut]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.editor :as editor])
  (:gen-class))

(comment
  ;; Make sure to disable instrumentation if running at the REPL
  (require '[orchestra.spec.test :as st])
  (st/unstrument))

;;
;; * Refs
;;

(def master-uid (uuid :master))
(def child-uid  (uuid :child))
(def gchild-uid (uuid :gchild))
(def file1-uid  (uuid :file1))
(def file2-uid  (uuid :file2))

(def id0 (uuid :uid0))
(def id1 (uuid :uid1))
(def id2 (uuid :uid2))
(def id3 (uuid :uid3))
(def id4 (uuid :uid4))
(def id5 (uuid :uid5))
(def id6 (uuid :uid6))
(def id7 (uuid :uid7))
(def id8 (uuid :uid8))


(def master {:db/uid master-uid})
(def child  {:db/uid child-uid, ::branch/parent master ::branch/branch-off-delta-uid id1 ::branch/start-deltas-uid id2})
(def gchild {:db/uid gchild-uid ::branch/parent child, ::branch/branch-off-delta-uid id3 ::branch/start-deltas-uid id4})

(def branches [master child gchild])

;;
;; * Helpers
;;

(defn edit-events->deltas
  [edit-events]
  (let [{uuid-fn :f} (uuid-gen)
        branches     [master]
        effects      {::editor/pad-fn       (constantly 1)
                      ::editor/uuid-fn      (fn [& _] (uuid-fn))
                      ::editor/timestamp-fn (constantly 1)}
        [_  deltas _] (-> (sut/empty-vcs branches)
                          (sut/add-edit-events effects (uuid :html) (uuid :master) nil edit-events))]
    deltas))

;;
;; * Data
;;

(def small-ipsum "

  Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque pharetra purus fermentum felis mollis ullamcorper. Nunc et ornare sem. Aenean mollis a odio at fringilla. Nam sit amet euismod felis. Praesent auctor a tortor nec sagittis. Aliquam commodo nulla sed lacus iaculis, quis vestibulum lorem elementum. Donec iaculis turpis sit amet fringilla tempus. Ut rhoncus luctus elit, in ultricies urna dignissim eget. Nam luctus maximus egestas. Fusce rhoncus mattis urna, quis ultrices risus semper aliquam. Nunc ultricies bibendum aliquet. Aenean enim nibh, suscipit vitae mattis ut, gravida eget risus. Integer posuere pellentesque ex, vel venenatis diam tempus eu. Aliquam blandit nisi enim, id imperdiet nibh commodo nec. Curabitur sit amet egestas massa. Nulla cursus condimentum convallis.
  ")

(def med-ipsum "

  Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque pharetra purus fermentum felis mollis ullamcorper. Nunc et ornare sem. Aenean mollis a odio at fringilla. Nam sit amet euismod felis. Praesent auctor a tortor nec sagittis. Aliquam commodo nulla sed lacus iaculis, quis vestibulum lorem elementum. Donec iaculis turpis sit amet fringilla tempus. Ut rhoncus luctus elit, in ultricies urna dignissim eget. Nam luctus maximus egestas. Fusce rhoncus mattis urna, quis ultrices risus semper aliquam. Nunc ultricies bibendum aliquet. Aenean enim nibh, suscipit vitae mattis ut, gravida eget risus. Integer posuere pellentesque ex, vel venenatis diam tempus eu. Aliquam blandit nisi enim, id imperdiet nibh commodo nec. Curabitur sit amet egestas massa. Nulla cursus condimentum convallis.

  Ut fringilla nisl mi, at viverra mi lobortis at. Donec fermentum ullamcorper iaculis. Duis accumsan turpis sollicitudin, lobortis augue vel, efficitur odio. Vestibulum in nunc orci. Nam volutpat lorem id condimentum auctor. Cras eu nunc nec arcu elementum bibendum ut in nunc. Vivamus laoreet scelerisque lacinia. Morbi eget tincidunt ligula, id hendrerit lorem. Mauris feugiat non mauris placerat ultricies. Sed lobortis condimentum quam sit amet efficitur. Sed eu maximus ex, id mollis diam. Nunc non ipsum sed augue tempor facilisis. Mauris nibh ligula, fermentum sed nisl ac, semper sagittis dui. Phasellus lorem turpis, cursus et varius non, porta id nisi. Cras dictum pharetra dolor, eget venenatis est gravida non. Morbi a risus quis elit ultrices dignissim quis vitae eros.

  Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Nam placerat urna id nulla porttitor sagittis. Vestibulum sed ligula quis justo egestas congue. Proin eu tristique massa. Morbi eu tempor neque. Sed congue nisl interdum velit cursus, at mollis est cursus. Vivamus iaculis aliquet erat, a suscipit diam faucibus quis. Interdum et malesuada fames ac ante ipsum primis in faucibus. Phasellus rutrum nibh ut nulla hendrerit luctus. Nulla dignissim mollis facilisis. Suspendisse potenti. Nulla vehicula in risus in ultrices. Mauris sit amet lacus scelerisque risus blandit consectetur. Fusce malesuada sodales congue. Aliquam efficitur feugiat sem at tempus. Quisque sed nisi a tortor luctus imperdiet non et enim.

  Pellentesque pulvinar nulla in urna consequat, nec malesuada sapien vulputate. Praesent et urna sagittis, mattis nisl sed, rhoncus nulla. Ut ipsum quam, suscipit eget nulla convallis, semper interdum lacus. Vivamus mattis aliquam mauris, sed dignissim magna sodales quis. Curabitur in nibh a nulla finibus congue viverra quis massa. Pellentesque viverra lectus porttitor arcu mollis euismod. Cras sit amet nibh fringilla, tristique massa vitae, pulvinar ipsum. Suspendisse commodo dapibus fringilla. In hac habitasse platea dictumst. Donec nec eros et neque lacinia viverra id at velit. Nam eu enim et metus luctus maximus auctor at metus. Etiam eu lectus a est placerat vulputate non sed lectus. Etiam at augue mollis, lacinia diam sit amet, dictum mi. Vivamus ante eros, placerat ut viverra at, faucibus vitae odio. Maecenas ultrices suscipit ligula in volutpat.

  Suspendisse consequat ac mauris nec vestibulum. Nullam at odio bibendum metus posuere malesuada at et turpis. Nunc cursus semper molestie. Quisque mollis libero in eros eleifend eleifend. Mauris sollicitudin lorem vitae eros vehicula, sit amet facilisis dui mattis. Suspendisse sed est enim. Duis lorem leo, elementum at diam a, blandit dignissim sem. Donec id sodales tortor, sit amet faucibus mauris. Nunc eget consequat metus. Vestibulum molestie mauris sed libero euismod, vitae porta felis sagittis. ")


(defn spliced-edit-events
  [txt]
  [{:vimsical.vcs.edit-event/op :str/ins, :vimsical.vcs.edit-event/diff txt :vimsical.vcs.edit-event/idx 0}
   {:vimsical.vcs.edit-event/op :str/rplc, :vimsical.vcs.edit-event/diff txt :vimsical.vcs.edit-event/idx 0 , :vimsical.vcs.edit-event/amt (count txt)}
   {:vimsical.vcs.edit-event/op :str/ins, :vimsical.vcs.edit-event/diff txt :vimsical.vcs.edit-event/idx 0}])

(def  uuid-fn (:f (uuid-gen)))

;;
;; * Benchmarks
;;

(defn sep [s]
  (println
   (str \newline (apply str (repeat (- 80 (count s)) "-")) \space s \newline)))

(defn -main [& _]
  (sep "1. Spliced events")
  (doseq [txt [small-ipsum med-ipsum]]
    (let [edit-events (spliced-edit-events txt)
          deltas      (edit-events->deltas edit-events)]
      (println "Deltas count:" (count deltas))
      (sep "1.1 Add edit event")
      (quick-bench (edit-events->deltas edit-events))
      (sep "1.2 Add deltas")
      (quick-bench (-> (sut/empty-vcs branches) (sut/add-deltas uuid-fn deltas))))))

