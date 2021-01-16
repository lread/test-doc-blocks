(ns lread.test-doc-blocks.impl.process
  (:require [clojure.string :as string]
            [lread.test-doc-blocks.impl.amalg-ns :as amalg-ns]
            [lread.test-doc-blocks.impl.body-prep :as body-prep]
            [lread.test-doc-blocks.impl.inline-ns :as inline-ns]
            [lread.test-doc-blocks.impl.test-body :as test-body]))

(defn- throw-with-block-context [action cause {:keys [doc-filename line-no header]}]
  (throw (ex-info (format (string/join "\n" ["Unable to %s"
                                             "While processing code block from:"
                                             " file: %s"
                                             " line: %d"
                                             " under header: %s"])
                          action doc-filename line-no header)
                  {} cause)))

(defn- reader-wrap-block-text
  "Return block with `:block-text` wrapped in reader conditional, if requested."
  [{:keys [block-text test-doc-blocks/reader-cond] :as block}]
  (if reader-cond
    (assoc block :block-text (format "#?(%s\n(do\n%s\n))" reader-cond block-text))
    block))

(defn- find-inline-ns-forms
  "Return block with a string vector of `:ns-forms` found in `:block-text`"
  [block]
  (try
    (assoc block :ns-forms (inline-ns/find-forms (:block-text block)))
    (catch Throwable e
      (throw-with-block-context "find inline namespace forms" e block))))

(defn- remove-inline-ns-forms
  "Return block with `:block-text` absent of inline ns forms"
  [block]
  (try
    (update block :block-text inline-ns/remove-forms)
    (catch Throwable e
      (throw-with-block-context "remove inline namespace forms" e block))))

(defn- prep-block-for-conversion-to-test
  "Return block with new `:prepped-block-text`"
  [block]
  (try
    (assoc block :prepped-block-text (body-prep/prep-block-for-conversion-to-test (:block-text block)))
    (catch Throwable e
      (throw-with-block-context "prep block for conversion to test" e block)) ) )

(defn- create-test-body
  "Return block with new `:test-body`"
  [block]
  (try
    (assoc block :test-body (test-body/to-test-body (:prepped-block-text block)))
    (catch Throwable e
      (throw-with-block-context "create test body" e block))))

(defn- restructure-to-tests
  "Return blocks restructured to tests"
  [blocks]
  (->> blocks
       (map #(update % :test-doc-blocks/test-ns str))
       (sort-by (juxt :test-doc-blocks/test-ns :test-doc-blocks/platform :doc-filename :line-no))
       (group-by (juxt :test-doc-blocks/test-ns :test-doc-blocks/platform))
       (map (fn [[[test-ns platform] tests]] {:test-ns test-ns :platform platform :tests tests}))))

(defn- amalg-ns-refs [tests]
  {:imports (amalg-ns/amalg-imports (mapcat #(get-in % [:ns-forms :imports]) tests))
   :requires (amalg-ns/amalg-requires (mapcat #(get-in % [:ns-forms :requires]) tests))})

(defn- amalgamate-ns-refs
  "Returns test def `t` with new `:ns-refs`"
  [t]
  (try
    (assoc t :ns-refs (amalg-ns-refs (:tests t)))
    (catch Throwable e
      (throw (ex-info (format "unable to amalgamate inline namespace refs for test namespace %s" (:test-ns t))
                      {} e)))))

(defn convert-to-tests
  "Takes parsed input [{block}...] and preps for output to
  [{:test-doc-blocks/test-ns ns1
    :tests [{block}...]
    :ns-refs [[boo.ya :as ya]...]}]"
        [parsed]
        (->> parsed
             (remove :test-doc-blocks/skip)
             (map reader-wrap-block-text)
             (map find-inline-ns-forms)
             (map remove-inline-ns-forms)
             (map prep-block-for-conversion-to-test)
             (map create-test-body)
             restructure-to-tests
             (map amalgamate-ns-refs)))

(comment
  (->> (group-by (juxt :a :b) [{:a 1 :b 2 :c 3}
                              {:a 1 :b 3 :c 4}
                              {:a 2 :b 1 :c 5}])
       (map (fn [[[test-ns platform] tests]] {:test-ns test-ns :platform platform :tests tests}))
      )

  )
