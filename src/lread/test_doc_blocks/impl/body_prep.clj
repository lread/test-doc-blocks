(ns ^:no-doc lread.test-doc-blocks.impl.body-prep
  "Prep a doc block to be converted into a test."
  (:require [clojure.string :as string]))

(defn ^:no-doc prep-block-for-conversion-to-test
  "Convert doc block to Clojure that can be more easily parsed.

  We uncomment editor style assertions converting stdout and stderr style.

  Example 1:

    actual
    ;; => expected

   becomes:

    actual
    => expected

  Example 2:

    ;; =stdout=>           ;; =stdout=> line1
    ; line1           OR   ; line2
    ; line2

   becomes:

    =stdout=> [\"line1\" \"line2\"]
  "
  [block-text]
  (let [re-editor-style-out-expected #"^\s*(?:;;\s*){0,1}(=stdout=>|=stderr=>)(?:\s*$| {0,1}(.*))"
        re-out-continue #"^\s*;+(?:\s*$| {0,1}(.*))"
        re-editor-style-expected #"^\s*(?:;;\s*){0,1}(=clj=>|=cljs=>|=>)\s*(.*$)"]
    (-> (loop [acc {:body ""}
               [line :as lines] (string/split-lines block-text)]
          (let [line (and line (string/trimr line))
                [_ assert-token payload] (when line
                                           (or (re-matches re-editor-style-expected line)
                                               (re-matches re-editor-style-out-expected line)))]
            (cond
              ;; expectation ends
              (and (:out acc) (or (not line)
                                  assert-token
                                  (not (re-matches re-out-continue line))
                                  (re-matches re-editor-style-expected line)))
              (recur (as-> acc acc
                       (case (:style acc)
                         :out
                         (update acc :body str (str (:out-token acc) " "
                                                    (conj (:out acc)) "\n"))
                         :editor
                         (update acc :body str (str (:assert-token acc) " "
                                                    (string/join "\n" (:out acc))
                                                    "\n")))
                       (dissoc acc :out :out-token :assert-token :style))
                     lines)

              ;; all done?
              (not line)
              acc

              ;; collecting expectation
              (and (:out acc)
                   (not assert-token)
                   (re-matches re-out-continue line))
              (let [[_ out-line] (re-matches re-out-continue line)]
                (recur (update acc :out conj (or out-line ""))
                       (rest lines)))

              ;; out expectation starts
              (or (= "=stdout=>" assert-token)
                  (= "=stderr=>" assert-token))
              (recur (assoc acc
                            :style :out
                            :out (if payload [payload] [])
                            :out-token assert-token)
                     (rest lines))

              ;; editor style evaluation expectation:
              ;; actual
              ;; ;;=> expected
              assert-token
              (recur (assoc acc
                            :out (if payload [payload] [])
                            :style :editor
                            :assert-token assert-token)
                     (rest lines))

              ;; other lines
              :else
              (recur (update acc :body str (str line "\n"))
                     (rest lines)))))
        :body)))


(comment
  (prep-block-for-conversion-to-test ";; =stdout=> line1
; line2")

  (prep-block-for-conversion-to-test
   "(assoc {} :foo :bar :baz :kikka)
;; => {:foo :bar
;  :baz :kikka}"
   )
  )
