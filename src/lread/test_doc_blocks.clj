(ns lread.test-doc-blocks
  "Parse code blocks from markdown and generate Clojure test code."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.nio.file Files]))

(defn- file-ext[fname]
  (let [pos (.lastIndexOf fname ".")]
    (when (> pos 0)
      (subs fname (inc pos)))))

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

(defn- test-ns-for-doc-file
  "Return a default test namespace for given `doc-filename`.

  Cleans up what might be illegal chars and tacks on a `-test` suffix."
  [doc-filename]
  (-> (.getName (io/file doc-filename))
      (string/replace #"^[0-9-. _]*" "")
      (string/replace #"[0-9-. _]*$" "")
      (string/replace #"[ ._]" "-")
      (string/replace #"-+" "-")
      (str "-test")
      (string/lower-case)))

;;
;; Parsing markdown
;;

(defn- adoc-parse-inline-opts
  "Parse inline test-doc-blocks options to map.
  Can be map or keyword.
  If keyword k returns {:k true} "
  [{:keys [doc-filename doc-line-no]} sopts]
  (try
    (let [o (edn/read-string sopts)]
      (if (keyword? o)
        {o true}
        o))
    (catch Throwable e
      (throw (ex-info (format "Unable to parse test-doc-blocks opts. file: %s @ line: %d"
                              doc-filename doc-line-no) {} e)))))


(defn- normalize-lang
  "Normalized lang specified in code block to something consistent for Clojure blocks."
  [lang]
  (if (and lang (re-matches #"(?i)Clojure.*" lang))
    "Clojure"
    lang))

;; md (CommonMark parse support)

(defn- md-header-2line
  "Header 1
   =======

   Header 2
   --------"
  [{:keys [in-code-block? line line-prev]}]
  (when (and (not in-code-block?)
             (re-matches #"\s{0,3}[=-]+\s*" line))
    {:header line-prev}))

(defn- md-header-1line
  "# header 1
   ## header 2
   ### header 3
   etc"
  [{:keys [in-code-block? line]}]
  (when-not in-code-block?
    (when-let [header (second (re-matches #"^\s{0,3}#+\s(.*)\s*$" line))]
      {:header header})))

(defn- md-header [parse-state]
  (or (md-header-2line parse-state)
      (md-header-1line parse-state)))

(defn- md-code-block-start
  "```Clojure"
  [{:keys [in-code-block? line]}]
  (when (not in-code-block?)
    (when-let [[_ lang] (re-matches #"(?i)^\s*```\s*(\w*)\s*$" line)]
      {:new-block? true :lang (normalize-lang lang) :end-block-re #"\s*```\s*" })))

(defn- md-opts
  "Test-doc-blocks opts are conveyed in comments:
   <!-- {:test-doc-blocks/opt value} -->
   <!-- :test-doc-blocks/boolean-opt -->"
  [{:keys [in-code-block? line] :as parse-state}]
  (when-not in-code-block?
    (when-let [[_ sopts] (re-matches #"\s*<!--\s+(\{?\s*:test-doc-blocks/.*)\s+-->\s*$" line)]
      {:opts (adoc-parse-inline-opts parse-state sopts)})))

;; adoc (Asciidoctor parse support)

(defn- adoc-header-1line
  "= header 1
   == header 2
   === header 3
   etc"
  [{:keys [in-code-block? line]}]
  (when-not in-code-block?
    (when-let [header (second (re-matches #"^=+\s+(.*)\s*$" line))]
      {:header header})))

(defn- adoc-header
  "Adoc also recongizes md header syntax.
  For now we only support 1line md headers, as I don't yet understand the rules in adoc for the 2 line version."
  [parse-state]
  (or (adoc-header-1line parse-state)
      (md-header-1line parse-state)))

(defn- adoc-code-block-start*
  "[source,clojure]
   ----"
  [{:keys [in-code-block? line line-prev]}]
  (when (and (not in-code-block?)
             (= line "----"))
    (when-let [[_ lang] (re-matches #"(?i)^\[source\s*,\s*(\w*)\s*\]\s*$" line-prev)]
      {:new-block? true :lang (normalize-lang lang) :end-block-re #"----\s*"})))

(defn- adoc-code-block-start
  "Adoc also recongizes md code block syntax."
  [parse-state]
  (or (adoc-code-block-start* parse-state)
      (md-code-block-start parse-state)))

(defn- adoc-opts
  "Test-doc-blocks opts are conveyed in comments:
   //{:test-doc-blocks/boolean-opt value}
   //:test-doc-blocks/boolean-opt"
  [{:keys [in-code-block? line] :as parse-state}]
  (when-not in-code-block?
    (when-let [[_ sopts] (re-matches #"//\s*(\{?\s*:test-doc-blocks.*)" line)]
      {:opts (adoc-parse-inline-opts parse-state sopts)})))

(defn- code-block-end
  "End block regular expression is defined by block start"
  [{:keys [in-code-block? line end-block-re]}]
  (when (and in-code-block? (re-matches end-block-re line))
    {:end-block? true}))

;; assemble matchers for parsing

(def parse-def {"adoc" [code-block-end
                        adoc-code-block-start
                        adoc-opts
                        adoc-header]
                "md" [code-block-end
                      md-code-block-start
                      md-opts
                      md-header]})

(defn- parsers-for [filename]
  (let [doc-type (file-ext filename)]
    (or (get parse-def doc-type)
        (throw (ex-info (format "Don't know how to parse %s, file types supported: %s"
                                filename
                                (string/join ", " (keys parse-def))) {})))))

(defn- parse-next [parsers state line]
  (first (keep #(% (assoc state :line line)) parsers)))

(defn- parse-doc-code-blocks
  "Parse out clojure code blocks from markdown in `doc-filename`.
  Formats supported are md and adoc and determined by file extension.

  Returns vector of maps representing blocks found
  - :doc-filename - same as `doc-filename`
  - :line-no - line number at start of block
  - :header - last header found before code block
  - :block-text - content of block in string "
  ([doc-filename] (parse-doc-code-blocks doc-filename (io/reader doc-filename)))
  ;; this arity to support REPL testing via string reader
  ([doc-filename rdr]
   (let [parsers (parsers-for doc-filename)]
     (loop [[line & lines] (line-seq rdr)
            state {:opts {:test-doc-blocks/test-ns (test-ns-for-doc-file doc-filename)}
                   :doc-filename doc-filename ;; unchanging but used for error reporting
                   :doc-line-no 1
                   :blocks []}]
       (if line
         (recur lines
                (-> (let [p (parse-next parsers state line)]
                      (cond
                        (:end-block? p)
                        (let [lang (:block-lang state)
                              state (dissoc state :in-code-block? :end-block-re :block-lang)]
                          (if-not (= "Clojure" lang)
                            state
                            (-> state
                                (update :blocks conj (-> (:block state)
                                                         (merge (:opts state))
                                                         (assoc :header (:header state))
                                                         (assoc :doc-filename (:doc-filename state))))
                                (update :opts dissoc :test-doc-blocks/skip)
                                (assoc :block {}))))

                        (and (:new-block? p))
                        (let [state (-> state (assoc :in-code-block? true
                                                     :end-block-re (:end-block-re p)
                                                     :block-lang (:lang p)))]
                          (if-not (= "Clojure" (:lang p))
                            state
                            (-> state (assoc :block {:line-no (:doc-line-no state)
                                                     :block-text ""}))))

                        (and (:in-code-block? state) (= "Clojure" (:block-lang state)))
                        (update-in state [:block :block-text] #(str % "\n" line))

                        (:header p)
                        (merge state p)

                        (:opts p)
                        (update state :opts merge (:opts p))

                        :else
                        state))
                    (update :doc-line-no inc)
                    (assoc :line-prev line)))
         (:blocks state))))))

;;
;; Post parse
;;

(defn- yank-requires
  "Returns map where `:block-text` is `s` without any `(require ...)` and `:requires` is a vector of strings of each yanked `(require ...)`.

  Warning: Super naive for now.
  No handling of parens that maybe nested, in string or comments, etc."
  [s]
  (loop [new-s ""
         yanked []
         pos 0]
    (let [start (or (string/index-of s "(require " pos)
                    (string/index-of s "(require\n" pos))
          end (string/index-of s ")" pos)]
      (if (and start end)
        (recur (str new-s (subs s pos (dec start)))
               (conj yanked (subs s start (inc end)))
               (inc end))
        {:block-text (str new-s (subs s pos))
         :requires yanked}))))

(defn- ns-refs-from-requires
  "Returns a unique set of namespace refs (the vectors) from a sequence of `(require ..)` strings."
  [requires]
  (->> requires
       (map edn/read-string)
       (mapcat rest)
       (filter vector?)))

(defn- doc-block->test-body
  "Convert doc block to something suitable for test body.

  ;; =stdout=>
  ;; line
  ;; line2

  becomes:

  =stdout=> [\"line1\" \"line2\"]

  Editor style:
  actual
  ;; => expected
  becomes:
  actual
  => expected

  And expected gets wrapped in a string so that it can be compared:
  (= expected (pr-str actual)).
  It also stringifies potentially illegal Clojure."
  [block-text]
  (let [re-stdout-start #"^\s*;;\s{0,1}=stdout=>\s*$"
        re-stdout-continue #"^\s*;;(?:\s*$| (.*))"
        re-repl-style-actual #"^\s*user=>.*"
        re-editor-style-expected #"^\s*(?:;;\s*){0,1}(=clj=>|=cljs=>|=>)\s*(.*$)"]
    (-> (loop [acc {:body ""}
               ;; add extra empty line to trigger close of trailing multiline
               [line line-next & more :as lines] (string/split block-text #"\n")]
          (if-not line
            acc
            (cond
              ;; stdout expectation ends
              (and (:stdout acc) (re-matches re-stdout-continue line)
                   (not (re-matches re-stdout-continue (or line-next ""))))
              (let [[_ out-line] (re-matches re-stdout-continue line)]
                   (recur (-> acc
                              (update :body str (str "=stdout=> " (str (conj (:stdout acc) (or out-line "")) "\n")))
                              (dissoc :stdout))
                          (rest lines)))

              ;; collecting stdout expectation
              (and (:stdout acc) (re-matches re-stdout-continue line))
              (let [[_ out-line] (re-matches re-stdout-continue line)]
                (recur (update acc :stdout conj (or out-line ""))
                       (rest lines)))

              ;; stdout expectation starts
              (re-matches re-stdout-start line)
              (recur (assoc acc :stdout [])
                     (rest lines))

              ;; repl style evaluation expectation:
              ;; user=> actual
              ;; expected
              (re-matches re-repl-style-actual line)
              (recur (-> acc
                         (update :body str (str line "\n" (pr-str (string/trim line-next)) "\n")))
                     more)

              ;; editor style evaluation expectation:
              ;; actual
              ;; ;;=> expected
              (re-matches re-editor-style-expected line)
              (let [[_ prefix expected] (re-matches re-editor-style-expected line)]
                (recur (update acc :body str (str prefix (pr-str expected) "\n"))
                       (rest lines)))

              ;; other lines
              :else
              (recur (update acc :body str (str line "\n"))
                     (rest lines)))))
        :body)))

;
;; Generating test files
;;

(defn- testing-text [{:keys [doc-filename header line-no]}]
  (string/join " - " (keep identity [doc-filename header (str "line " line-no)])))

(defn- write-tests
  "Write out `tests` to test namespace `test-ns` under dir `target-root`.
  Some attention paid to formatting just to make manual verification easier on the eyes."
  [target-root [test-ns tests]]
  (let [ns-name (str "test-doc-blocks.gen" "." test-ns)
        test-fname (io/file target-root (fname-for-ns ns-name))
        test-doc-blocks-refs ["#?(:clj [lread.test-doc-blocks.runtime :refer [deftest-doc-blocks testing-block]]"
                              "   :cljs [lread.test-doc-blocks.runtime :refer-macros [deftest-doc-blocks testing-block]])"]
        yanked-require-refs (->> tests
                                 (mapcat #(ns-refs-from-requires (:requires %)))
                                 sort
                                 distinct
                                 (into []))]
    (io/make-parents test-fname)
    (println "writing -" (str test-fname))
    (spit test-fname
          (str "(ns " ns-name  "\n"
               "  (:require " (-> (into [] yanked-require-refs)
                                  (into test-doc-blocks-refs)
                                  (->> (string/join "\n            "))) "))\n"
               "\n"
               "(deftest-doc-blocks\n"
               "\n"
               (->> (reduce (fn [acc t]
                              (conj acc (str
                                         "(testing-block " " \"" (testing-text t) "\"\n"
                                         (doc-block->test-body (:block-text t)) ")")))
                            []
                            tests)
                    (string/join "\n\n"))
               ")\n"))))

(def default-opts
  {:target-root "./target"
   :docs ["README.md"]})

;;
;; Entry points
;;

(defn gen-tests
  "Generate tests for code blocks found in markdown files.
  Invoke from clojure CLI with -X."
  [opts]

  (let [{:keys [target-root docs]} (merge default-opts opts )
        target-root (str (io/file target-root "test-doc-blocks"))]
    (when (.exists (io/file target-root))
      (delete-dir target-root))
    (let [target-root (str (io/file target-root "test"))]
      (println "\nGenerating tests to:" target-root)
      (run! (fn [d]
              (println "Processing:" d)
              (->> (parse-doc-code-blocks d)
                   (remove :test-doc-blocks/skip)
                   (map #(merge % (yank-requires (:block-text %))))
                   (group-by :test-doc-blocks/test-ns)
                   (into [])
                   (sort-by (fn [[_tgroupname [t & _ts]]]  (:line-no t)))
                   (run! #(write-tests target-root %))))
            docs))))

(comment
  (gen-tests {:target-root "./target/"
              :docs ["README.adoc"
                     "doc/example.md"
                     "doc/example.adoc"]})

  (gen-tests {:docs ["README.adoc"]})

  )
