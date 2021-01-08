(ns example-md-cljs-test
  (:require clojure.test
            clojure.string
            [lread.test-doc-blocks.runtime :include-macros]
            [goog.math :as math])
  (:import goog.events.EventType))

(clojure.test/deftest block-1
  (clojure.test/testing  "doc/example.md - line 157 - Specifying The Platform"
;; this code block will generate tests under example-md-cljs-test ns to a .cljs file
(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture EventType.CLICK)]
  (clojure.test/is (= '"click" actual)))


(let [[actual actual-out actual-err] (lread.test-doc-blocks.runtime/eval-capture (math/clamp -1 0 5))]
  (clojure.test/is (= '0 actual)))

))