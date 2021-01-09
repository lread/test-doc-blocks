(ns readme-adoc-test
  (:require clojure.test
            clojure.string
            #?(:cljs [lread.test-doc-blocks.runtime :include-macros])
            #?(:clj lread.test-doc-blocks.runtime)))

(clojure.test/deftest block-1
  (clojure.test/testing  "README.adoc - line 43 - Rationale"
(clojure.test/is (= '42 (* 6 7)))))

(clojure.test/deftest block-2
  (clojure.test/testing  "README.adoc - line 52 - Rationale"
(clojure.test/is (= '42 (* 6 7)))))

(clojure.test/deftest block-3
  (clojure.test/testing  "README.adoc - line 119 - Usage"
(clojure.test/is (= '42 (/ 714 17)))))