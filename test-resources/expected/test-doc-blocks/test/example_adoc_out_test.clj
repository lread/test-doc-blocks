(ns example-adoc-out-test
  (:require clojure.test
            clojure.string
            lread.test-doc-blocks.runtime))

(clojure.test/deftest block-0001
  (clojure.test/testing  "doc/example.adoc - line 71 - The Basics"
;; A snippit of Clojure where we check result, stderr and stdout
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (do
  (println "To out I go")
  (binding [*out* *err*] (println "To err is human"))
  (* 9 9)))]
  (clojure.test/is (= '81 actual))
  (clojure.test/is (= ["To err is human"] (clojure.string/split-lines actual-err)))
  (clojure.test/is (= ["To out I go"] (clojure.string/split-lines actual-out))))))

(clojure.test/deftest block-0002
  (clojure.test/testing  "doc/example.adoc - line 113 - Multiline Expectations"
;; A snippit of Clojure where we check result, stderr and stdout
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (do
  (println "Hi ho\nHi ho\nTo out I will go")
  (binding [*out* *err*] (println "To err is human\nTo forgive\nIs devine"))
  (range 1 16)))]
  (clojure.test/is (= '( 1  2  3  4  5
     6  7  8  9 10
    11 12 13 14 15) actual))
  (clojure.test/is (= ["To err is human" "To forgive" "Is devine"] (clojure.string/split-lines actual-err)))
  (clojure.test/is (= ["Hi ho" "Hi ho" "To out I will go"] (clojure.string/split-lines actual-out))))))

(clojure.test/deftest block-0003
  (clojure.test/testing  "doc/example.adoc - line 135 - Multiline Expectations"
;; A snippit of Clojure where we check result, stderr and stdout
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (do
  (println "Hi ho\nHi ho\nTo out I will go\n=stderr=> still part of stdout")
  (binding [*out* *err*] (println "To err is human\nTo forgive\nIs devine\n=> part of stderr"))
  [:bip :bop '=stdout> :bap :borp]))]
  (clojure.test/is (= '[:bip :bop
     =stdout> :bap
     :borp] actual))
  (clojure.test/is (= ["To err is human" "To forgive" "Is devine" "=> part of stderr"] (clojure.string/split-lines actual-err)))
  (clojure.test/is (= ["Hi ho" "Hi ho" "To out I will go" "=stderr=> still part of stdout"] (clojure.string/split-lines actual-out))))))

(defn test-ns-hook [] (block-0001) (block-0002) (block-0003))