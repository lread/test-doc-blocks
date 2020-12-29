(ns test-doc-blocks.gen.example-adoc-test
  (:require #?(:clj [lread.test-doc-blocks.runtime :refer [deftest-doc-blocks testing-block]]
               :cljs [lread.test-doc-blocks.runtime :refer-macros [deftest-doc-blocks testing-block]])))

(deftest-doc-blocks

(testing-block  "doc/example.adoc - The Basics - line 13"

;; test-doc-block will generate an assertion to verify (+ 1 2 3) evaluates to the expected 6
user=> (+ 1 2 3)
"6"
)

(testing-block  "doc/example.adoc - The Basics - line 22"

;; test-doc-blocks will generate an assertion to verify (+ 1 2 3 4) evaluates to the expected 10
(+ 1 2 3 4)
=>"10"

;; it understands that Clojure and ClojureScript can evaluate differently
\C
=clj=>"\\C"
=cljs=>"\"C\""

;; and verifies, when asked, to check what was written to stdout
(println "is this right?\nor not?")
=stdout=> ["is this right?" "or not?"]

;; finish your stdout block with an empty or non-comment line
)

(testing-block  "doc/example.adoc - The Basics - line 45"

(->> "here we are only checking that our code will run"
     reverse
     reverse
     (apply str))
))
