(ns readme-adoc-test
  (:require clojure.test
            clojure.string
            #?(:cljs [lread.test-doc-blocks.runtime :include-macros])
            #?(:clj lread.test-doc-blocks.runtime)))

(clojure.test/deftest block-1
  (clojure.test/testing  "README.adoc - line 44 - Rationale"
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (* 6 7))]
  (clojure.test/is (= '42 actual))) 

))

(clojure.test/deftest block-2
  (clojure.test/testing  "README.adoc - line 53 - Rationale"
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (* 6 7))]
  (clojure.test/is (= '42 actual)))

))

(clojure.test/deftest block-3
  (clojure.test/testing  "README.adoc - line 121 - Usage"
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (/ 714 17))]
  (clojure.test/is (= '42 actual))) 

))