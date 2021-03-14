#!/usr/bin/env bb

(ns gen-local-tests
  (:require [babashka.classpath :as cp]))

(cp/add-classpath "./script")
(require '[helper.env :as env]
         '[helper.shell :as shell]
         '[helper.status :as status])

(defn- gen-tests
  ([] (gen-tests nil))
  ([target-root]
   (shell/command (concat ["clojure" "-X:test-doc-blocks:test-opts" "gen-tests"]
                          (when target-root
                            [":target-root" (pr-str target-root)])))))

(defn main [args]
  (env/assert-min-versions)
  (let [[cmd] args]
    (cond
      (= "regen-expected" cmd)
      (do
        (status/line :info "Regnerating expected tests for local documents.")
        (gen-tests "test-resources/expected"))

      (nil? cmd)
      (do
        (status/line :info "Generating tests for local documents.")
        (gen-tests))

      :else
      (status/fatal (str "Invalid cmd: " cmd " - see docs for usage.")))))

(main *command-line-args*)
nil
