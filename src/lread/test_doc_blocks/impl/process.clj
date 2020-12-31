(ns lread.test-doc-blocks.impl.process
  (:require [clojure.edn :as edn]
            [clojure.string :as string]))

;;
;; Post parse
;;

(defn- yank-requires
  ;; TODO: does 2 things, maybe should only do 1
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
        (recur (str new-s (subs s pos start))
               (conj yanked (subs s start (inc end)))
               (inc end))
        {:block-text (str new-s (subs s pos))
         :requires yanked}))))

(defn- extract-ns-refs
  "Returns unique set of namespace refs (the vectors) from a sequence of `(require ..)` strings."
  [requires]
  (->> requires
       (map edn/read-string)
       (mapcat rest)
       (filter vector?)))

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
  => expected

  And expected gets wrapped in a string so that it can be compared:
  (= expected (pr-str actual)).
  It also stringifies potentially illegal Clojure."
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
                         (update :body str (str line "\n" (pr-str (string/trim line-next)) "\n")))
                     more)

              ;; editor style evaluation expectation:
              ;; actual
              ;; ;;=> expected
              assert-token
              (recur (update acc :body str (str assert-token (pr-str payload) "\n"))
                     (rest lines))

              ;; other lines
              :else
              (recur (update acc :body str (str line "\n"))
                     (rest lines)))))
        :body)))

(defn- amalgamate-ns-refs [tests]
  (->> tests
       (mapcat :requires-ns-refs)
       sort
       distinct
       (into [])))

(defn convert-to-tests
  "Takes parsed input [{block}...] and preps for output to
  [{:test-doc-blocks/test-ns ns1
    :tests [{block}...]
    :ns-refs [[boo.ya :as ya]...]}]"
        [parsed]
        (->> parsed
             (remove :test-doc-blocks/skip)
             (map #(merge % (yank-requires (:block-text %))))
             (map #(assoc % :requires-ns-refs (extract-ns-refs (:requires %))))
             (map #(assoc % :test-body (block-text->test-body (:block-text %))))
             ;; TODO: I guess I should read the test-ns as strs in first place
             (map #(update % :test-doc-blocks/test-ns str))
             (sort-by (juxt :test-doc-blocks/test-ns :doc-filename :line-no))
             (group-by :test-doc-blocks/test-ns)
             (into [])
             (map #(zipmap [:test-ns :tests] %))
             (map #(assoc % :ns-refs (amalgamate-ns-refs (:tests %))))))
