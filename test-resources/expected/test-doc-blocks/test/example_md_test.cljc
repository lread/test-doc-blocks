(ns example-md-test
  (:require clojure.test
            clojure.string
            #?(:cljs [lread.test-doc-blocks.runtime :include-macros])
            #?(:clj lread.test-doc-blocks.runtime)
            [clojure.string :as string]))

(clojure.test/deftest block-1
  (clojure.test/testing  "doc/example.md - line 13 - The Basics"
;; test-doc-block will generate an assertion to verify (+ 1 2 3) evaluates to the expected 6
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (+ 1 2 3))]
  (clojure.test/is (= '6 actual))) 

))

(clojure.test/deftest block-2
  (clojure.test/testing  "doc/example.md - line 20 - The Basics"
;; test-doc-blocks will generate an assertion to verify (+ 1 2 3 4) evaluates to the expected 10
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (+ 1 2 3 4))]
  (clojure.test/is (= '10 actual)))


;; it understands that Clojure and ClojureScript can evaluate differently
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture \C)]
  #?(:clj (clojure.test/is (= '\C actual)))
  #?(:cljs (clojure.test/is (= '"C" actual))))



;; and verifies, when asked, to check what was written to stdout
(println "hey there!")
;; stdout=> hey there!

;; multiple stdout lines can be verified like so (notice the single ;):
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (println "is this right?\nor not?"))]
  (clojure.test/is (= ["is this right?" "or not?"] (clojure.string/split actual-out #"\n"))))


;; sometimes we might care about evaluated result, stderr and stdout
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
  (clojure.test/is (= ["To err is human"] (clojure.string/split actual-err #"\n")))
  (clojure.test/is (= ["To out I go"] (clojure.string/split actual-out #"\n"))))



))

(clojure.test/deftest block-3
  (clojure.test/testing  "doc/example.md - line 59 - The Basics"
(->> "here we are only checking that our code will run"
     reverse
     reverse
     (apply str))


; dummy assertion to appease tools fail on no assertions 
(clojure.test/is (= "dummy" "dummy"))))

(clojure.test/deftest block-4
  (clojure.test/testing  "doc/example.md - line 177 - Section Titles"
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (string/join "!" ["well" "how" "about" "that"]))]
  (clojure.test/is (= '"well!how!about!that" actual)))

))

(clojure.test/deftest block-5
  (clojure.test/testing  "doc/example.md - line 192 - Indented Blocks"
;; we handle simple cases a-OK.
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (+ 1 2 3))]
  (clojure.test/is (= '6 actual))) 

))

(clojure.test/deftest block-6
  (clojure.test/testing  "doc/example.md - line 200 - Indented Blocks"
;; we handle indented wrapped strings just fine
(def s "my
goodness
gracious")

(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (println s))]
  (clojure.test/is (= ["my" "goodness" "gracious"] (clojure.string/split actual-out #"\n"))))

))