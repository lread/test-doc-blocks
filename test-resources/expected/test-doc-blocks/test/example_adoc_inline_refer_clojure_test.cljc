(ns example-adoc-inline-refer-clojure-test
  (:refer-clojure :exclude [for read-string])
  (:require clojure.test
            clojure.string
            #?(:cljs [lread.test-doc-blocks.runtime :include-macros])
            #?(:clj lread.test-doc-blocks.runtime)
            [clojure.edn :refer [read-string]]))

(clojure.test/deftest block-0001
  (clojure.test/testing  "doc/example.adoc - line 503 - Inline refer-clojure"
;; a contrived example that uses uses clojure.edn/read-string in place
;; of clojure.core/read-string and excludes clojure.core/for
nil
nil

;; our own for
(defn for [x]
  (* 4 x))

(clojure.test/is (= '"clojure.edn" (-> #'read-string meta :ns ns-name str)))
(clojure.test/is (= '[1 2 3] (read-string "[1 2 3]")))
(clojure.test/is (= '16 (for 4)))))

(defn test-ns-hook [] (block-0001))