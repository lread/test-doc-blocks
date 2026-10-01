(ns generate-tests
  (:require [babashka.fs :as fs]
            [helper.clojure-versions :as clojure-versions]
            [helper.shell :as shell]
            [lread.status-line :as status]))


(defn task
  {:org.babashka/cli {:spec (clojure-versions/cli-opt
                              (mapv :version (clojure-versions/for-test-generation)))} }
  [{:keys [clojure-version]}]
  (let [clojure-version (clojure-versions/lookup clojure-version)
        target "target/test-doc-blocks"
        success-marker (fs/file target "SUCCESS")
        regen-reason (if (not (fs/exists? success-marker))
                       "a previous successful gen result not found"
                       (let [newer-thans (fs/modified-since target
                                                            (concat
                                                              (fs/glob "doc" "**.{adoc,md,cljc}")
                                                              (fs/glob "src" "**/*.*")))]
                         (when (seq newer-thans)
                           (str "found files newer than last gen: " (mapv str newer-thans)))))]
    (if regen-reason
      (do
        (fs/delete-if-exists success-marker)
        (status/line :head "Generating tests using clojure v%s: %s" (:mvn-version clojure-version) regen-reason )
        (shell/clojure (format "-X:test-doc-blocks:test-opts:%s gen-tests" (:alias clojure-version)))
        (spit success-marker "SUCCESS"))
      (status/line :detail "Tests already successfully generated - run a \"bb clean\" to force regen"))))
