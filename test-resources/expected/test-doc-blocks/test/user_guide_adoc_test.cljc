(ns user-guide-adoc-test
  (:require clojure.test
            clojure.string
            #?(:cljs [lread.test-doc-blocks.runtime :include-macros])
            #?(:clj lread.test-doc-blocks.runtime)))

(clojure.test/deftest block-0001
  (clojure.test/testing  "doc/01-user-guide.adoc - line 48 - Introduction"
(clojure.test/is (= '42 (* 6 7)))))

(clojure.test/deftest block-0002
  (clojure.test/testing  "doc/01-user-guide.adoc - line 57 - Introduction"
(clojure.test/is (= '42 (* 6 7)))))

(clojure.test/deftest block-0003
  (clojure.test/testing  "doc/01-user-guide.adoc - line 93 - Usage"
(clojure.test/is (= '42 (/ 714 17)))))

(defn test-ns-hook [] (block-0001) (block-0002) (block-0003))