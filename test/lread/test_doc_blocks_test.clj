(ns ^:integration lread.test-doc-blocks-test
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.test :refer [deftest is]]
   [lread.test-doc-blocks :as sut]))

(defn- slurp-to-lines [dir filename]
  (string/split (slurp (io/file dir filename)) #"\n"))

(defn- files-under [dir]
  (->> dir
       io/file
       file-seq
       (filter #(.isFile %))
       (map #(string/replace (str %) dir ""))))

(deftest verify-generation-against-known-good-run

  (sut/gen-tests {:docs ["README.adoc"
                         "doc/example.adoc"
                         "doc/example.md"]
                  :target-root "target/actual"})

  (let [expected-dir "test-resources/expected/test-doc-blocks/test/"
        actual-dir "target/actual/test-doc-blocks/test/"
        expected-files (files-under expected-dir)]
    ;; compare contents
    (run!
     (fn [filename]
       ;; kaocha displays diffs beautifully for seqs of strings
       (is (= (slurp-to-lines expected-dir filename)
              (slurp-to-lines actual-dir filename))))
     expected-files)
    ;; compare filenames
    (is (= expected-files (files-under actual-dir)) "generated test files")))
