(ns lread.test-doc-blocks.impl.doc-parse-test
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.test :refer [deftest testing is]]
            [lread.test-doc-blocks.impl.doc-parse :as sut]))

;; TODO: Spot checks for now until/if I tangle apart parsing from evaluation

(defn rdr[svec]
  (io/reader (char-array
              (string/join "\n" svec))))

(deftest parses-adoc
  (testing "no code blocks"
    (is (= [] (sut/parse-doc-code-blocks "my-doc.adoc" :cljc (rdr [""])))))
  (testing "an clojure adoc code block"
    (is (= [{:block-text "some\nclojure\n"
             :header nil
             :doc-filename "my-doc.adoc"
             :line-no 2
             :test-doc-blocks/test-ns "my-doc-adoc-test"
             :test-doc-blocks/platform :cljc}]
           (sut/parse-doc-code-blocks "my-doc.adoc" :cljc
                                      (rdr ["[source,clojure]"
                                            "----"
                                            "some"
                                            "clojure"
                                            "----"]))))))

(deftest parses-md
  (testing "handles indented block"
    (is (= [{:block-text " some\n  clojure\nhere\nskew handled\n"
             :header "My heading"
             :doc-filename "my-doc.md"
             :line-no 5
             :test-doc-blocks/test-ns "my-doc-md-test"
             :test-doc-blocks/platform :cljs}]
           (sut/parse-doc-code-blocks "my-doc.md" :cljs
                                      (rdr ["# My heading"
                                            ""
                                            "Blocks can be indented under lists"
                                            ""
                                            "    ```Clojure"
                                            "     some"
                                            "      clojure"
                                            "    here"
                                            "   skew handled"
                                            "    ```"]))))))
