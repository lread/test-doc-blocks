#!/usr/bin/env bb

;;
;; This script is ultimately run from GitHub Actions
;;

(ns ci-release
  (:require [build-shared]
            [clean]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(def changelog-fname "CHANGELOG.adoc")
(def user-guide-fname "doc/01-user-guide.adoc")

(defn- last-release-tag []
  (->  (shell/command {:out :string}
                      "git describe --abbrev=0 --match v[0-9]*")
       :out
       string/trim))

(defn- bump-version!
  "bump version stored in deps.edn"
  []
  (shell/command "bb neil version patch --no-tag"))

(defn- update-file! [fname desc match replacement]
  (let [old-content (slurp fname)
        new-content (string/replace-first old-content match replacement)]
    (if (= old-content new-content)
      (status/die 1 "Expected to %s in %s" desc fname)
      (spit fname new-content))))

(defn- update-user-guide! [version]
  (status/line :detail "Updating library version in user guide to %s" version)
  (update-file! user-guide-fname
                "update :lib-version: adoc attribute"
                #"(?m)(^:lib-version: ).*$"
                (str "$1" version)))

(defn- analyze-changelog
  "Certainly not fool proof, but should help for common mistakes"
  []
  (let [content (slurp changelog-fname)
        valid-attrs ["[minor breaking]" "[breaking]"]
        [_ attr content :as match] (re-find #"(?ims)^== Unreleased ?(.*?)$(.*?)(== v\d|\z)" content)]
    (if (not match)
      [{:error :section-missing}]
      (cond-> []
        (and attr
             (not (string/blank? attr))
             (not (contains? (set valid-attrs) attr)))
        (conj {:error :suffix-invalid :valid-attrs valid-attrs :found attr})

        ;; without any words of a reasonable min length, we consider section blank
        (not (re-find #"(?m)[\p{L}]{3,}" content))
        (conj {:error :content-missing})))))

(defn- changelog-checks []
  (let [changelog-findings (reduce (fn [acc n] (assoc acc (:error n) n))
                                   {}
                                   (analyze-changelog))]
    [{:check "changelog has unreleased section"
      :result (if (:section-missing changelog-findings) :fail :pass)}
     {:check "changelog unreleased section attributes valid"
      :result (cond
                (:section-missing changelog-findings) :skip
                (:suffix-invalid changelog-findings) :fail
                :else :pass)
      :msg (when-let [{:keys [valid-attrs found]} (:suffix-invalid changelog-findings)]
             (format "expected attributes to absent or one of %s, but found: %s" (string/join ", " valid-attrs) found))}
     {:check "changelog unreleased section has content"
      :result (cond
                (:section-missing changelog-findings) :skip
                (:content-missing changelog-findings) :fail
                :else :pass)}]))

(defn- validate-changelog []
  (status/line :head "Performing publish checks")
  (let [check-results (changelog-checks)
        passed? (every? #(= :pass (:result %)) check-results)]
    (doseq [{:keys [check result msg]} check-results]
      (status/line :detail "%s %s"
                   (case result
                     :pass "✓"
                     :fail "x"
                     :skip "~")
                   check)
      (when msg
        (status/line :detail "  > %s" msg)))
    (when (not passed?)
      (status/die 1 "Changelog checks failed"))))

(defn- yyyy-mm-dd-now-utc []
  (-> (java.time.Instant/now) str (subs 0 10)))

(defn- update-changelog! [version release-tag last-release-tag]
  (status/line :detail "Applying version %s to changelog" version)
  (update-file! changelog-fname
                "update unreleased header"
                #"(?ims)^== Unreleased(.*?)($.*?)(== v\d|\z)"
                (str
                 ;; add Unreleased section for next released
                 "== Unreleased\n\n"
                 ;; replace "Unreleased" with actual version
                 "== v" version
                 ;; followed by any attributes
                 "$1"
                 ;; followed by datestamp
                 " - " (yyyy-mm-dd-now-utc)
                 ;; followed by an AsciiDoc anchor for easy referencing
                 " [[v" version  "]]"
                 ;; followed by section content
                 "$2"
                 ;; followed by link to commit log
                 (when last-release-tag
                   (str
                    "https://github.com/" (build-shared/lib-github-coords) "/compare/"
                    last-release-tag
                    "\\\\..."  ;; single backslash is escape for AsciiDoc
                    release-tag
                    "[commit log]\n\n"))
                 ;; followed by next section indicator
                 "$3")))

(defn- create-jar! []
  (status/line :head "Creating jar for release")
  (shell/command "clojure -T:build jar")
  nil)

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
  (shell/command "clojure -T:build:deploy deploy")
  nil)

(defn- commit-changes! [tag-version]
  (status/line :head "Committing and pushing changes made for %s" tag-version)
  (assert-on-ci "commit changes")
  (status/line :detail "Adding changes")
  (shell/command "git add deps.edn" user-guide-fname changelog-fname)
  (status/line :detail "Committing")
  (shell/command "git commit -m" (str  "Release job: updates for version " tag-version))
  (status/line :detail "Version tagging")
  (shell/command "git tag -a" tag-version "-m" (str  "Release " tag-version))
  (status/line :detail "Pushing commit")
  (shell/command "git push")
  (status/line :detail "Pushing version tag")
  (shell/command "git push origin" tag-version)
  nil)

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

(defn- create-github-release! [release-tag]
  (status/line :head "Creating GitHub Release")
  (assert-on-ci "create github release")
  (let [changelog-url (format "https://github.com/%s/blob/main/%s"
                              (build-shared/lib-github-coords)
                              changelog-fname)]
    (shell/command "gh release create"
                   release-tag
                   "--title" release-tag
                   "--notes" (format "[Changelog](%s#%s)" changelog-url release-tag))))

(def args-usage "Valid args: (prep|deploy-remote|commit|create-github-release|inform-cljdoc|validate|--help)

Commands:
  prep                   Bump version, create jar and update user guide and changelog
  deploy-remote          Deploy jar to clojars
  commit                 Commit changes made back to repo
  create-github-release  Create a GitHub release
  inform-cljdoc          Trigger a doc build on cljdoc

These commands are expected to be run in order from CI.
Why the awkward separation?
To restrict the exposure of our secrets during deploy workflow

Additional commands:
  validate      Verify that change log is good for release

Options
  --help        Show this help")

(defn -main [& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (cond
      (get opts "prep")
      (do (clean/clean!)
          (validate-changelog)
          (bump-version!)
          (let [last-release-tag (last-release-tag)
                version (build-shared/lib-version)
                release-tag (build-shared/version->tag version)]
            (status/line :detail "Release version: %s" version)
            (status/line :detail "Release tag: %s" release-tag)
            (status/line :detail "Last release tag: %s" last-release-tag)
            (io/make-parents "target")
            (create-jar!)
            (status/line :head "Updating docs")
            (update-user-guide! version)
            (update-changelog! version release-tag last-release-tag)))

      (get opts "deploy-remote")
      (deploy-jar!)

      (get opts "commit")
      (commit-changes! (-> (build-shared/lib-version) build-shared/version->tag))

      (get opts "inform-cljdoc")
      (inform-cljdoc! (build-shared/lib-version))

      (get opts "create-github-release")
      (create-github-release! (-> (build-shared/lib-version) build-shared/version->tag))

      (get opts "validate")
      (do (validate-changelog)
          nil))))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
