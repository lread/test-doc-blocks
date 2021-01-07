(ns lread.test-doc-blocks.impl.test-body
  (:require [lread.test-doc-blocks.impl.zutil :as zutil]
            [rewrite-cljc.node :as n]
            [rewrite-cljc.zip :as z] ))

(defn- eval-assertion [expected]
  (n/list-node [(n/token-node 'clojure.test/is)
                (n/spaces 1)
                (n/list-node [(n/token-node '=)
                              (n/spaces 1)
                              (n/quote-node (z/node expected))
                              (n/spaces 1)
                              (n/token-node 'actual)])]))

(defn- platform-eval-assertion [platform expected]
  (n/reader-macro-node [(n/token-node '?)
                        (n/list-node [(n/keyword-node platform)
                                      (n/spaces 1)
                                      (eval-assertion expected)])]))

(defn- output-assertion [out-sym expected]
  (n/list-node [(n/token-node 'clojure.test/is)
                (n/spaces 1)
                (n/list-node [(n/token-node (symbol "="))
                              (n/spaces 1)
                              (z/node expected)
                              (n/spaces 1)
                              (n/list-node [(n/token-node 'clojure.string/split)
                                            (n/spaces 1)
                                            (n/token-node out-sym)
                                            (n/spaces 1)
                                            (n/regex-node "\\n")])])]))

(defn- interpose-nodes [items coll]
  (into [(first coll)]
        (mapcat #(concat items [%]) (rest coll))))

(defn- editor-assertions [expected-pairs]
  (->> (for [[atoken expected] expected-pairs]
         (case (z/sexpr atoken)
           => (eval-assertion expected)
           =clj=> (platform-eval-assertion :clj expected)
           =cljs=> (platform-eval-assertion :cljs expected)
           =stdout=> (output-assertion 'actual-out expected)
           =stderr=> (output-assertion 'actual-err expected)))
       (interpose-nodes [(n/newlines 1) (n/spaces 2)])))

(defn- assertion-token? [t]
  (some #{t} '(=> =stderr=> =stdout=> =clj=> =cljs=>)))

(defn token? [zloc token]
  (and (= :token (z/tag zloc))
       (= token (z/sexpr zloc))))

(defn- zassertion-token? [zloc]
  (and (= :token (z/tag zloc))
       (assertion-token? (z/sexpr zloc))))

(defn replace-editor-assert [zloc expected-pairs actual]
    (-> zloc
        ;; replace might be confusing, but we are replacing actual here
        (z/replace (n/list-node (concat [(n/token-node 'let)
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
                                        (editor-assertions expected-pairs))))
        ;; now we need to remove expected pairs
        (zutil/remove-n-siblings-right (* 2 (count expected-pairs)))))

(defn get-expected-pairs [zloc]
  (->> zloc
       (iterate z/right)
       (take-while identity)
       (partition 2)
       (take-while #(zassertion-token? (first %)))))

(defn convert-repl-assertions [prepared-doc-body]
 (-> (loop [zloc (z/of-string prepared-doc-body)]
                        (let [zloc (cond
                           (not zloc)
                           (throw (ex-info "unexpected error: zloc nil" {}))

                           (z/end? zloc)
                           (throw (ex-info "unexpected error: zloc end" {}))

                           ;; TODO: what if eof before complete?
                           ;; REPL style has 1 expectation
                           (token? zloc 'user=>)
                           (let [actual (z/right zloc)
                                 expected-pairs [[(z/of-string "=>") (z/right actual)]]]
                             (-> zloc
                                 (replace-editor-assert expected-pairs actual)))

                           ;; TODO: what if eof before complete?
                           ;; Editor style can have multiple expectations
                           (zassertion-token? zloc)
                           (let [actual (z/left zloc)
                                 expecteded-pairs (get-expected-pairs zloc) ]
                             (-> zloc
                                 z/left
                                 (replace-editor-assert expecteded-pairs actual)))

                           :else
                           zloc)]
                (if (z/right zloc)
                  (recur (z/right zloc))
                  zloc)))
            z/root-string) )

(defn add-dummy-assertion [test-body]
  (-> test-body
      z/of-string
      z/up
      (z/append-child (n/newlines 2))
      (z/append-child (n/comment-node " dummy assertion to appease tools fail on no assertions"))
      (z/append-child (n/newlines 1))
      (z/append-child (n/list-node [(n/token-node 'clojure.test/is)
                                    (n/spaces 1)
                                    (n/list-node [(n/token-node '=)
                                                  (n/spaces 1)
                                                  (n/string-node "dummy")
                                                  (n/spaces 1)
                                                  (n/string-node "dummy")])]))
      (z/root-string)))

(defn to-test-body [prepared-doc-body]
  (let [test-body (convert-repl-assertions prepared-doc-body)]
    (if (= prepared-doc-body test-body)
      (add-dummy-assertion test-body)
      test-body)))

(comment


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

  (-> s
      z/of-string
      z/right
      z/right
      z/node
      meta)


  (println (to-test-body s))

  (println "---")
  )
