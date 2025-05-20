(ns lread.test-doc-blocks.impl.body-prep-test
  (:require [clojure.string :as string]
            [clojure.test :refer [deftest testing is]]
            [lread.test-doc-blocks.impl.body-prep :as sut]
            [matcher-combinators.test]))

(deftest converts-stdout-assertions-test
  (testing "single line"
    (is (= "=stdout=> [\"line1\"]\n"
           (sut/prep-block-for-conversion-to-test ";;=stdout=> line1"))))
  (testing "multiple lines"
    (is (= "=stdout=> [\"line1\" \"line2\" \"line3\"]\n"
           (sut/prep-block-for-conversion-to-test
            (string/join "\n"
                         [";; =stdout=> line1"
                          "; line2"
                          "; line3"]))))

    ;; Should not matter how many leading comments are used
    (is (= "=stdout=> [\"line1\" \"line2\" \"line3\"]\n"
           (sut/prep-block-for-conversion-to-test
            (string/join "\n"
                         [";; =stdout=> line1"
                          ";; line2"
                          ";; line3"])))))
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

(deftest converts-stderr-assertions-test
  (testing "sanity test - multiple lines"
    (is (= "=stderr=> [\"line1\" \"line2\" \"line3\"]\n"
           (sut/prep-block-for-conversion-to-test
            (string/join "\n"
                         [";; =stderr=> line1"
                          "; line2"
                          "; line3"]))))))

(deftest converts-simple-editor-style-assertions-test
  (is (= "=> (+ 1 2 3)\n"
         (sut/prep-block-for-conversion-to-test ";; => (+ 1 2 3)")))
  (is (= "=clj=> (+ 4 5 6)\n"
         (sut/prep-block-for-conversion-to-test ";; =clj=> (+ 4 5 6)")))
  (is (= "=cljs=> (+ 7 8 9)\n"
         (sut/prep-block-for-conversion-to-test ";; =cljs=> (+ 7 8 9)"))))


(deftest ignores-repl-style-assertions-test
  (is (= "user=> (* 6 7)\n42\n"
         (sut/prep-block-for-conversion-to-test
          (string/join "\n"
                       ["user=> (* 6 7)"
                        "42"])))))

(deftest whitespace-handling-test
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

(deftest comment-handling-test
  (testing "Editor style multiline blocks removes leading comments"
    ;; The part of code that is rewriting these blocks as test cases expects the assertion symbol
    ;; `=>` to be on its own line and the following expectation on a separate line without leading
    ;; comment characters
    (is (= "=> {:foo :bar\n:baz :quu}\n"
           (sut/prep-block-for-conversion-to-test ";; => {:foo :bar\n;:baz :quu}\n"))))
  (testing "Editor style multiline blocks can use more than one leading comment characted"
    (is (= "=> {:foo :bar\n:baz :quu}\n"
           (sut/prep-block-for-conversion-to-test ";; => {:foo :bar\n;;:baz :quu}\n"))))
  (testing "Editor style multiline blocks without leading comments"
    (is (= "=> {:foo :bar\n:baz :quu}\n"
           (sut/prep-block-for-conversion-to-test ";; => {:foo :bar\n:baz :quu}\n"))))
  (testing "missing ;; is ok for assertion"
    (is (= "=stdout=> [\"hello there\"]\n"
           (sut/prep-block-for-conversion-to-test "=stdout=> hello there"))))
  (testing "no conversion when assertion token line does not startw with ;;"
    (is (= ";=stdout=> hello there\n"
           (sut/prep-block-for-conversion-to-test ";=stdout=> hello there")))
    (is (= ";;;=stdout=> hello there\n"
           (sut/prep-block-for-conversion-to-test ";;;=stdout=> hello there"))))
  )

(deftest multiline-expectations-test
  (testing "pass-thru editor-style"
    (is (match? ["[:i :can :be"
                 " :on :multiple"
                 " :lines]"
                 "=> [:i :can :be"
                 "    :on :multiple"
                 "    :lines]"]
                (string/split-lines
                 (sut/prep-block-for-conversion-to-test
                  (string/join "\n" ["[:i :can :be"
                                     " :on :multiple"
                                     " :lines]"
                                     "=> [:i :can :be"
                                     "    :on :multiple"
                                     "    :lines]"]))))))
  (testing "comment editor-style"
    (is (match? ["[:i :can :be"
                 " :on :multiple"
                 " :lines]"
                 "=> [:i :can :be"
                 #" * :on :multiple$"
                 #" * :lines]$"]
                (string/split-lines
                 (sut/prep-block-for-conversion-to-test
                  (string/join "\n" ["[:i :can :be"
                                     " :on :multiple"
                                     " :lines]"
                                     ";;=> [:i :can :be"
                                     ";;    :on :multiple"
                                     ";;    :lines]"]))))))
  (testing "comment editor-style different leading commas on continuation lines"
    (is (match? ["[:i :can :be"
                 " :on :multiple"
                 " :lines]"
                 "=> [:i :can :be"
                 #" * :on :multiple$"
                 #" * :lines]$"]
                (string/split-lines
                 (sut/prep-block-for-conversion-to-test
                  (string/join "\n" ["[:i :can :be"
                                     " :on :multiple"
                                     " :lines]"
                                     ";; => [:i :can :be"
                                     ";      :on :multiple"
                                     ";;;;   :lines]"]))))))
  (testing "comment editor-style no leading commas on continuation lines"
    (is (match? ["[:i :can :be"
                 " :on :multiple"
                 " :lines]"
                 "=> [:i :can :be"
                 #" * :on :multiple$"
                 #" * :lines]$"]
                (string/split-lines
                 (sut/prep-block-for-conversion-to-test
                  (string/join "\n" ["[:i :can :be"
                                     " :on :multiple"
                                     " :lines]"
                                     ";; => [:i :can :be"
                                     "       :on :multiple"
                                     "       :lines]"])))))
    (testing "can handle embedded => assertion token in =stdout=> expectation"
      (is (match? ["(do (println \"When do I end?\\n=> 77\")"
                   "    42)"
                   "=stdout=> [\"When do I end?\" \"=> 77\"]"
                   "=> 42"
                   "]"]
                  (string/split-lines
                   (sut/prep-block-for-conversion-to-test
                    (string/join "\n" ["(do (println \"When do I end?\\n=> 77\")"
                                       "    42)"
                                       ";; =stdout=> When do I end?"
                                       "; => 77"
                                       ";; => 42"
                                       "]"]))))))
    (testing "can handle embedded =stdout=> token in => expectation"
      (is (match? ["[:when :do :i '=stdout=> (inc 41) :end?]"
                   "=> [:when"
                   ":do"
                   ":i"
                   "=stdout=> 42"
                   ":end?]"]
                  (string/split-lines
                   (sut/prep-block-for-conversion-to-test
                    (string/join "\n" ["[:when :do :i '=stdout=> (inc 41) :end?]"
                                       ";; => [:when"
                                       ";; :do"
                                       ";; :i"
                                       ";; =stdout=> 42"
                                       ";; :end?]"]))))))
    (testing "can handle embedded =stdout=> token in => expectation"
      (is (match? ["[:when :do :i '=stdout=> (inc 41) :end?]"
                   "=> [:when"
                   ":do"
                   ":i"
                   "=stdout=> 42"
                   ":end?]"]
                  (string/split-lines
                   (sut/prep-block-for-conversion-to-test
                    (string/join "\n" ["[:when :do :i '=stdout=> (inc 41) :end?]"
                                       "=> [:when"
                                       ":do"
                                       ":i"
                                       "=stdout=> 42"
                                       ":end?]"]))))))))

(deftest larger-test
  (is (match? ["Testing 123"
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
               ";; some comment"
               "=stdout=> [\"some stdout\" \"continue stdout\"]"
               "=stderr=> [\"some stderr goes\" \"here and\" \"here\"]"
               ""
               "(assoc {} :foo :bar :baz :quu)"
               "=> {:foo :bar"
               "    :baz :quu}"
               ";; some other comment"
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
                                   ";; some comment"
                                   ";; =stdout=> some stdout"
                                   "; continue stdout"
                                   ";; =stderr=>"
                                   "; some stderr goes"
                                   "; here and"
                                   "; here"
                                   ""
                                   "(assoc {} :foo :bar :baz :quu)"
                                   ";; => {:foo :bar"
                                   ";;     :baz :quu}"
                                   ";; some other comment"
                                   "That is all"]))))))
