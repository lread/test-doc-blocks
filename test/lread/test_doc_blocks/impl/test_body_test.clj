(ns lread.test-doc-blocks.impl.test-body-test
  (:require [clojure.string :as string]
            [clojure.test :refer [deftest testing is]]
            [lread.test-doc-blocks.impl.test-body :as sut]))

(defn tbody [& lines]
  (str (string/join "\n" lines) "\n"))

(defn tlines [t]
  (string/split-lines t))

(defmacro lsug [f & lines]
  `(tlines (~f (tbody ~@lines))))

(def dummy-assertion [""
                      "; test-doc-blocks dummy assertion to appease tools that fail on no assertions"
                      "(clojure.test/is (= '\"dummy\" \"dummy\"))"])

(defn- out-assertion [atype expected]
  (str "  (clojure.test/is (= " expected " (clojure.string/split-lines actual-" atype ")))") )

(defn- out-assertion-block [actual assertions]
  (concat
   [(str "(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture " actual ")]")]
   (butlast assertions)
   [(str (last assertions) ")")]))

(deftest converts-user=>
  (testing "simplest case"
    (is (= ["(clojure.test/is (= '6 (+ 1 2 3)))"]
           (lsug sut/to-test-body
                 "user=> (+ 1 2 3)"
                 "6"))))
  (testing "code sandwich"
    (is (= ["(some other code before)"
            "(clojure.test/is (= '6 (+ 1 2 3)))"
            "(some other code after)"]
           (lsug sut/to-test-body
                 "(some other code before)"
                 "user=> (+ 1 2 3)"
                 "6"
                 "(some other code after)"))))
  (testing "incomplete - no actual"
    (is (= (concat ["user=>"]
                   dummy-assertion)
           (lsug sut/to-test-body
                 "user=>"))))
  (testing "incomplete - no expected"
    (is (= (concat ["user=> (* 7 8)"]
                   dummy-assertion)
           (lsug sut/to-test-body
                 "user=> (* 7 8)"))))
  ;; TODO: provide some more context in the error
  (testing "incomplete - actual not closed (invalid code)"
    (is (thrown-with-msg? Exception #"Unexpected EOF"
           (lsug sut/to-test-body
                 "user=> {:a 1 :b 2}"
                 "{:a 1")))) )

(deftest converts-=>
  (testing "simplest case"
    (is (= ["(clojure.test/is (= '6 (+ 1 2 3)))"]
           (lsug sut/to-test-body
                 "(+ 1 2 3)"
                 "=> 6"))))
  (testing "code sandwich"
    (is (= ["(some other code before)"
            "(clojure.test/is (= '6 (+ 1 2 3)))"
            "(some other code after)"]
           (lsug sut/to-test-body
                 "(some other code before)"
                 "(+ 1 2 3)"
                 "=> 6"
                 "(some other code after)"))))
  (testing "incomplete - no actual"
    (is (= (concat ["=>"]
                   dummy-assertion)
           (lsug sut/to-test-body
                 "=>"))))
  (testing "incomplete - no expected"
    (is (= (concat ["=> (* 7 8)"]
                   dummy-assertion)
           (lsug sut/to-test-body
                 "=> (* 7 8)"))))
  ;; TODO: provide some more context in the error
  (testing "incomplete - actual not closed (invalid code)"
    (is (thrown-with-msg? Exception #"Unexpected EOF"
           (lsug sut/to-test-body
                 "{:a 1 :b 2}"
                 "=> {:a 1")))) )

(deftest converts-=clj=>
  (testing "simplest case"
    (is (= ["#?(:clj (clojure.test/is (= '6 (+ 1 2 3))))"]
           (lsug sut/to-test-body
                 "(+ 1 2 3)"
                 "=clj=> 6")))))

(deftest converts-=cljs=>
  (testing "simplest case"
    (is (= ["#?(:cljs (clojure.test/is (= '6 (+ 1 2 3))))"]
           (lsug sut/to-test-body
                 "(+ 1 2 3)"
                 "=cljs=> 6")))))

(deftest converts-=stdout=>
  (testing "simplest case"
    (is (= (out-assertion-block "(some-fun 1 2 3)"
                                [(out-assertion "out" "[\"line1\" \"line2\"]")])
           (lsug sut/to-test-body
                 "(some-fun 1 2 3)"
                 "=stdout=> [\"line1\" \"line2\"]")))))

(deftest converts-=stderr=>
  (testing "simplest case"
    (is (= (out-assertion-block "(some-fun 1 2 3)"
                                [(out-assertion "err" "[\"line1\" \"line2\"]")])
           (lsug sut/to-test-body
                 "(some-fun 1 2 3)"
                 "=stderr=> [\"line1\" \"line2\"]")))))

(deftest converts-multiple
  (testing "all possible"
    (is (= (out-assertion-block "(some-fun 1 2 3)"
                                ["  (clojure.test/is (= '789 actual))"
                                 "  #?(:clj (clojure.test/is (= '99 actual)))"
                                 "  #?(:cljs (clojure.test/is (= '45 actual)))"
                                 (out-assertion "err" "[\"eline1\" \"eline2\"]")
                                 (out-assertion "out" "[\"oline1\" \"oline2\"]")])
           (lsug sut/to-test-body
                 "(some-fun 1 2 3)"
                 ;; doesn't make sense to mix => with clj and cljs specific but we don't block it
                 "=> 789"
                 "=clj=> 99"
                 "=cljs=> 45"
                 "=stdout=> [\"oline1\" \"oline2\"]"
                 "=stderr=> [\"eline1\" \"eline2\"]"))))
  (testing "when no stdout, stderr, use simple assertions"
    (is (= ["(clojure.test/is (= '789 (some-fun 1 2 3)))"
            "#?(:clj (clojure.test/is (= '99 (some-fun 1 2 3))))"
            "#?(:cljs (clojure.test/is (= '45 (some-fun 1 2 3))))"]
           (lsug sut/to-test-body
                 "(some-fun 1 2 3)"
                 ;; doesn't make sense to mix => with clj and cljs specific but we don't block it
                 "=> 789"
                 "=clj=> 99"
                 "=cljs=> 45"))))
  (testing "incomplete pair"
    (is (= ["(clojure.test/is (= '789 (some-fun 1 2 3)))"
            "#?(:clj (clojure.test/is (= '99 (some-fun 1 2 3))))"
            "=cljs=>"]
           (lsug sut/to-test-body
                 "(some-fun 1 2 3)"
                 ;; doesn't make sense to mix => with clj and cljs specific but we don't block it
                 "=> 789"
                 "=clj=> 99"
                 "=cljs=>")))) )
