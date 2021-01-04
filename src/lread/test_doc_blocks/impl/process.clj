(ns lread.test-doc-blocks.impl.process
  (:require [clojure.string :as string]
            [lread.test-doc-blocks.impl.amalg-ns :as amalg-ns]
            [lread.test-doc-blocks.impl.inline-ns :as inline-ns]))

;;
;; Post parse
;;


(defn- block-text->test-body
  "Convert doc block to something suitable for test body.

  stdout and stderr:
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
  => expected "
  [block-text]
  (let [re-out-continue #"^\s*;;(?:\s*$| (.*))"
        re-repl-style-actual #"^\s*(user=>).*"
        re-editor-style-out-expected #"^\s*;;\s{0,1}(=stdout=>|=stderr=>)\s*$"
        re-editor-style-expected #"^\s*(?:;;\s*){0,1}(=clj=>|=cljs=>|=>)\s*(.*$)"]
    (-> (loop [acc {:body ""}
               ;; add extra empty line to trigger close of trailing multiline
               [line line-next & more :as lines] (string/split block-text #"\n")]
          (let [[_ assert-token payload] (when line
                                           (or (re-matches re-repl-style-actual line)
                                               (re-matches re-editor-style-expected line)
                                               (re-matches re-editor-style-out-expected line)))]
            (cond
              ;; out expectation ends
              (and (:out acc) (or (not line)
                                  assert-token
                                  (not (re-matches re-out-continue line))
                                  (re-matches re-editor-style-expected line)))
              (recur (-> acc
                         (update :body str (str (:out-token acc) " "
                                                (str (conj (:out acc)) "\n")))
                         (dissoc :out :out-token))
                     lines)

              ;; done?
              (not line)
              acc

              ;; collecting stdout/stderr expectation
              (and (:out acc)
                   (not assert-token)
                   (re-matches re-out-continue line))
              (let [[_ out-line] (re-matches re-out-continue line)]
                (recur (update acc :out conj (or out-line ""))
                       (rest lines)))

              ;; out expectation starts
              (or (= "=stdout=>" assert-token)
                  (= "=stderr=>" assert-token))
              (recur (assoc acc :out [] :out-token assert-token)
                     (rest lines))

              ;; repl style evaluation expectation:
              ;; user=> actual
              ;; expected
              (= "user=>" assert-token)
              (recur (-> acc
                         (update :body str (str line "\n" (string/trim line-next) "\n")))
                     more)

              ;; editor style evaluation expectation:
              ;; actual
              ;; ;;=> expected
              assert-token
              (recur (update acc :body str (str assert-token " " payload "\n"))
                     (rest lines))

              ;; other lines
              :else
              (recur (update acc :body str (str line "\n"))
                     (rest lines)))))
        :body)))

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
             (map #(assoc % :test-body (block-text->test-body (:block-text %))))
             ;; TODO: I guess I should read the test-ns as strs in first place
             (map #(update % :test-doc-blocks/test-ns str))
             (sort-by (juxt :test-doc-blocks/test-ns :doc-filename :line-no))
             (group-by :test-doc-blocks/test-ns)
             (into [])
             (map #(zipmap [:test-ns :tests] %))
             (map #(assoc % :ns-refs (amalg-ns-refs (:tests %))))))
