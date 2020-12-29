(ns test-doc-blocks.gen.example-adoc-new-ns.ns1-test
  (:require [clojure.set :as set]
            [clojure.string :as string]
            #?(:clj [lread.test-doc-blocks.runtime :refer [deftest-doc-blocks testing-block]]
               :cljs [lread.test-doc-blocks.runtime :refer-macros [deftest-doc-blocks testing-block]])))

(deftest-doc-blocks

(testing-block  "doc/example.adoc - Specifying Test Namespace - line 109"

;; this code block will generate tests under example-adoc-new-ns.ns1-test


(string/join ", " [1 2 3])
=>"\"1, 2, 3\""
)

(testing-block  "doc/example.adoc - Section Titles - line 145"


(string/join "!" ["well" "how" "about" "that"])
=>"\"well!how!about!that\""
)

(testing-block  "doc/example.adoc - Support for CommonMark Code BlockSyntax - line 161"


(set/map-invert {:a 1 :b 2})
=>"{1 :a, 2 :b}"
))
