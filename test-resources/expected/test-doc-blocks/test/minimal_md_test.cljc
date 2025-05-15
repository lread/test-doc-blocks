(ns minimal-md-test
  (:require clojure.test
            clojure.string
            #?(:cljs [lread.test-doc-blocks.runtime :include-macros true])
            #?(:clj lread.test-doc-blocks.runtime)))

(clojure.test/deftest block-0001
  (clojure.test/testing  "test-resources/doc/minimal.md - line 1"
(clojure.test/is (= '3 (+ 1 2)))))

(defn test-ns-hook [] (block-0001))