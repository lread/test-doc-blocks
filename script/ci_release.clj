#!/usr/bin/env bb

;;
;; This script is ultimately run from GitHub Actions
;;

(ns ci-release
  (:require [clean]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn- last-release-tag []
  (->  (shell/command {:out :string}
                      "git describe --abbrev=0 --match v[0-9]*")
       :out
       string/trim))

(defn- update-file! [fname match replacement]
  (let [old-content (slurp fname)
        new-content (string/replace-first old-content match replacement)]
    (if (= old-content new-content)
      (status/die 1 "Expected %s to be updated." fname)
      (spit fname new-content))))

(defn- update-user-guide! [version]
  (status/line :head "Updating library version in user guide to %s" version)
  (update-file! "doc/01-user-guide.adoc"
                #"( +\{:extra-deps \{com.github.lread/test-doc-blocks \{:mvn/version \").*(\"\}\})"
                (str  "$1" version "$2")))

(defn- adoc-section-search[content find-section]
  (cond
    (re-find (re-pattern  (str  "(?ims)^=+ " find-section " *$\\s+(^=|\\z)")) content)
    :found-with-no-text

    (re-find (re-pattern  (str  "(?ims)^=+ " find-section " *$")) content)
    :found

    :else
    :not-found))

(defn- validate-changelog
  "Certainly not fool proof, but should help for common mistakes"
  []
  (status/line :head "Validating change log")
  (let [content (slurp "CHANGELOG.adoc")
        unreleased-status (adoc-section-search content "Unreleased")
        unreleased-breaking-status (adoc-section-search content "Unreleased Breaking Changes")]
    (case unreleased-status
      :found (status/line :detail "✅ Unreleased section found with descriptive text.")
      :found-with-no-text (status/line :detail "❌ Unreleased section seems empty, please put some descriptive text in there to help our users understand what is in the release.")
      :not-found (status/line :detail "❌ Unreleased section not found, please add one."))
    (case unreleased-breaking-status
      :found (status/line :detail "✅ Unreleased Breaking Changes section found with descriptive text")
      :found-with-no-text (status/line :detail (str "❌ Unreleased breaking changes section found but seems empty.\n"
                                                    "   Please put some descriptive text in there to help our users understand what is in the release\n"
                                                    "   OR delete the section if there are no breaking changes for this release."))
      :not-found (status/line :detail "✅ Unreleased Breaking Changes section not found, assuming no breaking changes for this release."))

    (if (or (not (= :found unreleased-status))
            (= :found-with-no-text unreleased-breaking-status))
      (status/die 1 "Changelog needs some love")
      (status/line :detail "Changelog looks good for update by release workflow."))
    {:unreleased unreleased-status
     :unreleased-breaking unreleased-breaking-status}))

(defn- update-changelog! [version last-version {:keys [unreleased-breaking]}]
  (status/line :head "Updating Change Log unreleased headers to release %s" version)
  (update-file! "CHANGELOG.adoc"
                #"(?ims)^(=+) +unreleased *$(.*?)(^=+)"
                (str "$1 Unreleased\n\n$1 v" version "$2"
                     (when last-version
                       (str
                        "https://github.com/lread/test-doc-blocks/compare/"
                        last-version
                        "\\\\...v"  ;; single backslash is escape for AsciiDoc
                        version
                        "[Gritty details of changes for this release]\n\n"))
                     "$3"))
  (when (= :found unreleased-breaking)
    (update-file! "CHANGELOG.adoc"
                  #"(?ims)(=+) +unreleased breaking changes *$"
                  (str "$1 v" version))))

(defn- create-jar! []
  (status/line :head "Creating jar for release")
  (shell/command "clojure -T:build jar")
  nil)

(defn- built-version []
  (-> (shell/command {:out :string}
                     "clojure -T:build built-version")
      :out
      string/trim))

(defn- assert-on-ci
  "Little blocker to save myself from myself when testing."
  [action]
  (when (not (System/getenv "CI"))
    (status/die 1 "We only want to %s from CI" action)))

(defn- deploy-jar!
  "For this to work, appropriate CLOJARS_USERNAME and CLOJARS_PASSWORD must be in environment."
  []
  (status/line :head "Deploying jar to clojars")
  (assert-on-ci "deploy a jar")
  (shell/command "clojure -T:build deploy")
  nil)

(defn- commit-changes! [version]
  (let [tag-version (str "v" version)]
    (status/line :head "Committing and pushing changes made for %s" tag-version)
    (assert-on-ci "commit changes")
    (status/line :detail "Adding changes")
    (shell/command "git add doc/01-user-guide.adoc CHANGELOG.adoc")
    (status/line :detail "Committing")
    (shell/command "git commit -m" (str  "Release job: updates for version " tag-version))
    (status/line :detail "Version tagging")
    (shell/command "git tag -a" tag-version "-m" (str  "Release " tag-version))
    (status/line :detail "Pushing commit")
    (shell/command "git push")
    (status/line :detail "Pushing version tag")
    (shell/command "git push origin" tag-version)
    nil))

(defn- inform-cljdoc! [version]
  (status/line :head "Informing cljdoc of new version %s" version)
  (assert-on-ci "inform cljdoc")
  (let [exit-code (->  (shell/command {:continue true}
                                      "curl" "-X" "POST"
                                      "-d" "project=com.github.lread/test-doc-blocks"
                                      "-d" (str  "version=" version)
                                      "https://cljdoc.org/api/request-build2")
                       :exit)]
    (when (not (zero? exit-code))
      (status/line :warn "Informing cljdoc did not seem to work, attempt exited with %d" exit-code))))

(def args-usage "Valid args: (prep|deploy-remote|commit|validate|--help)

Commands:
  prep           Update user guide, changelog and create jar
  deploy-remote  Deploy jar to clojars
  commit         Commit changes made back to repo, inform cljdoc of release

These commands are expected to be run in order from CI.
Why the awkward separation?
To restrict the exposure of our CLOJARS secrets during deploy workflow

Additional commands:
  validate      Verify that change log is good for release

Options
  --help        Show this help")

(defn -main [& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (cond
      (get opts "prep")
      (do (clean/clean!)
          (let [changelog-status (validate-changelog)
                last-version (last-release-tag)]
            (status/line :detail "Last version released: %s" (or last-version "<none>"))
            (io/make-parents "target")
            (create-jar!)
            (let [version (built-version)]
              (status/line :detail (str "Built version: " version))
              (update-user-guide! version)
              (update-changelog! version last-version changelog-status))))

      (get opts "deploy-remote")
      (deploy-jar!)

      (get opts "commit")
      (let [version (built-version)]
        (commit-changes! version)
        (inform-cljdoc! version))

      (get opts "validate")
      (do (validate-changelog)
          nil))))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
