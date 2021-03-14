(ns lread.test-doc-blocks
  "Parse code blocks from markdown and generate Clojure test code."
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [lread.test-doc-blocks.impl.doc-parse :as doc-parse]
            [lread.test-doc-blocks.impl.process :as process]
            [lread.test-doc-blocks.impl.test-write :as test-write]
            [lread.test-doc-blocks.impl.validate :as validate])
  (:import [java.nio.file Files]))

(def windows? (-> (System/getProperty "os.name")
                  (string/lower-case)
                  (string/includes? "win")))

(defn- delete-dir!
  "Delete dir at `path` recursively.
  For safety, throws if there are any symbolic links found in file set to be deleted."
  [path]
  (let [flist (->>  (io/file path) file-seq reverse)
        sym-links (filter #(Files/isSymbolicLink (.toPath %)) flist)]
    (if (seq sym-links)
      (throw (ex-info (format "Won't delete %s, unsafe because it contains symbolic link(s): %s"
                              path (mapv str sym-links)) {}))
      (run! #(io/delete-file %) flist))))

(defn- indent [s indent-cnt]
  (string/replace s #"(^|\R)" (str "$1" (apply str (repeat indent-cnt " ")))))

(defn- print-found! [parsed indent-cnt]
  (let [fnames (->> parsed (group-by :doc-filename) (into []) (sort-by first))]
    (doseq [[fname headers] fnames]
      (println (indent fname indent-cnt))
      (doseq [[header lines] (->> headers (group-by :header) (into []) (sort-by #(-> % second first :line-no)))]
        (println (indent header (+ 2 indent-cnt)))
        (doseq [line lines]
          (println (-> (format "%03d: %s" (:line-no line) (:test-doc-blocks/test-ns line))
                       (indent (+ 4 indent-cnt)))))))))

(defn- report-on-found! [parsed]
  (println "Will generate tests for following Clojure doc blocks:")
  (println "- under each found heading is listed <lineno>: <target test namespace>")
  (let [by-platform (->> parsed
                         (remove :test-doc-blocks/skip)
                         (group-by :test-doc-blocks/platform)
                         (into [])
                         (sort-by first))]
    (if (seq by-platform)
      (doseq [[platform blocks] by-platform]
        (println)
        (println (indent (name platform) 1))
        (print-found! blocks 3))
      (println (indent "\n-none found-" 1))))
  (when (some :test-doc-blocks/skip parsed)
    (println (str "\nAnd, as requested, skipping:\n"))
    (print-found! (filter :test-doc-blocks/skip parsed) 1)))

(defn- copy-runtime! [target-root]
  (let [runtime-path "lread/test_doc_blocks/runtime.cljc"
        runtime-src (io/resource runtime-path)
        runtime-target (io/file target-root runtime-path)]
    (io/make-parents runtime-target)
    (with-open [in (io/input-stream runtime-src)]
      (io/copy in runtime-target))))

;;
;; Entry points
;;

(def default-opts
  {:target-root "./target"
   :docs ["README.md"]
   :platform :cljc})

(defn- generic-glob
  "Wrap glob to always take UNIX style pattern and output UNIX style paths as strings"
  [root pattern]
  (if windows?
    (->> (fs/glob root (string/replace pattern "/" "\\\\"))
         (map #(-> % str (string/replace "\\" "/"))))
    (map str (fs/glob root pattern))))

(defn gen-tests
  "Generate tests for code blocks found in markdown files.
  Invoke from clojure CLI with -X."
  [opts]
  (let [opts (merge default-opts opts)]
    (if-let [errs (validate/errors [:map {:closed true}
                                    [:target-root string?]
                                    [:docs  [:fn {:error/fn (fn [_ _] "should be a vector of filename strings (glob is supported)")}
                                             (fn [x] (and (vector? x) (first x) (every? string? x)))]]
                                    [:platform [:enum :clj :cljs :cljc]]]
                                   opts)]
      (do (println "Error, invalid args.")
          (println (pr-str errs))
          (System/exit 1))
      (let [{:keys [target-root docs platform]} (merge default-opts opts)
            target-root (str (io/file target-root "test-doc-blocks"))]
        (when (.exists (io/file target-root))
          (delete-dir! target-root))
        (let [target-root (str (io/file target-root "test"))
              sources (->> docs
                           (mapcat #(generic-glob "./" %))
                           sort
                           distinct)
              parsed (mapcat #(doc-parse/parse-file % platform) sources) ]
          (report-on-found! parsed)
          (let [tests (process/convert-to-tests parsed)]
            (if (seq tests)
              (do (println "\nGenerating tests to:" target-root)
                  (run! #(test-write/write-tests! target-root %) tests)
                  (copy-runtime! target-root))
              (do (println "\nError, no tests to generate.")
                  (System/exit 2))))
          (println "Done"))))))

(comment
  (gen-tests {:target-root "./target/"
              :docs ["doc/erp.adoc"
                     "README.adoc"
                     "doc/example.md"
                     "doc/example.adoc"]
              :src ["doc/example.clj"]
              :platform :cljs})

  (gen-tests {:docs ["doc/example.cljc"]})

  (gen-tests {:docs ""}))
