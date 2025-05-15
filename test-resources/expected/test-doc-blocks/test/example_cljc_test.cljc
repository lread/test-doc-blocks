(ns example-cljc-test
  (:require clojure.test
            clojure.string
            #?(:cljs [lread.test-doc-blocks.runtime :include-macros true])
            #?(:clj lread.test-doc-blocks.runtime)
            [clojure.string :as string]))

(clojure.test/deftest block-0001
  (clojure.test/testing  "doc/example.cljc - line 11 - namespace docstring"
(clojure.test/is (= '6 (+ 1 2 3)))))

(clojure.test/deftest block-0002
  (clojure.test/testing  "doc/example.cljc - line 18 - namespace docstring"
nil

(clojure.test/is (= '"who,who" (string/replace "what,what" "what" "who")))))

(clojure.test/deftest block-0003
  (clojure.test/testing  "doc/example.cljc - line 31 - fn2 docstring"
(clojure.test/is (= '64 (* 8 8)))))

(clojure.test/deftest block-0004
  (clojure.test/testing  "doc/example.cljc - line 53 - fn4 docstring"
(clojure.test/is (= '"ALL \"GOOD\"?" (string/upper-case "all \"good\"?")))))

(defn test-ns-hook [] (block-0001) (block-0002) (block-0003) (block-0004))