(ns readme-adoc-test
  (:require clojure.test
            clojure.string
            #?(:cljs [lread.test-doc-blocks.runtime :include-macros])
            #?(:clj lread.test-doc-blocks.runtime)))

(clojure.test/deftest block-1
  (clojure.test/testing  "README.adoc - line 119 - Usage"
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (/ 714 17))]
  (clojure.test/is (= '42 actual))) 

))