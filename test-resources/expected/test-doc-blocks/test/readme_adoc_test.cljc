(ns readme-adoc-test
  (:require clojure.test
            clojure.string
            #?(:cljs [lread.test-doc-blocks.runtime :include-macros])
            #?(:clj lread.test-doc-blocks.runtime)))

(clojure.test/deftest block-0001
  (clojure.test/testing  "README.adoc - line 45 - Rationale"
(clojure.test/is (= '42 (* 6 7)))))

(clojure.test/deftest block-0002
  (clojure.test/testing  "README.adoc - line 54 - Rationale"
(clojure.test/is (= '42 (* 6 7)))))

(clojure.test/deftest block-0003
  (clojure.test/testing  "README.adoc - line 112 - Usage"
(clojure.test/is (= '42 (/ 714 17)))))

(defn test-ns-hook [] (block-0001) (block-0002) (block-0003))