#!/usr/bin/env bb

(ns ci-tests
  (:require [babashka.fs :as fs]
            [helper.env :as env]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn clean []
  (doseq [dir ["target" ".cpcache"]]
    (when (fs/exists? dir)
      (fs/delete-tree dir))))

(defn- lint[]
  (shell/command ["bb" "./script/lint.clj"]))

(defn- unit-tests []
  (status/line :head "Running test-doc-blocks unit tests")
  (shell/command ["clojure" "-M:kaocha" "unit"]))

(defn- generate-tests []
  (shell/command ["bb" "./script/gen_local_tests.clj"]))

(defn- run-generated-tests []
  (status/line :head "Running locally generated tests under Clojure via kaocha")
  (shell/command ["clojure" "-M:isolated/kaocha" "generated"])

  (status/line :head "Running locally generated tests under Clojure via Clojure test-runner")
  (shell/command ["clojure" "-M:isolated/clj-test-runner"])

  (status/line :head "Running locally generated tests under ClojureScript via cljs-test-runner")
  (shell/command ["clojure" "-M:isolated/cljs-test-runner"]))

(defn- integration-tests []
  (status/line :head "Running test-doc-blocks integration tests")
  (shell/command ["clojure" "-M:kaocha" "integration" ]))

(defn -main[]
  (env/assert-min-versions)
  (clean)
  (lint)
  (unit-tests)
  (generate-tests)
  (run-generated-tests)
  (integration-tests)
  (println "ci tests all done")
  nil)

(env/when-invoked-as-script
 (-main))
