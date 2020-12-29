(ns test-doc-blocks.gen.example-md-new-ns-test
  (:require #?(:clj [lread.test-doc-blocks.runtime :refer [deftest-doc-blocks testing-block]]
               :cljs [lread.test-doc-blocks.runtime :refer-macros [deftest-doc-blocks testing-block]])))

(deftest-doc-blocks

(testing-block  "doc/example.md - Specifying Test Namespace - line 79"
;; this code block will generate tests under example-md-new-ns-test

user=> (* 2 4)
"8"
))
