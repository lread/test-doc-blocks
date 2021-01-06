(ns lread.test-doc-blocks
  "Parse code blocks from markdown and generate Clojure test code."
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as string]
            [lread.test-doc-blocks.impl.doc-parse :as doc-parse]
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

(defn- testing-text [{:keys [doc-filename line-no header]}]
  (string/join " - " (keep identity [doc-filename (str "line " line-no) header])))

(defn- expand-refs [refs]
  {:common (-> refs :common vec sort)
   :reader-cond (->> (dissoc refs :common)
                     (map (fn [[k v]] [k v]))
                     (sort-by first)
                     (mapcat (fn [[platform elems]] (concat [platform] (sort-by str elems)))))})


(defn- str-reader-cond [l]
  (when (seq l)
    [(str "#?(" (string/join " " (map str l)) ")")]))

(defn- ns-declaration [test-ns {:keys [requires imports] :as _ns-ref} ]
  (let [test-doc-blocks-refs ['clojure.test
                              'clojure.string
                              "#?(:cljs [lread.test-doc-blocks.runtime :include-macros])"
                              "#?(:clj lread.test-doc-blocks.runtime)"]
        requires (expand-refs requires)
        imports (expand-refs imports)]
    (str "(ns " test-ns  "\n"
         "  (:require " (->> []
                             (into (:common requires))
                             (into (str-reader-cond (:reader-cond requires)))
                             (into test-doc-blocks-refs)
                             (string/join "\n            ")) ")"
         (when (or (seq (:common imports)) (seq (:reader-cond imports)))
           (str "\n  (:import " (->> []
                                     (into (:common imports))
                                     (into (str-reader-cond (:reader-cond imports)))
                                     (string/join "\n    ")) ")"))
         ")\n")))

(defn- test-defs [tests]
  (str (->> (reduce (fn [acc t]
                      (conj acc (str
                                 "(clojure.test/deftest block-" (inc (count acc)) "\n"
                                 "  (clojure.test/testing " " \"" (testing-text t) "\"\n"
                                 (:test-body t) "))")))
                    []
                    tests)
            (string/join "\n\n"))))

(defn- write-tests
  "Write out `tests` to test namespace `test-ns` under dir `target-root`"
  [target-root {:keys [test-ns ns-refs tests]}]
  (let [test-fname (io/file target-root (fname-for-ns test-ns))]
    (io/make-parents test-fname)
    (spit test-fname (str (ns-declaration test-ns ns-refs) "\n"
                          (test-defs tests)))))

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
           (run! #(write-tests target-root %)))
      (println "Done"))))


(comment
  (gen-tests {:target-root "./target/"
              :docs ["doc/erp.adoc"
                     "README.adoc"
                     "doc/example.md"
                     "doc/example.adoc"]})

  (gen-tests {:docs ["README.adoc"]})

  )
