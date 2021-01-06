(ns example-adoc-new-ns.ns1-test
  (:require clojure.test
            clojure.string
            #?(:cljs [lread.test-doc-blocks.runtime :include-macros])
            #?(:clj lread.test-doc-blocks.runtime)
            [clojure.string :as string]
            [clojure.set :as set]))

(clojure.test/deftest block-1
  (clojure.test/testing  "doc/example.adoc - line 127 - Specifying Test Namespace"
;; this code block will generate tests under example-adoc-new-ns.ns1-test
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (string/join ", " [1 2 3]))]
  (clojure.test/is (= '"1, 2, 3" actual)))

))

(clojure.test/deftest block-2
  (clojure.test/testing  "doc/example.adoc - line 163 - Section Titles"
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (string/join "!" ["well" "how" "about" "that"]))]
  (clojure.test/is (= '"well!how!about!that" actual)))

))

(clojure.test/deftest block-3
  (clojure.test/testing  "doc/example.adoc - line 179 - Support for CommonMark Code Block Syntax"
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (set/map-invert {:a 1 :b 2}))]
  (clojure.test/is (= '{1 :a, 2 :b} actual)))

))