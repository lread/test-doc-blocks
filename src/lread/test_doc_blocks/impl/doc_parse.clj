(ns ^:no-doc lread.test-doc-blocks.impl.doc-parse
  "Parse doc blocks from AsciiDoc and CommonMark files."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [lread.test-doc-blocks.impl.validate :as validate]))
;;
;; Parsing markdown, we look at:
;; - code blocks
;; - the heading that appears before a code block
;; - test-doc-block opts conveyed in comments

;; TODO: I am more than just parsing markdown, I am also interpreting test-doc-opts opts.
;; probably a good idea to separate those two things.

(defn- file-ext[fname]
  (let [pos (.lastIndexOf fname ".")]
    (when (> pos 0)
      (subs fname (inc pos)))))

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


(defn- validate-inline-opts
  ;; fail fast on first error for now
  [doc-filename doc-line-no m]
  (if-let [errs (validate/errors [:map {:closed true}
                                  [:test-doc-blocks/platform {:optional true} [:enum :clj :cljs :cljc]]
                                  [:test-doc-blocks/skip {:optional true} boolean?]
                                  [:test-doc-blocks/reader-cond {:optional true} [:enum :clj :cljs :cljr]]
                                  [:test-doc-blocks/test-ns {:optional true} symbol?]
                                  [:test-doc-blocks/meta {:optional true} [:or map? keyword?]]
                                  [:test-doc-blocks/apply {:optional true} [:enum :next :all-next]]]
                                 m)]
    (throw (ex-info (format "Invalid inline options: %s\n file: %s line: %d"
                            errs, doc-filename doc-line-no) {}))
    m))


(defn- parse-inline-opts
  "Parse inline test-doc-blocks options to map.
  Can be map or keyword.
  If keyword k returns {:k true} "
  [{:keys [doc-filename doc-line-no]} sopts]
  (let [m (try
            (let [o (edn/read-string sopts)]
              (if (keyword? o) {o true} o))
            (catch Throwable e
              (throw (ex-info (format "Unable to parse test-doc-blocks opts.\n file: %s line: %d"
                                      doc-filename doc-line-no) {} e))))]
    (validate-inline-opts doc-filename doc-line-no m)))

(defn- normalize-lang
  "Normalized lang specified in code block to something consistent for Clojure blocks."
  [lang]
  (if (and lang (re-matches #"(?i)(Clojure.*|clj|cljs|cljc)" lang))
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
    (when-let [[_ indent lang] (re-matches #"(?i)^( *)```\s*(\w*)\s*$" line)]
      {:new-block? true :lang (normalize-lang lang) :end-block-re #"\s*```\s*" :block-indentation (count indent)})))

(defn- md-opts
  "Test-doc-blocks opts are conveyed in comments:
   <!-- {:test-doc-blocks/opt value} -->
   <!-- :test-doc-blocks/boolean-opt -->"
  [{:keys [in-code-block? line] :as parse-state}]
  (when-not in-code-block?
    (when-let [[_ sopts] (re-matches #"\s*<!--\s+(\{?\s*#?:test-doc-blocks.*)\s+-->\s*$" line)]
      {:opts (parse-inline-opts parse-state sopts)})))

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
   //:test-doc-blocks/boolean-opt
   //#:test-doc-blocks{:my-opt1 val1 :my-opt2 val2}"
  [{:keys [in-code-block? line] :as parse-state}]
  (when-not in-code-block?
    (when-let [[_ sopts] (re-matches #"//\s*(\{?\s*#?:test-doc-blocks.*)" line)]
      {:opts (parse-inline-opts parse-state sopts)})))

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

(defn- block-line
  "CommonMark code blocks can be indented.
   We need to strip the indentation.

   CommonMark seems somewhat forgiving of

      ```Clojure
      somewhat
    skewed left
      here
      ```
  So we allow for that."
  [{:keys [block-indentation]} line]
  (if block-indentation
    (if (string/blank? line)
      ""
      (subs line (min (->> line (re-find #"^( *)[^ ]") second count)
                      block-indentation)))
    line))

(defn parse-doc-code-blocks
  "Parse out clojure code blocks from markdown in `doc-filename`.
  Formats supported are md and adoc and determined by file extension.

  Returns vector of maps representing blocks found
  - :doc-filename - same as `doc-filename`
  - :line-no - line number at start of block
  - :header - last header found before code block
  - :block-text - content of block in string "
  ([doc-filename platform]
   (with-open [rdr (io/reader doc-filename)]
     (parse-doc-code-blocks doc-filename platform rdr)))
  ;; this arity to support REPL testing via string reader
  ([doc-filename platform rdr]
   (let [parsers (parsers-for doc-filename)]
     (loop [[line & lines] (line-seq rdr)
            state {:default-opts {:test-doc-blocks/test-ns (test-ns-for-doc-file doc-filename)
                                  :test-doc-blocks/platform platform}
                   :doc-filename doc-filename ;; unchanging but used for error reporting
                   :doc-line-no 1
                   :blocks []}]
       (if line
         (recur lines
                (try
                  (-> (let [p (parse-next parsers state line)]
                        (cond
                          (:end-block? p)
                          (let [lang (:block-lang state)
                                state (dissoc state :in-code-block? :end-block-re :block-lang :block-indentation)]
                            (if-not (= "Clojure" lang)
                              state
                              (-> state
                                  (update :blocks conj (-> (:block state)
                                                           (merge (:default-opts state) (:next-block-opts state))
                                                           (assoc :header (:header state))
                                                           (assoc :doc-filename (:doc-filename state))))
                                  (dissoc :next-block-opts)
                                  (assoc :block {}))))

                          (:new-block? p)
                          (let [state (-> state (assoc :in-code-block? true
                                                       :end-block-re (:end-block-re p)
                                                       :block-indentation (:block-indentation p)
                                                       :block-lang (:lang p)))]
                            (if-not (= "Clojure" (:lang p))
                              state
                              (-> state (assoc :block {:line-no (:doc-line-no state)
                                                       :block-text ""}))))

                          (and (:in-code-block? state) (= "Clojure" (:block-lang state)))
                          (update-in state
                                     [:block :block-text]
                                     #(str % (block-line state line) "\n"))

                          (:header p)
                          (merge state p)

                          (:opts p)
                          (let [opts (:opts p)
                                apply-to (:test-doc-blocks/apply opts)
                                opts (dissoc opts :test-doc-blocks/apply)]
                            (if (= :all-next apply-to)
                              (update state :default-opts merge opts)
                              (update state :next-block-opts merge opts)))

                          :else
                          state))
                      (update :doc-line-no inc)
                      (assoc :line-prev line))
                  (catch Throwable e
                    (throw (ex-info (format "Unable to parse %s, issue at line %d" doc-filename (:doc-line-no state)) {} e)))))
         (:blocks state))))))

(comment
  (parse-doc-code-blocks "test.adoc" :clj (io/reader (char-array
                                               (string/join "\n" ["# hey"
                                                                  "//#:test-doc-blocks{:a 1 :b 2 :apply :subsequent}"
                                                                  "   ```Clojure"
                                                                  "   clj"
                                                                  "    clj"
                                                                  "```"
                                                                  ""
                                                                  "   ```Clojure"
                                                                  "   clj"
                                                                  "    clj"
                                                                  "```"
                                                                  ""


                                                                  ]))))

)
