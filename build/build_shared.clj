(ns build-shared
  "a few things that are both needed by bb script code and build.clj"
  (:require [clojure.edn :as edn]))

(defn- project-info []
  (-> (edn/read-string (slurp "deps.edn"))
      :aliases :neil :project))

(def version-tag-prefix "v")

(defn lib-version []
  (-> (project-info) :version))

(defn lib-artifact-name []
  (-> (project-info) :name))

(defn lib-github-coords []
  (-> (project-info) :github-coords))

(defn version->tag [version]
  (str version-tag-prefix version))
