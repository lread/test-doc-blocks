(ns lread.test-doc-blocks.impl.inline-ns-test
  (:require [clojure.string :as string]
            [clojure.test :refer [deftest testing is]]
            [lread.test-doc-blocks.impl.inline-ns :as sut]))

(deftest find-inline-forms
  (testing "emptiness"
    (is (= {:requires [] :imports []} (sut/find-forms ""))))
  (testing "top level require"
    (is (= {:requires ["(require '[boo :as ya])"]
            :imports []}
           (sut/find-forms "(require '[boo :as ya])"))))
  (testing "top level import"
    (is (= {:requires []
            :imports ["(import the.thing here)"]}
           (sut/find-forms "(import the.thing here)"))))
  (testing "1 deep import"
    (is (= {:requires []
            :imports ["#?(:clj (import the.thing here))"]}
           (sut/find-forms "#?(:clj (import the.thing here))"))))
  (testing "deeper with sibs"
    ;; TODO: brings in other code too.. not horrible.. not great
    (is (= {:requires ["#?(:clj\n(do\n(some other code)(require '[cljs.reader :refer [read-string]])(more code)))"]
            :imports []}
           (sut/find-forms "#?(:clj\n(do\n(some other code)(require '[cljs.reader :refer [read-string]])(more code)))")))))

(deftest remove-inline-forms
  (testing "top levels"
    (is (= "(some code) nil (some other code) nil"
           (sut/remove-forms "(some code) (require '[hey there]) (some other code) (import '[good stuff])"))))
  (testing "producing empty reader cond"
    (is (= "#?(:clj nil)"
           (sut/remove-forms "#?(:clj (require '[my.ns.here]))"))))
  (testing "deeper"
    (is (= (string/join "\n"
                        ["(some code here)"
                         "#?(:clj"
                         "(do"
                         " (other code)"
                         " nil"
                         " (more code)"
                         " nil"
                         " (still more code)"
                         ")"
                         ")"])
           (sut/remove-forms (string/join "\n"
                                          ["(some code here)"
                                           "#?(:clj"
                                           "(do"
                                           " (other code)"
                                           " (require '[all is good])"
                                           " (more code)"
                                           " (import '[my.stuff rocks])"
                                           " (still more code)"
                                           ")"
                                           ")"]))))))
