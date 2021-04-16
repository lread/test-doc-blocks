#!/usr/bin/env bb

(ns gen-local-tests
  (:require [docopt.core :as docopt]
            [docopt.match :as docopt-match]
            [helper.env :as env]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn- gen-tests
  [opts-aliases]
  (shell/command ["clojure"
                  (format "-X:test-doc-blocks:%s" opts-aliases)
                  "gen-tests"])
  nil)

(def docopt-usage "Usage: gen_local_tests.clj [options]

Options:
  --help            Show this screen.
  --regen-expected  Regenerate expected tests (see README)")

(defn -main [& args]
  (env/assert-min-versions)
  (if-let [opts (-> docopt-usage docopt/parse (docopt-match/match-argv args))]
    (cond
      (get opts "--regen-expected")
      (do
        (status/line :head "Regnerating expected tests for local documents.")
        (gen-tests "test-opts:regen-opts"))

      (get opts "--help")
      (println docopt-usage)

      :else
      (do
        (status/line :head "Generating tests for local documents.")
        (gen-tests "test-opts")))

    (status/die 1 docopt-usage)))

(env/when-invoked-as-script
 (apply -main *command-line-args*))
