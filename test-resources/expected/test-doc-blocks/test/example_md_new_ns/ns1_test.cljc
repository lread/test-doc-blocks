(ns example-md-new-ns.ns1-test
  (:require clojure.test
            clojure.string
            #?(:cljs [lread.test-doc-blocks.runtime :include-macros])
            #?(:clj lread.test-doc-blocks.runtime)
            [clojure.string :as string]))

(clojure.test/deftest block-1
  (clojure.test/testing  "doc/example.md - line 127 - Specifying Test Namespace"
;; this code block will generate tests under example-md-new-ns.ns1-test
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (string/join ", " [1 2 3]))]
  (clojure.test/is (= '"1, 2, 3" actual)))

))