(ns lread.test-doc-blocks.impl.doc-parse-test
  (:require [babashka.fs :as fs]
            [clojure.string :as string]
            [clojure.test :refer [deftest testing is]]
            [lread.test-doc-blocks.impl.doc-parse :as sut])
  (:import [java.io File]))

;; TODO: Spot checks for now until/if I tangle apart parsing from evaluation

(defn temp-file [prefix ext lines]
  (let [f (File/createTempFile prefix ext)]
    (fs/delete-on-exit f)
    (spit (fs/file f) (string/join "\n" lines))
    (str f)))

(defn expected-test-ns [fname]
  (str (string/replace (fs/file-name fname) "." "-") "-test"))

(deftest parses-adoc
  (testing "no code blocks"
    (let [fname (temp-file "no-blocks" ".adoc" [""])]
      (is (= [] (sut/parse-file fname :cljc)))))
  (testing "a clojure adoc code block"
    (let [fname (temp-file "a-block" ".adoc"
                           ["[source,clojure]"
                            "----"
                            "some"
                            "clojure"
                            "----"])]
      (is (= [{:block-text "some\nclojure\n"
               :header nil
               :doc-filename fname
               :line-no 2
               :test-doc-blocks/test-ns (expected-test-ns fname)
               :test-doc-blocks/platform :cljc}]
             (sut/parse-file fname :cljc))))))

(deftest parses-md
  (testing "handles indented block"
    (let [fname (temp-file "indented" ".md"
                           ["# My heading"
                            ""
                            "Blocks can be indented under lists"
                            ""
                            "    ```Clojure"
                            "     some"
                            "      clojure"
                            "    here"
                            "   skew handled"
                            "    ```"])]
      (is (= [{:block-text " some\n  clojure\nhere\nskew handled\n"
               :header "My heading"
               :doc-filename fname
               :line-no 5
               :test-doc-blocks/test-ns (expected-test-ns fname)
               :test-doc-blocks/platform :cljs}]
             (sut/parse-file fname :cljs))))))

(deftest parses-clojure-docstrings
  (testing "blocks from docstrings"
    (let [fname (temp-file "sanity" ".cljc"
                           ["(ns my.ns.here"
                            "  \"preamble "
                            "   ```Clojure"
                            "   (+ 1 2 3)"
                            "   => 6"
                            "   ```\")"
                            ""
                            "(defn afn"
                            "  \"Some desc"
                            "   ```Clojure"
                            "   (+ 9 1)"
                            "   => 10"
                            "   ```\""
                            "  [])"
                            ""])]
      (is (= [{:block-text "(+ 1 2 3)\n=> 6\n"
               :header "namespace docstring"
               :doc-filename fname
               :line-no 3
               :test-doc-blocks/test-ns (expected-test-ns fname)
               :test-doc-blocks/platform :cljc}
              {:block-text "(+ 9 1)\n=> 10\n"
               :header "afn docstring"
               :doc-filename fname
               :line-no 10
               :test-doc-blocks/test-ns (expected-test-ns fname)
               :test-doc-blocks/platform :cljc}]
             (sut/parse-file fname :cljc))))))
