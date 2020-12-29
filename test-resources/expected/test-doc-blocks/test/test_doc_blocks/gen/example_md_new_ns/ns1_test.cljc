(ns test-doc-blocks.gen.example-md-new-ns.ns1-test
  (:require [clojure.string :as string]
            #?(:clj [lread.test-doc-blocks.runtime :refer [deftest-doc-blocks testing-block]]
               :cljs [lread.test-doc-blocks.runtime :refer-macros [deftest-doc-blocks testing-block]])))

(deftest-doc-blocks

(testing-block  "doc/example.md - Specifying Test Namespace - line 96"
;; this code block will generate tests under example-md-new-ns.ns1-test



(string/join ", " [1 2 3])
=>"\"1, 2, 3\""
)

(testing-block  "doc/example.md - Section Titles - line 113"


(string/join "!" ["well" "how" "about" "that"])
=>"\"well!how!about!that\""
)

(testing-block  "doc/example.md - Indented Blocks - line 128"
;; we handle simple cases a-OK.
user=> (+ 1 2 3)
"6"
)

(testing-block  "doc/example.md - Indented Blocks - line 136"
;; we handle indented wrapped strings just fine
(def s "my
goodness
gracious")

(println s)
=stdout=> ["my" "goodness" "gracious"]
))
