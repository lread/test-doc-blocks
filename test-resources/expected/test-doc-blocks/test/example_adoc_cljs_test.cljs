(ns example-adoc-cljs-test
  (:require clojure.test
            clojure.string
            [lread.test-doc-blocks.runtime :include-macros true]
            [goog.math :as math])
  (:import goog.events.EventType))

(clojure.test/deftest block-0001
  (clojure.test/testing  "doc/example.adoc - line 305 - Specifying The Platform - :platform"
;; this code block will generate a test under example-adoc-cljs-test ns to a .cljs file

nil
(clojure.test/is (= '"click" EventType.CLICK))
nil
(clojure.test/is (= '0 (math/clamp -1 0 5)))))

(defn test-ns-hook [] (block-0001))