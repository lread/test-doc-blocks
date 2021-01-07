(ns lread.test-doc-blocks.impl.test-write
  (:require [clojure.java.io :as io]
            [clojure.string :as string]))

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

(defn write-tests
  "Write out `tests` to test namespace `test-ns` under dir `target-root`"
  [target-root {:keys [test-ns ns-refs tests]}]
  (let [test-fname (io/file target-root (fname-for-ns test-ns))]
    (io/make-parents test-fname)
    (spit test-fname (str (ns-declaration test-ns ns-refs) "\n"
                          (test-defs tests)))))
