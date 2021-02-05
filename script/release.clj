#!/usr/bin/env bb

;;
;; This script is ultimately run from GitHub Actions
;; 

(ns release
  (:require [babashka.classpath :as cp]
            [babashka.deps :as deps]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(deps/add-deps '{:deps {version-clj/version-clj {:mvn/version  "0.1.2"}}})
(cp/add-classpath (.getParent (io/file *file*)))

(require '[helper.fs :as fs]
         '[helper.shell :as shell]
         '[helper.status :as status]
         '[version-clj.core :as version])

(defn- most-recent-released-version []
  (let [vtag (->  (shell/command ["git" 
                                  "tag" "--sort=committerdate" 
                                  "--list" "v[0-9]*"] {:out :string})
                  :out
                  string/split-lines
                  last
                  string/trim)]
    (when (seq vtag)
      (subs vtag 1))))

(defn clean []
  (doseq [dir ["target" ".cpcache"]]
    (fs/delete-file-recursively dir true)))

(defn- commit-count-since 
  "Stolen from a ClojurScript bash script along with explanation:

  The command `git describe --match v0.0` will return a string like

  v0.0-856-g329708b

  where 856 is the number of commits since the v0.0 tag. It will always
  find the v0.0 tag and will always return the total number of commits (even
  if the tag is v0.0.1)."
  [version]
  (->>  (shell/command ["git" 
                        "--no-replace-objects" 
                        "describe" 
                        "--match" (str  "v" version)] {:out :string})
        :out
        string/trim
        (re-matches #".*-(\d+)-.*$")
        last
        Long/parseLong))

(defn- dev-specified-version []
  (-> "version.edn"
      slurp
      edn/read-string))

(defn- calculate-version []
  (status/line :info "Calculating release version")
  (let [version-repo (most-recent-released-version)
        major-minor-repo (->> (or version-repo "0.0.0") 
                              version/version-data 
                              (take 2) 
                              (string/join "."))
        version-target (dev-specified-version)
        major-minor-target (str (:major version-target) "." (:minor version-target))]
    (status/line :detail (str "Found version in repo: " (or version-repo "<none>")))
    (status/line :detail (str "Our major.minor target version is: " major-minor-target))
    (let [commit-count (case (version/version-compare major-minor-target major-minor-repo)
                         -1 (status/fatal "Target version is lower than version in repo")
                         0 (inc ;; inc to account for upcoming commit 
                            (commit-count-since major-minor-repo)) 
                         1 0)
          new-version (str major-minor-target "." commit-count 
                           (cond->> (:qualifier version-target)
                             true (str "-")))]
      (status/line :detail (str  "Release version is: " new-version))
      new-version)))

(defn- update-file! [fname match replacement]
  (let [old-content (slurp fname)
        new-content (string/replace old-content match replacement)]
    (if (= old-content new-content)
      (status/fatal (str  "Expected " fname " to be updated."))
      (spit fname new-content))))

(defn- update-readme! [version]
  (status/line :info (str "Updating README usage to show version " version))
  (update-file! "README.adoc"
                #"( +\{:extra-deps \{lread/test-doc-blocks \{:mvn/version \").*(\"\}\})"
                (str  "$1" version "$2")))

(defn- update-changelog! [version]
  (status/line :info (str "Updating Change Log unreleased headers to release " version))
  (update-file! "CHANGELOG.adoc"
               #"(?im)^(=+) +unreleased *$"
               (str "$1 Unreleased\n\n$1 v" version)))

(defn- create-jar! [version]
  (status/line :info (str "Updating pom.xml and creating jar for version " version))
  (shell/command ["clojure" "-X:jar" ":version" (pr-str version)])
  nil)

(defn- assert-on-ci
  "Little blocker to save myself from myself when testing."
  [action]
  (when (not (System/getenv "CI"))
    (status/fatal (format  "We only want to %s from CI" action))))

(defn- deploy-jar!
  "For this to work, appropriate CLOJARS_USERNAME and CLOJARS_PASSWORD must be in environment."
  []
  (status/line :info "Deploying jar to clojars")
  (assert-on-ci "deploy a jar")
  (shell/command ["clojure" "-X:deploy"])
  nil)

(defn- commit-changes! [version]
  (let [tag-version (str "v" version)]
    (status/line :info (str  "Committing and pushing changes made for " tag-version))
    (assert-on-ci "commit changes")
    (status/line :detail "Adding changes")
    (shell/command ["git" "add" "README.adoc" "CHANGELOG.adoc" "pom.xml"])
    (status/line :detail "Committing")
    (shell/command ["git" "commit" "-m" (str  "Release job: updates for version " tag-version)])
    (status/line :detail "Version tagging")
    (shell/command ["git" "tag" "-a" tag-version "-m" (str  "Release " tag-version)])
    (status/line :detail "Pushing commit")
    (shell/command ["git" "push"])
    (status/line :detail "Pushing version tag")
    (shell/command ["git" "push" "origin" tag-version])
    nil))

(defn- inform-cljdoc! [version]
  (status/line :info (str "Informing cljdoc of new version " version))
  (assert-on-ci "inform cljdoc")
  (let [exit-code (->  (shell/command-no-exit ["curl" "-X" "POST"
                                               "-d" "project=lread/test-doc-blocks"
                                               "-d" (str  "version=" version)
                                               "https://cljdoc.org/api/request-build2"])
                       :exit)]
    (when (not (zero? exit-code))
      (status/line :warn (str  "Informing cljdoc did not seem to work, exited with " exit-code)))))

(defn- main []
  (clean)
  (let [target-version (calculate-version)]
    (update-readme! target-version)
    (update-changelog! target-version)
    (create-jar! target-version)
    (deploy-jar!)
    (commit-changes! target-version)
    (inform-cljdoc! target-version)
    (status/line :detail "Release work done.")))

#_(main)

(calculate-version)