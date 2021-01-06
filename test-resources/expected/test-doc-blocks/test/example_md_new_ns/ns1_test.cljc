(ns example-md-new-ns.ns1-test
  (:require clojure.test
            clojure.string
            #?(:cljs [lread.test-doc-blocks.runtime :include-macros])
            #?(:clj lread.test-doc-blocks.runtime)
            [clojure.string :as string]))

(clojure.test/deftest block-1
  (clojure.test/testing  "doc/example.md - line 113 - Specifying Test Namespace"
;; this code block will generate tests under example-md-new-ns.ns1-test
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (string/join ", " [1 2 3]))]
  (clojure.test/is (= '"1, 2, 3" actual)))

))

(clojure.test/deftest block-2
  (clojure.test/testing  "doc/example.md - line 130 - Section Titles"
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (string/join "!" ["well" "how" "about" "that"]))]
  (clojure.test/is (= '"well!how!about!that" actual)))

))

(clojure.test/deftest block-3
  (clojure.test/testing  "doc/example.md - line 145 - Indented Blocks"
;; we handle simple cases a-OK.
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (+ 1 2 3))]
  (clojure.test/is (= '6 actual))) 

))

(clojure.test/deftest block-4
  (clojure.test/testing  "doc/example.md - line 153 - Indented Blocks"
;; we handle indented wrapped strings just fine
(def s "my
goodness
gracious")

(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (println s))]
  (clojure.test/is (= ["my" "goodness" "gracious"] (clojure.string/split actual-out #"\n"))))

))