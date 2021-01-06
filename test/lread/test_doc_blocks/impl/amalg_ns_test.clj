(ns lread.test-doc-blocks.impl.amalg-ns-test
  (:require [clojure.test :refer [deftest testing is]]
            [lread.test-doc-blocks.impl.amalg-ns :as sut]))

(deftest almag-requires
  (testing "single require"
    (is (= {:common #{'moo.cow.woof}}
           (sut/amalg-requires ["(require 'moo.cow.woof)"]))))
  (testing "multiple requires"
    (is (= {:common #{'moo.cow.woof1 'moo.cow.woof2 'hello.milk.dud}}
           (sut/amalg-requires ["(require 'moo.cow.woof1) (require 'hello.milk.dud)"
                                "(require 'moo.cow.woof2)"]))))
  (testing "duplicate requires"
    (is (= {:common #{'moo.cow.woof1 'moo.cow.woof2 'hello.milk.dud}}
           (sut/amalg-requires [;; TODO: are 2 requires supported by accident?
                                "(require 'moo.cow.woof1) (require 'hello.milk.dud)"
                                "(require 'moo.cow.woof2)"
                                "(require 'hello.milk.dud)"]))))
  (testing "platform requires"
    (is (= {:common #{'three 'five 'six 'seven '[x :as y]}
            :cljs #{'four '[w :as w]}
            :clj #{'one 'two '[w1 :as w1]}
            :cljr #{'eight}}
           (sut/amalg-requires ["#?(:clj (require 'one) :cljs (require 'three))"
                                "#?(:clj (require 'two) :cljs (require 'three))"
                                "#?(:cljs (require 'four))"
                                "#?(:clj (require 'three))"
                                "#?(:cljr (require 'three))"
                                "#?(:cljr (require 'eight))"
                                "(require '[x :as y] #?(:cljs '[w :as w] :clj '[w1 :as w1]))"
                                "(require 'five 'six 'seven)"]))))
  (testing "various syntaxes"
    ;; TODO: amalg my.ns1 and [my.ns1]?
    (is (= {:common #{'my.ns1 '[my.ns1] '[my.ns2] '[my.ns3 :as ns3]}}
           (sut/amalg-requires ["(require 'my.ns1)"
                                "(require '[my.ns1])"
                                "(require '[my.ns2])"
                                "(require '[my.ns3 :as ns3])"])))))

(deftest amalg-import
  (testing "single import"
    (is (= {:common #{'my.import.here}}
           (sut/amalg-imports ["(import 'my.import.here)"]))))
  (testing "various syntaxes"
    (is (= {:common #{'my.import 'my.import.one 'my.import.two 'binky.banky}}
           (sut/amalg-imports ["(import 'my.import) (import 'binky.banky)"
                               "(import '[my.import])"
                               "(import '[my.import one two])"
                               "(import 'my.import.one)"])))))
