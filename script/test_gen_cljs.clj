(ns test-gen-cljs
  (:require [generate-tests]
            [helper.clojure-versions :as clojure-versions]
            [helper.jdk :as jdk]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn run-generated-tests
  []
  (status/line :head "Running generated tests using cljs-test-runner")
  (shell/clojure "-M:isolated/cljs-test-runner"))

(defn task
  [_opts]
  (let [jdk-version (jdk/version)]
    (if (< (:major jdk-version) 21)
      (status/die 1 "ClojureScript requires JDK 21 or later (found JDK %s)"
                  (:version jdk-version))
      (do
        (generate-tests/task {:clojure-version clojure-versions/min-version-test-generation})
        (run-generated-tests)))))

