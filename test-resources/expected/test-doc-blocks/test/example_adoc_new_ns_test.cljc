(ns example-adoc-new-ns-test
  (:require clojure.test
            clojure.string
            #?(:cljs [lread.test-doc-blocks.runtime :include-macros])
            #?(:clj lread.test-doc-blocks.runtime)))

(clojure.test/deftest block-1
  (clojure.test/testing  "doc/example.adoc - line 132 - Specifying Test Namespace"
;; this code block will generate tests under example-adoc-new-ns-test

(clojure.test/is (= '8 (* 2 4)))))