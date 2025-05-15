(ns ^:no-doc lread.test-doc-blocks.impl.test-write
  "Write test to file system."
  (:require [clojure.java.io :as io]
            [clojure.string :as string]))

(defn- fname-for-ns
  "Converts `ns-name` to a cljc file path."
  [ns-nom platform]
  (-> ns-nom
      (string/replace "-" "_")
      (string/replace "." "/")
      (str "." (name platform))))

(defn- testing-text [{:keys [doc-filename line-no header]}]
  (string/join " - " (keep identity [doc-filename (str "line " line-no) header])))

(defn- str-splicing-reader-cond [l]
  (when (seq l)
    [(str "#?@(" (string/join " " (map str l)) ")")]))

(defn- str-reader-cond [l]
  (when (seq l)
    (str "#?(" (string/join " " (map str l)) ")")))

(defn- base-requires [platform]
  (let [base ['clojure.test 'clojure.string]
        runtime-cljs "[lread.test-doc-blocks.runtime :include-macros true]"
        runtime-clj "lread.test-doc-blocks.runtime"]
    (case platform
      :clj (conj base runtime-clj)
      :cljs (conj base runtime-cljs)
      :cljc (conj base (str "#?(:cljs " runtime-cljs ")")
                       (str "#?(:clj " runtime-clj ")")))))

(defn- ns-declaration [test-ns platform {:keys [refer-clojures requires imports] :as _ns-ref}]
  (str "(ns " test-ns
       (some->> (:default refer-clojures) seq (str "\n  "))
       (some->> (:reader-cond refer-clojures) seq str-reader-cond (str "\n  "))
       "\n  (:require " (->> []
                             (into (:default requires))
                             (into (str-splicing-reader-cond (:reader-cond requires)))
                             (into (base-requires platform))
                             (string/join "\n            ")) ")"
       (when (or (seq (:default imports)) (seq (:reader-cond imports)))
         (str "\n  (:import " (->> []
                                   (into (:default imports))
                                   (into (str-splicing-reader-cond (:reader-cond imports)))
                                   (string/join "\n    ")) ")"))
       ")\n"))

(defn- test-var [tst]
  (let [meta-data (when-let [md (:test-doc-blocks/meta tst)]
                    (str "^" (pr-str md)))
        test-name (:test-name tst)]
    (if meta-data
      (str meta-data " " test-name)
      test-name)))

(defn- test-defs [tests]
  (str (->> (reduce (fn [acc t]
                      (conj acc (str
                                 "(clojure.test/deftest " (test-var t) "\n"
                                 "  (clojure.test/testing " " \"" (testing-text t) "\"\n"
                                 (:test-body t) "))")))
                    []
                    tests)
            (string/join "\n\n"))))

(defn- test-ns-hook
  "To enforce tests running in order, we provide a test-ns-hooks.

  Koacha does not support this feature, but can be asked not to randomize tests.
  In this case it seems to run them in alphabetical sort order."
  [tests]
  (str
   "\n\n(defn test-ns-hook [] "
   (string/join " " (mapv #(format "(%s)" (:test-name %)) tests))
   ")")
  )

(defn write-tests!
  "Write out `tests` to test namespace `test-ns` under dir `target-root`"
  [target-root {:keys [test-ns platform ns-refs tests]}]
  (let [test-fname (io/file target-root (fname-for-ns test-ns platform))
        tests (map-indexed (fn [ndx t] (assoc t :test-name (format "block-%04d" (inc ndx)))) tests)]
    (io/make-parents test-fname)
    (spit test-fname (str (ns-declaration test-ns platform ns-refs) "\n"
                          (test-defs tests)
                          (test-ns-hook tests)))))
