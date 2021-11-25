(ns lread.test-doc-blocks
  "Parse code blocks from markdown and generate Clojure test code."
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [docopt.core :as docopt]
            [lread.test-doc-blocks.impl.doc-parse :as doc-parse]
            [lread.test-doc-blocks.impl.process :as process]
            [lread.test-doc-blocks.impl.test-write :as test-write]
            [lread.test-doc-blocks.impl.validate :as validate])
  (:import [java.nio.file Files]))

(def ^:private windows? (-> (System/getProperty "os.name")
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
  (let [by-platform (->> parsed
                         (remove :test-doc-blocks/skip)
                         (group-by :test-doc-blocks/platform)
                         (into [])
                         (sort-by first))]
    (when (seq by-platform)
      (println "Will generate tests for following Clojure doc blocks:")
      (println "- under each found heading is listed <lineno>: <target test namespace>")
      (doseq [[platform blocks] by-platform]
        (println)
        (println (indent (name platform) 1))
        (print-found! blocks 3))
      (println)))
  (when (some :test-doc-blocks/skip parsed)
    (println (str "As requested, skipping:\n"))
    (print-found! (filter :test-doc-blocks/skip parsed) 1)
    (println)))

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

(def ^:private default-opts
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

(defn- realize-files [file-patterns]
  (let [files (reduce (fn [acc pat]
                          (let [matched-files (generic-glob "./" pat)]
                            (if (seq matched-files)
                              (concat acc matched-files)
                              (reduced {:error (str "file not found for: " pat)}))))
                        []
                        file-patterns)]
    (if (:error files)
      files
      (-> files sort distinct))))

(def ^:private valid-platforms [:clj :cljs :cljc])

(defn- gen-tests*
  [opts]
  (let [opts (merge default-opts opts)]
    (if-let [errs (validate/errors [:map {:closed true}
                                    [:target-root string?]
                                    [:docs  [:fn {:error/fn (fn [_ _] "should be a vector of filename strings (glob is supported)")}
                                             (fn [x] (and (vector? x) (first x) (every? string? x)))]]
                                    [:platform (into [:enum] valid-platforms)]]
                                   opts)]
      {:error (str "invalid args: " (pr-str errs))}
      (let [{:keys [target-root docs platform]} (merge default-opts opts)
            target-root (str (io/file target-root "test-doc-blocks"))]
        (when (.exists (io/file target-root))
          (delete-dir! target-root))
        (let [sources (realize-files docs)]
          (if (:error sources)
            sources
            (do
              (println (format "Analyzing code blocks in files:\n %s\n" (string/join "\n " sources)))
              (let [parsed (mapcat #(doc-parse/parse-file % platform) sources)]
                (report-on-found! parsed)
                (if (not (seq (remove :test-doc-blocks/skip parsed)))
                  {:error "no code blocks for test generation found"}
                  (let [target-root (str (io/file target-root "test"))
                        tests (process/convert-to-tests parsed)]
                    (println "Generating tests to:" target-root)
                    (run! #(test-write/write-tests! target-root %) tests)
                    (copy-runtime! target-root)))))))))))

(defn gen-tests
  "Generate tests for code blocks found in markdown and source files.

  Invoke from clojure CLI with -X.

  See [user guide](/doc/01-user-guide.adoc)."
  [opts]
  (let [{:keys [error]} (gen-tests* opts)]
    (when error
      (println (str "* Error: " error))
      (System/exit 1))
    (println "Done")))

(def ^:private docopt-usage
  "test-doc-blocks

Usage:
  test-doc-blocks gen-tests [--target-root=<dir>] [--platform=<platform>] [<file>...]
  test-doc-blocks --help

Options:
  -t, --target-root=<dir>    Target directory where tests are generated [default: ./target]
  -p, --platform=<platform>  By default, generate test files for one of clj, cljs or cljc [default: cljc]

Where:
  <file>...  Specifies adoc, md, clojure file(s) with code blocks from which you want to generate tests.
             Supports Java glob syntax, see https://docs.oracle.com/javase/tutorial/essential/io/fileOps.html#glob
             (Be sure to use appropriate quoting when you don't want your shell to interpret glob wildcards).

Replace test-doc-blocks with your appropriate Clojure tools CLI launch sequence. For example, via an alias:
|
| clojure -M:test-doc-blocks gen-tests --platform clj 'src/**.clj' doc/example.adoc
|
Or calling main directly:
|
| clojure -M -m lread.test-doc-blocks gen-tests --target ./some/other/dir 'src/**.{clj,cljs,cljc}' README.md
|

Clojure CLI -X syntax is also supported, see the test-doc-blocks user guide.")

(defn -main
  "Conventional command-line support. Use --help for help."
  [& args]
  (docopt/docopt docopt-usage args
                 (fn result-fn [arg-map]
                   (if (get arg-map "--help")
                     (println docopt-usage)
                     (let [platform (get arg-map "--platform")
                           ;; don't punish user for specifying keyword instead of string, ex :clj and clj are equivalent
                           platform (if (string/starts-with? platform ":") (subs platform 1) platform)
                           valid-platforms (map name valid-platforms)]
                       (if (not (some #{platform} valid-platforms))
                         (println (format "*\n* Usage error: platform must be one of: %s\n*\n\n%s"
                                          (string/join ", " valid-platforms) docopt-usage))
                         (let [target-root (get arg-map "--target-root")
                               docs (get arg-map "<file>")
                               opts (cond-> {}
                                      target-root (assoc :target-root target-root)
                                      (seq docs) (assoc :docs docs)
                                      platform (assoc :platform (keyword platform)))]
                           (gen-tests opts))))))
                 (fn usage-fn [_]
                   (println "*\n* Usage error\n*\n")
                   (println docopt-usage)
                   (System/exit 1))))
