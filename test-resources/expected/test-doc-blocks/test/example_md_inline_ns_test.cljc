(ns example-md-inline-ns-test
  (:require clojure.test
            clojure.string
            #?(:cljs [lread.test-doc-blocks.runtime :include-macros])
            #?(:clj lread.test-doc-blocks.runtime)
            clojure.string
            [clojure.string]
            [clojure.string :as string]
            [clojure.set :as cset])
  (:import #?(:clj java.util.List java.util.Queue java.util.Set :cljs goog.math.Long goog.math.Vec2 goog.math.Vec3)))

(clojure.test/deftest block-0001
  (clojure.test/testing  "doc/example.md - line 277 - Inline requires and imports"
;; Stick the basics for requires, shorthand notation isn't supported

;; Some examples:
;; For cljc code examples it is fine for your requires and imports to contain, or be wrapped by, reader conditionals

;; Some examples of supported imports

; test-doc-blocks dummy assertion to appease tools that fail on no assertions
(clojure.test/is (= '"dummy" "dummy"))))

(defn test-ns-hook [] (block-0001))