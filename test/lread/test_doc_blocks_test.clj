(ns ^:integration lread.test-doc-blocks-test
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.test :refer [deftest is]]
   [lread.test-doc-blocks :as sut]))

(defn- slurp-to-lines [dir filename]
  (string/split (slurp (io/file dir filename)) #"\n"))

(deftest verify-generation-against-known-good-run

  (sut/gen-tests {:docs ["README.adoc"
                         "doc/example.adoc"
                         "doc/example.md"]
                  :target-root "target/actual"})

  (let [expected-dir "test-resources/expected/test-doc-blocks/test/"
        actual-dir "target/actual/test-doc-blocks/test/"
        files (->> expected-dir
                   io/file
                   file-seq
                   (filter #(.isFile %))
                   (map #(string/replace (str %) expected-dir "")))]
    ;; sanity check files count
    (is (= 14 (count files)))
    (run!
     (fn [filename]
       ;; kaocha displays diffs beautifully for seqs of strings
       (is (= (slurp-to-lines expected-dir filename)
              (slurp-to-lines actual-dir filename))))
     files)))
