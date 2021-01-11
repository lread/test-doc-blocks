(ns example-md-out-test
  (:require clojure.test
            clojure.string
            lread.test-doc-blocks.runtime))

(clojure.test/deftest block-0001
  (clojure.test/testing  "doc/example.md - line 43 - The Basics"
;; A snippit of Clojure where we check result, stderr and stdout
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (do
  (println "To out I go")
  (binding [*out* *err*] (println "To err is human"))
  (* 9 9)))]
  (clojure.test/is (= '81 actual))
  (clojure.test/is (= ["To err is human"] (clojure.string/split-lines actual-err)))
  (clojure.test/is (= ["To out I go"] (clojure.string/split-lines actual-out))))))

(defn test-ns-hook [] (block-0001))