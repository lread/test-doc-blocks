(ns example-adoc-test
  (:require clojure.test
            clojure.string
            #?(:cljs [lread.test-doc-blocks.runtime :include-macros])
            #?(:clj lread.test-doc-blocks.runtime)
            [clojure.string :as string]
            [clojure.set :as set]))

(clojure.test/deftest block-1
  (clojure.test/testing  "doc/example.adoc - line 13 - The Basics"
;; test-doc-block will generate an assertion to verify (+ 1 2 3) evaluates to the expected 6
(clojure.test/is (= '6 (+ 1 2 3)))))

(clojure.test/deftest block-2
  (clojure.test/testing  "doc/example.adoc - line 22 - The Basics"
;; test-doc-blocks will generate an assertion to verify (+ 1 2 3 4) evaluates to the expected 10
(clojure.test/is (= '10 (+ 1 2 3 4)))
;; it understands that Clojure and ClojureScript can evaluate differently
#?(:clj (clojure.test/is (= '\C \C)))
#?(:cljs (clojure.test/is (= '"C" \C)))
;; and verifies, when asked, to check what was written to stdout
(println "hey there!")
;; stdout=> hey there!

;; multiple stdout lines can be verified like so (notice the single ;):
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (println "is this right?\nor not?"))]
  (clojure.test/is (= ["is this right?" "or not?"] (clojure.string/split-lines actual-out))));; sometimes we might care about evaluated result, stderr and stdout
;; TODO: reader conditionals makes this example awkward
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (do
  (println "To out I go")
  #?(:clj
     (binding [*out* *err*]
       (println "To err is human"))
     :cljs
     (binding [*print-fn* *print-err-fn*]
       (println "To err is human")))
  (* 9 9)))]
  (clojure.test/is (= '81 actual))
  (clojure.test/is (= ["To err is human"] (clojure.string/split-lines actual-err)))
  (clojure.test/is (= ["To out I go"] (clojure.string/split-lines actual-out))))))

(clojure.test/deftest block-3
  (clojure.test/testing  "doc/example.adoc - line 62 - The Basics"
(->> "here we are only checking that our code will run"
     reverse
     reverse
     (apply str))

; test-doc-blocks dummy assertion to appease tools that fail on no assertions
(clojure.test/is (= '"dummy" "dummy"))))

(clojure.test/deftest block-4
  (clojure.test/testing  "doc/example.adoc - line 207 - Section Titles"
(clojure.test/is (= '"well!how!about!that" (string/join "!" ["well" "how" "about" "that"])))))

(clojure.test/deftest block-5
  (clojure.test/testing  "doc/example.adoc - line 223 - Support for CommonMark Code Block Syntax"
(clojure.test/is (= '{1 :a, 2 :b} (set/map-invert {:a 1 :b 2})))))