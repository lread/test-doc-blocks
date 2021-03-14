#!/usr/bin/env bb

(ns gen-local-tests
  (:require [babashka.classpath :as cp]))

(cp/add-classpath "./script")
(require '[helper.env :as env]
         '[helper.shell :as shell]
         '[helper.status :as status])

(defn- gen-tests
  [opts-aliases]
  (shell/command ["clojure"
                  (format "-X:test-doc-blocks:%s" opts-aliases)
                  "gen-tests"]))

(defn main [args]
  (env/assert-min-versions)
  (let [[cmd] args]
    (cond
      (= "regen-expected" cmd)
      (do
        (status/line :info "Regnerating expected tests for local documents.")
        (gen-tests "test-opts:regen-opts"))

      (nil? cmd)
      (do
        (status/line :info "Generating tests for local documents.")
        (gen-tests "test-opts"))

      :else
      (status/fatal (str "Invalid cmd: " cmd " - see docs for usage.")))))

(main *command-line-args*)
nil
