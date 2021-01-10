(ns lread.test-doc-blocks.impl.process
  (:require [lread.test-doc-blocks.impl.amalg-ns :as amalg-ns]
            [lread.test-doc-blocks.impl.body-prep :as body-prep]
            [lread.test-doc-blocks.impl.inline-ns :as inline-ns]
            [lread.test-doc-blocks.impl.test-body :as test-body]))

(defn- amalg-ns-refs [tests]
  {:imports (amalg-ns/amalg-imports (mapcat #(get-in % [:ns-forms :imports]) tests))
   :requires (amalg-ns/amalg-requires (mapcat #(get-in % [:ns-forms :requires]) tests))})

(defn convert-to-tests
  "Takes parsed input [{block}...] and preps for output to
  [{:test-doc-blocks/test-ns ns1
    :tests [{block}...]
    :ns-refs [[boo.ya :as ya]...]}]"
        [parsed]
        (->> parsed
             (remove :test-doc-blocks/skip)
             (map #(assoc % :ns-forms (inline-ns/find-forms (:block-text %))))
             (map #(update % :block-text inline-ns/remove-forms))
             (map #(assoc % :prepped-block-text (body-prep/prep-block-for-conversion-to-test (:block-text %))))
             (map #(assoc % :test-body (test-body/to-test-body (:prepped-block-text %))))
             (map #(update % :test-doc-blocks/test-ns str))
             (sort-by (juxt :test-doc-blocks/test-ns :test-doc-blocks/platform :doc-filename :line-no))
             (group-by (juxt :test-doc-blocks/test-ns :test-doc-blocks/platform))
             (map (fn [[[test-ns platform] tests]] {:test-ns test-ns :platform platform :tests tests}))
             (map #(assoc % :ns-refs (amalg-ns-refs (:tests %))))))

(comment
  (->> (group-by (juxt :a :b) [{:a 1 :b 2 :c 3}
                              {:a 1 :b 3 :c 4}
                              {:a 2 :b 1 :c 5}])
       (map (fn [[[test-ns platform] tests]] {:test-ns test-ns :platform platform :tests tests}))
      )

  )
