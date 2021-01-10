(ns example-md-cljs-test
  (:require clojure.test
            clojure.string
            [lread.test-doc-blocks.runtime :include-macros]
            [goog.math :as math])
  (:import goog.events.EventType))

(clojure.test/deftest block-1
  (clojure.test/testing  "doc/example.md - line 167 - Specifying The Platform"
;; this code block will generate tests under example-md-cljs-test ns to a .cljs file
(clojure.test/is (= '"click" EventType.CLICK))
(clojure.test/is (= '0 (math/clamp -1 0 5)))))