(ns lread.test-doc-blocks.impl.inline-ns-test
  (:require [clojure.test :refer [deftest testing is]]
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
  (testing "1 deep with sibs"
    ;; TODO: bringing in sibs is not great... should either ignore this form or fail
    ;; TODO: wait this is not valid, :clj is followed by 1 form
    (is (= {:requires ["#?(:clj (require 'my.good.ns) (some other stuff))"]
            :imports []}
           (sut/find-forms "#?(:clj (require 'my.good.ns) (some other stuff))"))))
  (testing "multiple requires as sibs 1 level deep"
    ;; TODO: multiple requires/imports should be fine? double check with amalg alg
    ;; WAIT: this is is not valid :clj is follow by 1 form
    (is (= {:requires ["#?(:clj (require 'my.good.ns) (require 'another.good.ns))"]
            :imports []}
           (sut/find-forms "#?(:clj (require 'my.good.ns) (require 'another.good.ns))"))))
  )
