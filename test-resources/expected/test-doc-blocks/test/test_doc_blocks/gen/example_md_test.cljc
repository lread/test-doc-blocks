(ns test-doc-blocks.gen.example-md-test
  (:require #?(:clj [lread.test-doc-blocks.runtime :refer [deftest-doc-blocks testing-block]]
               :cljs [lread.test-doc-blocks.runtime :refer-macros [deftest-doc-blocks testing-block]])))

(deftest-doc-blocks

(testing-block  "doc/example.md - The Basics - line 11"
;; test-doc-block will generate an assertion to verify (+ 1 2 3) evaluates to the expected 6
user=> (+ 1 2 3)
"6"
)

(testing-block  "doc/example.md - The Basics - line 18"
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

;; sometimes we might care about evaluated result, stderr and stdout
;; TODO: reader conditionals makes this example awkward
(do
  (println "To out I go")
  #?(:clj
     (binding [*out* *err*]
       (println "To err is human"))
     :cljs
     (binding [*print-fn* *print-err-fn*]
       (println "To err is human")))
  (* 9 9))
=>"81"
=stderr=> ["To err is human"]
=stdout=> ["To out I go"]

;; finish your stdout block with an empty or non-comment line
)

(testing-block  "doc/example.md - The Basics - line 57"
(->> "here we are only checking that our code will run"
     reverse
     reverse
     (apply str))
))
