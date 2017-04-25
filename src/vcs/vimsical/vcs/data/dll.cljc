(ns vimsical.vcs.data.dll
  "A generic Doubly Linked List implemented on top of hash-map."
  (:require [clojure.pprint :as pprint :refer [pprint]])
  (:refer-clojure :rename {update clj-update} :exclude [replace #?(:cljs update)]))

(defprotocol PDoublyLinkedList
  (make-key [this x])
  (get-head [this])
  (get-tail [this])
  (set-head [this x])
  (set-tail [this x])
  (get-head-node [this])
  (get-tail-node [this])
  (get-node [this x])
  (get-node-by-key [this k])
  (add-before [this node-val x])
  (add-after [this node-val x])
  (get-prev [this node-val])
  (get-next [this node-val])
  (seq-from [this start-val dir])
  (replace [this node-val x])
  (subrange [this from] [this from to]))

(defrecord Node [prev next val])

#?(:cljs
   (extend-type Node
     IPrintWithWriter
     (-pr-writer [this writer _]
       (-write writer (str "#DoublyLinkedList/Node" (into {} this))))))

(defn make-node [prev next val]
  (Node. prev next val))

(defn seq*
  ([m start-key next-key]
   (seq* m start-key ::none next-key))
  ([m start-key stop-key next-key]
   (when-let [node (get m start-key)]
     (cons (:val node) (lazy-seq
                        (when-not (= start-key stop-key)
                          (seq* m (next-key node) stop-key next-key)))))))

(defn- entry-seq [m start next-key]
  (lazy-seq
   (when-let [[_ node :as node-entry] (find m start)]
     (cons node-entry (entry-seq m (next-key node) next-key)))))

(defn nth* [dll n not-found]
  (loop [node (.-head dll) i 0]
    (if-let [next-node (get (.-m dll) node)]
      (if (= i n)
        (:val next-node)
        (recur (:next next-node) (inc i)))
      not-found)))

(deftype DoublyLinkedList [m head tail kfn]
  #?@(:clj
      [Object
       (toString [_]
                 (str "#DoublyLinkedList" {:head head :tail tail :m m}))
       clojure.lang.Sequential
       clojure.lang.Counted
       (count [_] (count m))
       clojure.lang.Seqable
       (seq [_] (seq* m head tail :next))
       clojure.lang.Reversible
       (rseq [_] (seq* m tail head :prev))
       clojure.lang.ISeq
       (empty [_]
              (DoublyLinkedList. (empty m) nil nil kfn))
       (first [this]
              (get-head this))
       (next [this]
             (next (seq this)))
       (cons [this x]
             (if-let [tail-val (get-tail this)]
               (add-after this tail-val x)
               (set-tail this x)))
       clojure.lang.IPersistentStack
       (peek [this]
             (get-tail this))
       clojure.lang.IPersistentSet
       (equiv
        [this other]
        (and
         (= (type this) (type other))
         (= (.-m this) (.-m other))
         (= (.-head this) (.-head other))
         (= (.-tail this) (.-tail other))))
       (disjoin [_ node-val]
                (let [node-key (kfn node-val)
                      {:keys [next prev]} (get m node-key)]
                  (when-not (and node-val node-key)
                    (throw (ex-info "Node does not exist."
                                    {:node-key node-key
                                     :node-val node-val})))
                  (let [m    (-> m
                                 (dissoc node-key)
                                 (cond->
                                     prev (clj-update prev assoc :next next)
                                     next (clj-update next assoc :prev prev)))
                        head (if (= head node-key) next head)
                        tail (if (= tail node-key) prev tail)]
                    (DoublyLinkedList. m head tail kfn))))
       clojure.lang.ILookup
       (valAt [_ k]
              (let [node-key (kfn k)]
                (:val (.valAt m node-key nil))))
       (valAt [_ k not-found]
              (let [node-key (kfn k)]
                (or (:val (.valAt m node-key)) not-found)))]
      :cljs
      [Object
       (toString [_] (str "#DoublyLinkedList" {:head head :tail tail :m m}))
       IEquiv
       (-equiv
        [this other]
        (and
         (= (type this) (type other))
         (= (.-m this) (.-m other))
         (= (.-head this) (.-head other))
         (= (.-tail this) (.-tail other))))
       ICounted
       (-count [_] (count m))
       ISeqable
       (-seq [_] (seq* m head tail :next))
       IReversible
       (-rseq [_] (seq* m tail head :prev))
       IEmptyableCollection
       (-empty [this] (DoublyLinkedList. (empty m) nil nil kfn))
       ICollection
       (-conj
        [this x]
        (if-let [tail-val (get-tail this)]
          (add-after this tail-val x)
          (set-tail this x)))
       ISet
       (-disjoin
        [_ node-val]
        (let [node-key (kfn node-val)
              {:keys [next prev]} (get m node-key)]
          (when-not (and node-val node-key)
            (throw (ex-info "Node does not exist."
                            {:node-key node-key
                             :node-val node-val})))
          (let [m    (-> m
                         (dissoc node-key)
                         (cond->
                             prev (clj-update prev assoc :next next)
                             next (clj-update next assoc :prev prev)))
                head (if (= head node-key) next head)
                tail (if (= tail node-key) prev tail)]
            (DoublyLinkedList. m head tail kfn))))
       IIndexed
       (-nth [this i] (nth this i nil))
       (-nth [this i not-found] (nth* this i not-found))
       IStack
       (-peek
        [this]
        (get-tail this))
       ILookup
       (-lookup [this x] (-lookup this x nil))
       (-lookup
        [_ x not-found]
        (let [k (kfn x)]
          (or (:val (get m k)) not-found)))
       IPrintWithWriter
       (-pr-writer
        [this writer _]
        (-write writer (str "#DoublyLinkedList" (interpose '<-> (seq this)))))])
  PDoublyLinkedList
  (make-key [_ x]
    (let [k (kfn x)]
      (when-not (get m k)
        (throw (ex-info (str "Node must exist for key " k ".")
                        {:provided-value x
                         :generated-key  k})))
      k))
  (get-head [this]
    (:val (get-head-node this)))
  (get-tail [this]
    (:val (get-tail-node this)))
  (get-head-node [_]
    (get m head))
  (get-tail-node [_]
    (get m tail))
  (set-head [_ x]
    (let [next-head (kfn x)
          m         (cond-> m
                      (not (get m next-head))
                      (assoc next-head (make-node nil head x)))
          tail      (or tail next-head)]
      (when-not next-head
        (throw (ex-info "Head cannot be nil" {:val x})))
      (DoublyLinkedList. m next-head tail kfn)))
  (set-tail [_ x]
    (let [next-tail (kfn x)
          m         (cond-> m
                      (not (get m next-tail))
                      (assoc next-tail (make-node tail nil x)))
          head      (or head next-tail)]
      (when-not next-tail
        (throw (ex-info "Tail cannot be nil" {:val x})))
      (DoublyLinkedList. m head next-tail kfn)))
  (get-node [_ node-val]
    (when-let [k (kfn node-val)]
      (get m k)))
  (get-node-by-key [_ k]
    (get m k))
  (get-prev [this node-val]
    (let [node-key (kfn node-val)]
      (:val (get-node-by-key this (:prev (get m node-key))))))
  (get-next [this node-val]
    (let [node-key (kfn node-val)]
      (:val (get-node-by-key this (:next (get m node-key))))))
  (add-after [_ node-val x]
    (let [x-key     (kfn x)
          node-key  (kfn node-val)
          node-next (:next (get m node-key))]
      (when-not (and node-val node-key)
        (throw (ex-info "Node after which to insert does not exist"
                        {:node     node-val
                         :node-key node-key
                         :x        x})))
      (when-let [exists (get m x-key)]
        (throw (ex-info (str "Node at key " x-key " already exists. Use assoc "
                             "to replace an existing node.")
                        {:key           x-key
                         :existing-node exists})))
      (let [m    (-> m
                     (assoc x-key (make-node node-key node-next x))
                     (clj-update node-key assoc :next x-key)
                     (cond->
                         node-next (clj-update node-next assoc :prev x-key)))
            tail (if (= node-key tail) x-key tail)]
        (DoublyLinkedList. m head tail kfn))))
  (add-before [_ node-val x]
    (let [x-key     (kfn x)
          node-key  (kfn node-val)
          node-prev (:prev (get m node-key))]
      (when-not (and node-val node-key)
        (throw (ex-info "Node before which to insert does not exist"
                        {:node     node-val
                         :node-key node-key
                         :x        x})))
      (when-let [exists (get m x-key)]
        (throw (ex-info (str "Node at key " x-key " already exists. Use assoc "
                             "to replace an existing node.")
                        {:key           x-key
                         :existing-node exists})))
      (let [m    (-> m
                     (assoc x-key (make-node node-prev node-key x))
                     (clj-update node-key assoc :prev x-key)
                     (cond->
                         node-prev (clj-update node-prev assoc :next x-key)))
            head (if (= node-key head) x-key head)]
        (DoublyLinkedList. m head tail kfn))))
  (replace [_ node-val x]
    (let [x-key    (kfn x)
          node-key (kfn node-val)
          {node-next :next node-prev :prev} (get m node-key)]
      (when-not (and node-val node-key)
        (throw (ex-info "Node which to replace does not exist"
                        {:node     node-val
                         :node-key node-key
                         :x        x})))
      (let [m    (-> m
                     (dissoc node-key)
                     (assoc x-key (make-node node-prev node-next x))
                     (cond->
                         node-prev (clj-update node-prev assoc :next x-key)
                         node-next (clj-update node-next assoc :prev x-key)))
            head (if (= head node-key) x-key head)
            tail (if (= node-key tail) x-key tail)]
        (DoublyLinkedList. m head tail kfn))))
  (seq-from [_ start-val dir]
    {:pre [(get #{:next :prev} dir)]}
    (seq* m (kfn start-val) dir))
  (subrange [this from]
    (subrange this from (get-tail-node this)))
  (subrange [_ from-val to-val]
    (let [to-key (kfn to-val)]
      (sequence (comp (take-while (fn [[k _]] (not= to-key k)))
                      (map (comp :val second)))
                (entry-seq m (kfn from-val) :next)))))

(defn update [^DoublyLinkedList dll node-val f & args]
  (let [kfn      (.-kfn dll)
        m        (.-m dll)
        node-key (kfn node-val)
        cval     (:val (get m node-key))]
    (when-not (and node-val node-key)
      (throw (ex-info "Node which to update does not exist"
                      {:node     node-val
                       :node-key node-key})))
    (replace dll cval (apply f cval args))))

(defn doubly-linked-list
  ([kfn]
   (DoublyLinkedList. {} nil nil kfn))
  ([kfn & vals]
   (into (DoublyLinkedList. {} nil nil kfn) vals)))

#?(:clj (defmethod print-method DoublyLinkedList [dll w]
          (print-method (symbol "#DoublyLinkedList") w)
          (print-method (interpose '<-> (seq dll)) w)))

;; Fixes pretty print
#?(:clj (defmethod clojure.pprint/simple-dispatch DoublyLinkedList [o]
          ((get-method pprint/simple-dispatch clojure.lang.ISeq) o)))

#?(:clj (defmethod print-method Node [n w]
          (print-method (symbol "#DoublyLinkedList/Node") w)
          (print-method (into {} n) w)))

(comment
  (require '[criterium.core :refer [quick-bench]])
  (let [med       (vec (range 10000))
        large     (vec (range 1000000))
        dll-med   (apply doubly-linked-list str med)
        dll-large (apply doubly-linked-list str large)]

   ;;; Medium Size

                                        ;(quick-bench (apply doubly-linked-list str med))
                                        ;Execution time mean : 11.010305 ms

                                        ;(quick-bench (seq dll-med))
                                        ;Execution time mean : 24.671025 ns

    ;; subrange - small

                                        ;(quick-bench (subrange dll-med 5000 10000))
                                        ;Execution time mean : 2.858399 ms

                                        ;(quick-bench (assoc dll-med 5000 1000000000000000))
                                        ;Execution time mean : 1.511168 µs

   ;;; Large Size

                                        ;(quick-bench (apply doubly-linked-list str large))
                                        ;Execution time mean : 2.373459 sec

                                        ;(quick-bench (seq dll-large))
                                        ;Execution time mean : 16.552517 ns

    ;; subrange - small

                                        ;(quick-bench (subrange dll-large 5000 10000))
                                        ;Execution time mean : 4.663050 ms

    ;; subrange - large

                                        ;(quick-bench (subrange dll-large 500000 1000000))
                                        ;Execution time mean : 399.820492 ms

                                        ;(quick-bench (assoc dll-large 5000000 1000000000000000))
                                        ;Execution time mean : 619.556069 ns
    ))

(deftype DoublyLinkedSublist [^DoublyLinkedList dll subhead subtail]
  ;; todo cljs
  #?@(:clj
      [clojure.lang.Sequential
       clojure.lang.Counted
       (count [this] (count (seq this)))
       clojure.lang.Seqable
       (seq [_] (seq* (.-m dll) subhead subtail :next))
       clojure.lang.Reversible
       (rseq [_] (seq* (.-m dll) subtail subhead :prev))
       ]
      :cljs
      [ISeqable
       (-seq [_] (seq* (.-m dll) subhead subtail :next))
       IReversible
       (-rseq [_] (seq* (.-m dll) subtail subhead :prev))])

  PDoublyLinkedList
  (get-head [this]
    (:val (get-head-node this)))

  (get-tail [this]
    (:val (get-tail-node this)))

  (get-head-node [_]
    (get-node-by-key dll subhead))

  (get-tail-node [_]
    (get-node-by-key dll subtail))

  (set-head [_ x]
    (let [k (make-key dll x)]
      (DoublyLinkedSublist. dll k subtail)))

  (set-tail [_ x]
    (let [k (make-key dll x)]
      (DoublyLinkedSublist. dll subhead k)))

  (get-node-by-key [_ k]
    (get-node-by-key dll k)))

(defn doubly-linked-sublist
  ([dll] (DoublyLinkedSublist. dll (.-head dll) (.-tail dll)))
  ([dll subhead-val subtail-val]
   (let [[subhead subtail] (map (partial make-key dll) [subhead-val subtail-val])]
     (DoublyLinkedSublist. dll subhead subtail))))

#?(:clj (defmethod print-method DoublyLinkedSublist [dll w]
          (print-method (symbol "#DoublyLinkedSublist") w)
          (print-method (interpose '<-> (seq dll)) w)))

;; Fixes pretty print
#?(:clj (defmethod clojure.pprint/simple-dispatch DoublyLinkedSublist [o]
          ((get-method pprint/simple-dispatch clojure.lang.ISeq) o)))

(defn dll? [x] (and x (satisfies? PDoublyLinkedList x)))
