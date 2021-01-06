(ns example-adoc-new-ns-test
  (:require clojure.test
            clojure.string
            #?(:cljs [lread.test-doc-blocks.runtime :include-macros])
            #?(:clj lread.test-doc-blocks.runtime)))

(clojure.test/deftest block-1
  (clojure.test/testing  "doc/example.adoc - line 108 - Specifying Test Namespace"
;; this code block will generate tests under example-adoc-new-ns-test

(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (* 2 4))]
  (clojure.test/is (= '8 actual))) 

))