(ns example-adoc-test
  (:require clojure.test
            clojure.string
            #?(:cljs [lread.test-doc-blocks.runtime :include-macros])
            #?(:clj lread.test-doc-blocks.runtime)
            [clojure.string :as string]
            [clojure.set :as set]))

(clojure.test/deftest block-0001
  (clojure.test/testing  "doc/example.adoc - line 13 - The Basics"
;; test-doc-block will generate an assertion to verify (+ 1 2 3) evaluates to the expected 6
(clojure.test/is (= '6 (+ 1 2 3)))))

(clojure.test/deftest block-0002
  (clojure.test/testing  "doc/example.adoc - line 22 - The Basics"
;; test-doc-blocks will generate an assertion to verify (+ 1 2 3 4) evaluates to the expected 10
(clojure.test/is (= '10 (+ 1 2 3 4)))
;; it understands that Clojure and ClojureScript can evaluate differently
#?(:clj (clojure.test/is (= '\C \C)))
#?(:cljs (clojure.test/is (= '"C" \C)))
;; and verifies, when asked, to check what was written to stdout
(println "hey there!")
;; stdout=> hey there!

;; multiple stdout lines can be verified like so (notice the use single of ;):
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (println "is this right?\nor not?"))]
  (clojure.test/is (= ["is this right?" "or not?"] (clojure.string/split-lines actual-out))))))

(clojure.test/deftest block-0003
  (clojure.test/testing  "doc/example.adoc - line 75 - The Basics"
(->> "here we are only checking that our code will run"
     reverse
     reverse
     (apply str))

; test-doc-blocks dummy assertion to appease tools that fail on no assertions
(clojure.test/is (= '"dummy" "dummy"))))

(clojure.test/deftest ^:testing-meta123 block-0004
  (clojure.test/testing  "doc/example.adoc - line 211 - Specifying Metadata"
;; this code block will generate a test with metadata {:testing-meta123 true}

(clojure.test/is (= '[[:a 1]] (into [] {:a 1})))))

(clojure.test/deftest ^{:testing-meta123 "a-specific-value", :testing-meta789 :yip} block-0005
  (clojure.test/testing  "doc/example.adoc - line 224 - Specifying Metadata"
;; this code block will generate a test with metadata:
;;  {:testing-meta123 "a-specific-value" :testing-meta789 :yip}

(clojure.test/is (= '"!oh!my!goodness" (reduce
   (fn [acc n]
     (str acc "!" n))
   ""
   ["oh" "my" "goodness"])))))

(clojure.test/deftest block-0006
  (clojure.test/testing  "doc/example.adoc - line 264 - Section Titles"
(clojure.test/is (= '"well!how!about!that" (string/join "!" ["well" "how" "about" "that"])))))

(clojure.test/deftest block-0007
  (clojure.test/testing  "doc/example.adoc - line 280 - Support for CommonMark Code Block Syntax"
(clojure.test/is (= '{1 :a, 2 :b} (set/map-invert {:a 1 :b 2})))))

(clojure.test/deftest block-0008
  (clojure.test/testing  "doc/example.adoc - line 329 - Test Run Order"
(def var-block1 (+ 1 2 3))

; test-doc-blocks dummy assertion to appease tools that fail on no assertions
(clojure.test/is (= '"dummy" "dummy"))))

(clojure.test/deftest block-0009
  (clojure.test/testing  "doc/example.adoc - line 335 - Test Run Order"
(def var-block2 (+ 4 5 6))

(clojure.test/is (= '21 (+ var-block1 var-block2)))))

(clojure.test/deftest block-0010
  (clojure.test/testing  "doc/example.adoc - line 344 - Test Run Order"
(clojure.test/is (= '100 (+ var-block1 var-block2 79)))))

(defn test-ns-hook [] (block-0001) (block-0002) (block-0003) (block-0004) (block-0005) (block-0006) (block-0007) (block-0008) (block-0009) (block-0010))