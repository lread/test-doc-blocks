(ns ^:no-doc lread.test-doc-blocks.impl.body-prep
  "Prep a doc block to be converted into a test."
  (:require [clojure.string :as string]
            [rewrite-clj.zip :as z]))

(defn- parseable? [s]
  (try
    (let [zloc (z/of-string s)]
      (not= :forms (z/tag zloc)))
    (catch Exception _ex
      false)))

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
        re-line-continue             #"^\s*;+(?:\s*$| {0,1}(.*))"
        re-editor-style-expected     #"^\s*(?:;;\s*){0,1}(=clj=>|=cljs=>|=>)\s*(.*$)"]
    (-> (loop [{:keys [body parsing assert-token expectation eo-assert?] :as state} {:body ""}
               [line :as lines] (string/split-lines block-text)]
          (let [line (and line (string/trimr line))
                [_ line-assert-token line-payload] (when line
                                                     (or (re-matches re-editor-style-expected line)
                                                         (re-matches re-editor-style-out-expected line)))]
            (cond
              (= :eval-expectation parsing)
              (cond
                ;; end of eval expectation?
                (or (not line) eo-assert?)
                (recur {:body (str body
                                   assert-token " "
                                   (string/join "\n" expectation)
                                   "\n")}
                       lines)

                :else
                (let [[_ line-continue] (re-matches re-line-continue line)
                      eval-line (or line-continue line)]
                  (recur (let [{:keys [expectation] :as state} (update state :expectation conj (or eval-line ""))]
                           (if (parseable? (string/join "\n" expectation))
                             (assoc state :eo-assert? true)
                             state))
                         (rest lines))))

              (= :out-expectation parsing)
              (cond
                ;; end of expecation?
                (or (not line)
                    line-assert-token
                    eo-assert?
                    (not (re-matches re-line-continue line)))
                (recur {:body (str body
                                   assert-token " "
                                   (conj expectation)
                                   "\n")}
                       lines)

                (re-matches re-line-continue line)
                (let [[_ line-continue] (re-matches re-line-continue line)]
                  (recur (update state :expectation conj (or line-continue ""))
                         (rest lines)))

                :else
                (recur (assoc state :eo-assert? true) lines))

              ;; all done?
              (not line)
              state

              ;; start of out expection
              (or (= "=stdout=>" line-assert-token)
                  (= "=stderr=>" line-assert-token))
              (recur (assoc state
                            :parsing :out-expectation
                            :expectation (if line-payload [line-payload] [])
                            :assert-token line-assert-token)
                     (rest lines))

              ;; editor style expectation
              line-assert-token
              (recur (let [{:keys [expectation] :as state} (assoc state
                                                                  :parsing :eval-expectation
                                                                  :expectation (if line-payload [line-payload] [])
                                                                  :assert-token line-assert-token)]
                       (if (parseable? (string/join "\n" expectation))
                         (assoc state :eo-assert? true)
                         state))
                     (rest lines))

              :else
              (recur (update state :body str (str line "\n"))
                     (rest lines)))))
        :body)))


(comment
  (parseable? " , ")

  (prep-block-for-conversion-to-test ";; =stdout=> line1
; line2")
  ;; => "=stdout=> [\"line1\" \"line2\"]\n"

  (prep-block-for-conversion-to-test
   "(assoc {} :foo :bar :baz :kikka)
;; => {:foo :bar
;  :baz :kikka}"
   )
  ;; => "(assoc {} :foo :bar :baz :kikka)\n=> {:foo :bar\n :baz :kikka}\n"

  (prep-block-for-conversion-to-test
   "(assoc {} :foo :bar :baz :kikka)
;; => ^{:foo :bar
;;     :baz :kikka} []
;; some other comment
;; more comments
;; =stdout=> foo
"
)
  ;; => "(assoc {} :foo :bar :baz :kikka)\n=> ^{:foo :bar\n    :baz :kikka} []\n;; some other comment\n;; more comments\n=stdout=> [\"foo\"]\n"


  (parseable? "   ")
  ;; => false
  (parseable? "   32 ")
  ;; => true

  ;; only :a will be significant
  (parseable? ":a 2 3")
  ;; => true

  (parseable? "[:a 2\n;;some foo\n 3]")
  ;; => true

  (parseable? ";; foo")
  ;; => false

  ;; for now don't skip discards, user should get error for this eventualy
  (parseable? "#_ 3")
  ;; => true

  :eoc)
