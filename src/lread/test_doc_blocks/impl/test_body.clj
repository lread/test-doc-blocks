(ns ^:no-doc lread.test-doc-blocks.impl.test-body
  "Convert a prepared doc block into a test body."
  (:require [lread.test-doc-blocks.impl.zutil :as zutil]
            [rewrite-cljc.node :as n]
            [rewrite-cljc.zip :as z] ))

(defn- eval-assertion [expected-node actual-node]
  (n/list-node [(n/token-node 'clojure.test/is)
                (n/spaces 1)
                (n/list-node [(n/token-node '=)
                              (n/spaces 1)
                              (n/quote-node expected-node)
                              (n/spaces 1)
                              actual-node])]))

(defn- platform-eval-assertion [platform expected-node actual-node]
  (n/reader-macro-node [(n/token-node '?)
                        (n/list-node [(n/keyword-node platform)
                                      (n/spaces 1)
                                      (eval-assertion expected-node actual-node)])]))

(defn- output-assertion [expected-node actual-node]
  (n/list-node [(n/token-node 'clojure.test/is)
                (n/spaces 1)
                (n/list-node [(n/token-node (symbol "="))
                              (n/spaces 1)
                              expected-node
                              (n/spaces 1)
                              (n/list-node [(n/token-node 'clojure.string/split-lines)
                                            (n/spaces 1)
                                            actual-node])])]))

(defn- interpose-nodes [items coll]
  (into [(first coll)]
        (mapcat #(concat items [%]) (rest coll))))

(defn- capturing-assertions [expected-pairs]
  (->> (for [[atoken expected] expected-pairs]
         (case (z/sexpr atoken)
           => (eval-assertion (z/node expected) (n/token-node 'actual))
           =clj=> (platform-eval-assertion :clj (z/node expected) (n/token-node 'actual))
           =cljs=> (platform-eval-assertion :cljs (z/node expected) (n/token-node 'actual))
           =stdout=> (output-assertion  (z/node expected) (n/token-node 'actual-out))
           =stderr=> (output-assertion (z/node expected) (n/token-node 'actual-err))))
       (interpose-nodes [(n/newlines 1) (n/spaces 2)])))

(defn- assertion-token? [t]
  (some #{t} '(=> =stderr=> =stdout=> =clj=> =cljs=>)))

(defn- token? [zloc token]
  (and (= :token (z/tag zloc))
       (not (n/printable-only? (z/node zloc)))
       (= token (z/sexpr zloc))))

(defn- zassertion-token? [zloc]
  (and (= :token (z/tag zloc))
       (not (n/printable-only? (z/node zloc)))
       (assertion-token? (z/sexpr zloc))))

(defn- requires-capture? [expected-pairs]
  (some (fn [[atoken _]]
          (some #{(z/sexpr atoken)} ['=stdout=> '=stderr=>]))
        expected-pairs))

(defn- capturing-test-assert-node [expected-pairs actual]
  (n/list-node (concat [(n/token-node 'let)
                                       (n/spaces 1)
                                       (n/vector-node [(n/vector-node [(n/token-node 'actual)
                                                                       (n/spaces 1)
                                                                       (n/token-node 'actual-out)
                                                                       (n/spaces 1)
                                                                       (n/token-node 'actual-err)])
                                                       (n/spaces 1)
                                                       (n/list-node [(n/token-node 'lread.test-doc-blocks.runtime/eval-capture)
                                                                     (n/spaces 1)
                                                                     (z/node actual)])])
                                       (n/newlines 1)
                                       (n/spaces 2)]
                                      (capturing-assertions expected-pairs))))

(defn- simple-assertion-nodes [expected-pairs actual]
  (concat
   (->> (for [[atoken expected] expected-pairs]
          (case (z/sexpr atoken)
            => (eval-assertion (z/node expected) (z/node actual))
            =clj=> (platform-eval-assertion :clj (z/node expected) (z/node actual))
            =cljs=> (platform-eval-assertion :cljs (z/node expected) (z/node actual))))
        (interpose-nodes [(n/newlines 1)]))
   [(n/newlines 1)]))

(defn- replace-body-assert [zloc expected-pairs actual]
  (let [new-nodes (if (requires-capture? expected-pairs)
                    [(capturing-test-assert-node expected-pairs actual)]
                    (simple-assertion-nodes expected-pairs actual))]
    (-> (reduce z/insert-left* zloc new-nodes)
        z/left
        ;; remove REPL => token and expected pairs
        (zutil/remove-n-siblings-right (inc (* 2 (count expected-pairs)))))))

(defn- get-expected-pairs [zloc]
  (->> zloc
       (iterate z/right)
       (take-while identity)
       (partition 2)
       (take-while #(zassertion-token? (first %)))
       (sort-by (fn [[ztoken _zexpected]] (z/sexpr ztoken)))))

(defn- convert-repl-assertions [prepared-doc-body]
 (-> (loop [zloc (z/of-string prepared-doc-body)]
       (let [zloc (cond
                    (not zloc)
                    (throw (ex-info "unexpected error: zloc nil" {}))

                    (z/end? zloc)
                    zloc

                    ;; REPL style has 1 expectation
                    (and (token? zloc 'user=>) (-> zloc z/right z/right))
                    (let [actual (z/right zloc)
                          expected-pairs [[(z/of-string "=>") (z/right actual)]]]
                      (-> zloc
                          (replace-body-assert expected-pairs actual)))

                    ;; Editor style can have multiple expectations
                    (and (zassertion-token? zloc) (z/left zloc) (z/right zloc))
                    (let [actual (z/left zloc)
                          expecteded-pairs (get-expected-pairs zloc) ]
                      (-> zloc
                          z/left
                          (replace-body-assert expecteded-pairs actual)))

                    :else ;; no change to node
                    zloc)]
         (if (z/end? zloc)
           zloc
           (recur (z/next zloc)))))
     z/root-string) )

(defn- add-dummy-assertion [test-body]
  (-> test-body
      z/of-string
      (#(or (z/up %) %))
      (z/append-child* (n/newlines 1))
      (z/append-child* (n/comment-node " test-doc-blocks dummy assertion to appease tools that fail on no assertions"))
      (z/append-child* (n/newlines 1))
      (z/append-child* (eval-assertion (n/string-node "dummy") (n/string-node "dummy")))
      (z/root-string)))

(defn to-test-body [prepared-doc-body]
  (let [test-body (convert-repl-assertions prepared-doc-body)]
    (if (= prepared-doc-body test-body)
      (add-dummy-assertion test-body)
      test-body)))

(comment
(z/right nil)


  (def s "hey

user=> (* 1 2)
2
user=> (* 5 6)
30

(+ 8 9)
=> 17
=stdout=> [\"bingo\"]
=stderr=> [\"error\"]
=clj=> 44
=cljs=> 99
;; that's it

")



  (-> "(hey joe)"
      z/of-string
      z/next
      z/next
      z/next
      z/end?)

  (add-dummy-assertion ";; booya")

  (println (to-test-body s))

  (println "---")
  )
