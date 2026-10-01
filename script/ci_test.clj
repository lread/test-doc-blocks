(ns ci-test
  (:require [clean]
            [generate-tests]
            [helper.clojure-versions :as clojure-versions]
            [helper.jdk :as jdk]
            [lint]
            [lread.status-line :as status]
            [test-gen-clj]
            [test-gen-cljs]
            [test-gen-kaocha]
            [test-integration]
            [test-unit]))

(defn task [_opts]
  (let [jdk-version (jdk/version)]
    (clean/task {})
    (lint/task {})
    (test-unit/task {:clojure-version "all"})
    (test-integration/task {:clojure-version "all"})
    (generate-tests/task {:clojure-version clojure-versions/min-version-test-generation})
    (test-gen-kaocha/task {:clojure-version "all"})
    (test-gen-clj/task {:clojure-version "all"})
    (if (< (:major jdk-version) 21)
      (status/line :warn "ClojureScript requires JDK 21 or later (found JDK %s), skipping: test-gen-cljs"
                   (:version jdk-version))
      (test-gen-cljs/task {}))))
