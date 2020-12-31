(ns lread.test-doc-blocks
  "Parse code blocks from markdown and generate Clojure test code."
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as string]
            [lread.test-doc-blocks.impl.parse :as parse]
            [lread.test-doc-blocks.impl.process :as process])
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

(defn- fname-for-ns
  "Converts `ns-name` to a cljc file path."
  [ns-name]
  (-> ns-name
      (string/replace "-" "_")
      (string/replace "." "/")
      (str ".cljc")))

(defn- testing-text [{:keys [doc-filename header line-no]}]
  (string/join " - " (keep identity [doc-filename header (str "line " line-no)])))


(defn- write-tests
  "Write out `tests` to test namespace `test-ns` under dir `target-root`"
  [target-root {:keys [test-ns ns-refs tests]}]
  (let [ns-name (str "test-doc-blocks.gen" "." test-ns)
        test-fname (io/file target-root (fname-for-ns ns-name))
        test-doc-blocks-refs ["#?(:clj [lread.test-doc-blocks.runtime :refer [deftest-doc-blocks testing-block]]"
                              "   :cljs [lread.test-doc-blocks.runtime :refer-macros [deftest-doc-blocks testing-block]])"]]
    (io/make-parents test-fname)
    (spit test-fname
          (str "(ns " ns-name  "\n"
               "  (:require " (-> (into [] ns-refs)
                                  (into test-doc-blocks-refs)
                                  (->> (string/join "\n            "))) "))\n"
               "\n"
               "(deftest-doc-blocks\n"
               "\n"
               (->> (reduce (fn [acc t]
                              (conj acc (str
                                         "(testing-block " " \"" (testing-text t) "\"\n"
                                         (:test-body t) ")")))
                            []
                            tests)
                    (string/join "\n\n"))
               ")\n"))))

(defn- print-table [parsed]
  (clojure.pprint/print-table [:doc-filename :line-no :header :test-doc-blocks/test-ns]
                              parsed))

(defn- report-found [parsed]
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
          parsed (mapcat parse/parse-doc-code-blocks docs)]
     (report-found parsed)
     (println "\nGenerating tests to:" target-root)
      (->> parsed
           (process/convert-to-tests)
           (run! #(write-tests target-root %)))
      (println "Done"))))


(comment
  (gen-tests {:target-root "./target/"
              :docs ["README.adoc"
                     "doc/example.md"
                     "doc/example.adoc"]})

  (gen-tests {:docs ["README.adoc"]})

  )
