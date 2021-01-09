(ns lread.test-doc-blocks.impl.body-prep-test
  (:require [clojure.string :as string]
            [clojure.test :refer [deftest testing is]]
            [lread.test-doc-blocks.impl.body-prep :as sut]))

(deftest converts-stdout-assertions
  (testing "single line"
    (is (= "=stdout=> [\"line1\"]\n"
           (sut/prep-block-for-conversion-to-test ";;=stdout=> line1"))))
  (testing "multiple lines"
    (is (= "=stdout=> [\"line1\" \"line2\" \"line3\"]\n"
           (sut/prep-block-for-conversion-to-test
            (string/join "\n"
                         [";; =stdout=> line1"
                          "; line2"
                          "; line3"])))))
  (testing "multiple lines, first line in block"
    (is (= "=stdout=> [\"line1\" \"line2\" \"line3\"]\n"
           (sut/prep-block-for-conversion-to-test
            (string/join "\n"
                         [";; =stdout=> "
                          "; line1"
                          "; line2"
                          "; line3"])))))
  (testing "converts if leading ;; absent from assert line"
    (is (= "=stdout=> [\"line1\" \"line2\"]\n"
           (sut/prep-block-for-conversion-to-test "=stdout=> line1\n; line2"))))
  (testing "assertions in stdout blocks are not assertions"
    (is (= "=stdout=> [\"(println \\\"hello there\\\")\" \";; =stdout=>\" \"; hello\" \"; there\"]\n"
           (sut/prep-block-for-conversion-to-test
            (string/join "\n" ["=stdout=>"
                               "; (println \"hello there\")"
                               "; ;; =stdout=>"
                               "; ; hello"
                               "; ; there"]))))) )

(deftest converts-stderr-assertions
  (testing "sanity test - multiple lines"
    (is (= "=stderr=> [\"line1\" \"line2\" \"line3\"]\n"
           (sut/prep-block-for-conversion-to-test
            (string/join "\n"
                         [";; =stderr=> line1"
                          "; line2"
                          "; line3"]))))))

(deftest converts-simple-editor-style-assertions
  (is (= "=> (+ 1 2 3)\n"
         (sut/prep-block-for-conversion-to-test ";; => (+ 1 2 3)")))
  (is (= "=clj=> (+ 4 5 6)\n"
         (sut/prep-block-for-conversion-to-test ";; =clj=> (+ 4 5 6)")))
  (is (= "=cljs=> (+ 7 8 9)\n"
         (sut/prep-block-for-conversion-to-test ";; =cljs=> (+ 7 8 9)"))))


(deftest ignores-repl-style-assertions
  (is (= "user=> (* 6 7)\n42\n"
         (sut/prep-block-for-conversion-to-test
          (string/join "\n"
                       ["user=> (* 6 7)"
                        "42"])))))

(deftest whitespace-handling
  (testing "editor style"
    (is (= "=> {:a 1 :b 2}\n"
           (sut/prep-block-for-conversion-to-test ";;=>{:a 1 :b 2}")
           (sut/prep-block-for-conversion-to-test "    ;;     =>       {:a 1 :b 2}   "))))
  (testing "editor out style"
    (is (= "=stdout=> [\"hello there\"]\n"
           (sut/prep-block-for-conversion-to-test ";;=stdout=>hello there")))
    (is (= "=stdout=> [\"line1\" \"line2\"]\n"
           (sut/prep-block-for-conversion-to-test (string/join "\n" [";;=stdout=>"
                                                                     ";line1"
                                                                     ";line2"]))))
    (testing "considers leading space past the first space significant"
      (is (= "=stdout=> [\"    hello there\"]\n"
             (sut/prep-block-for-conversion-to-test ";;=stdout=>     hello there    ")))
      (is (= "=stdout=> [\"    line1\" \"     line2\"]\n"
             (sut/prep-block-for-conversion-to-test (string/join "\n" [";;=stdout=>   "
                                                                       ";     line1     "
                                                                       ";      line2    "]))))    )) )

(deftest comment-handling
  (testing "missing ;; is ok for assertion"
    (is (= "=stdout=> [\"hello there\"]\n"
           (sut/prep-block-for-conversion-to-test "=stdout=> hello there"))))
  (testing "no conversion when not ;;"
    (is (= ";=stdout=> hello there\n"
           (sut/prep-block-for-conversion-to-test ";=stdout=> hello there")))
    (is (= ";;;=stdout=> hello there\n"
           (sut/prep-block-for-conversion-to-test ";;;=stdout=> hello there"))) ))

(deftest larger-test
  (is (= ["Testing 123"
          ""
          "REPL style asserts"
          ""
          "user=> (+ 1 2 3)"
          "6"
          ""
          "Editor style asserts"
          ""
          "(* 8 8)"
          "=> 64"
          ""
          "(do-something)"
          "=> 42"
          "=stdout=> [\"some stdout\" \"continue stdout\"]"
          "=stderr=> [\"some stderr goes\" \"here and\" \"here\"]"
          "That is all"]
         (string/split-lines
          (sut/prep-block-for-conversion-to-test
           (string/join "\n" ["Testing 123"
                              ""
                              "REPL style asserts "
                              ""
                              "user=> (+ 1 2 3)"
                              "6"
                              ""
                              "Editor style asserts"
                              ""
                              "(* 8 8)"
                              ";; => 64"
                              ""
                              "(do-something)"
                              ";; => 42"
                              ";; =stdout=> some stdout"
                              "; continue stdout"
                              ";; =stderr=>"
                              "; some stderr goes"
                              "; here and"
                              "; here"
                              "That is all"]))))))
