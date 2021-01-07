(ns lread.test-doc-blocks
  "Parse code blocks from markdown and generate Clojure test code."
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [lread.test-doc-blocks.impl.doc-parse :as doc-parse]
            [lread.test-doc-blocks.impl.process :as process]
            [lread.test-doc-blocks.impl.test-write :as test-write])
  (:import [java.nio.file Files]))

(defn- delete-dir
  "Delete dir at `path` recursively.
  For safety, throws if there are any symbolic links found in file set to be deleted."
  [path]
  (let [flist (->>  (io/file path) file-seq reverse)
        sym-links (filter #(Files/isSymbolicLink (.toPath %)) flist)]
    (if (seq sym-links)
      (throw (ex-info (format "Won't delete %s, unsafe because it contains symbolic link(s): %s"
                              path (mapv str sym-links)) {}))
      (run! #(io/delete-file %) flist))))

(defn- print-table [parsed]
  (clojure.pprint/print-table [:doc-filename :line-no :header :test-doc-blocks/test-ns]
                              parsed))

(defn- report-on-found [parsed]
  (println "Will generate tests for Clojure doc blocks:")
  (print-table (remove :test-doc-blocks/skip parsed))
  (when (some :test-doc-blocks/skip parsed)
    (println "\nAnd, as requested, skipping:")
    (print-table (filter :test-doc-blocks/skip parsed))))

;;
;; Entry points
;;

(def default-opts
  {:target-root "./target"
   :docs ["README.md"]})

(defn gen-tests
  "Generate tests for code blocks found in markdown files.
  Invoke from clojure CLI with -X."
  [opts]
  (let [{:keys [target-root docs]} (merge default-opts opts )
        target-root (str (io/file target-root "test-doc-blocks"))]
    (when (.exists (io/file target-root))
      (delete-dir target-root))
    (let [target-root (str (io/file target-root "test"))
          parsed (mapcat doc-parse/parse-doc-code-blocks docs)]
     (report-on-found parsed)
     (println "\nGenerating tests to:" target-root)
      (->> parsed
           (process/convert-to-tests)
           (run! #(test-write/write-tests target-root %)))
      (println "Done"))))


(comment
  (gen-tests {:target-root "./target/"
              :docs ["doc/erp.adoc"
                     "README.adoc"
                     "doc/example.md"
                     "doc/example.adoc"]})

  (gen-tests {:docs ["README.adoc"]})

  )
