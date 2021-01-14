(ns example-md-new-ns.ns1-test
  (:require clojure.test
            clojure.string
            #?(:cljs [lread.test-doc-blocks.runtime :include-macros])
            #?(:clj lread.test-doc-blocks.runtime)
            [clojure.string :as string]))

(clojure.test/deftest block-0001
  (clojure.test/testing  "doc/example.md - line 140 - Specifying Test Namespace"
;; this code block will generate tests under example-md-new-ns.ns1-test
(clojure.test/is (= '"1, 2, 3" (string/join ", " [1 2 3])))))

(defn test-ns-hook [] (block-0001))