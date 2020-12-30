(ns test-doc-blocks.gen.readme-adoc-test
  (:require #?(:clj [lread.test-doc-blocks.runtime :refer [deftest-doc-blocks testing-block]]
               :cljs [lread.test-doc-blocks.runtime :refer-macros [deftest-doc-blocks testing-block]])))

(deftest-doc-blocks

(testing-block  "README.adoc - Usage - line 91"
user=> (/ 714 17)
"42"
))
