(ns minimal-adoc-test
  (:require clojure.test
            clojure.string
            #?(:cljs [lread.test-doc-blocks.runtime :include-macros])
            #?(:clj lread.test-doc-blocks.runtime)))

(clojure.test/deftest block-0001
  (clojure.test/testing  "test-resources/doc/minimal.adoc - line 2"
(clojure.test/is (= '3 (+ 1 2)))))

(defn test-ns-hook [] (block-0001))