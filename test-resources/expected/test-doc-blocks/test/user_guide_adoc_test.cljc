(ns user-guide-adoc-test
  (:require clojure.test
            clojure.string
            #?(:cljs [lread.test-doc-blocks.runtime :include-macros true])
            #?(:clj lread.test-doc-blocks.runtime)))

(clojure.test/deftest block-0001
  (clojure.test/testing  "doc/01-user-guide.adoc - line 50 - Introduction"
(clojure.test/is (= '42 (* 6 7)))))

(clojure.test/deftest block-0002
  (clojure.test/testing  "doc/01-user-guide.adoc - line 59 - Introduction"
(clojure.test/is (= '42 (* 6 7)))))

(defn test-ns-hook [] (block-0001) (block-0002))