(ns test-integration
  (:require [helper.clojure-versions :as clojure-versions]
            [helper.jdk :as jdk]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(def target-clojure-versions (clojure-versions/for-test-generation))

(defn run-integration-test
  [{:keys [mvn-version alias] :as _clojure-version}]
  (status/line :head "Verifying under clojure v%s" mvn-version)
  (shell/clojure (format "-M:kaocha:%s integration" alias)))

(defn task
  {:org.babashka/cli {:spec (clojure-versions/cli-opt
                              (conj (mapv :version target-clojure-versions) "all"))}}
  [{:keys [clojure-version]}]
  (let [env-jdk-version (jdk/version)
        clojure-versions (if (= "all" clojure-version)
                           target-clojure-versions
                           [(clojure-versions/lookup clojure-version)])]
    (doseq [v clojure-versions]
      (if (and (= "all" clojure-version)
               (< (:major env-jdk-version) (:min-jdk-major v)))
        (status/line :warn "Skipping testing clojure version %s\nIt requires min JDK %s, found JDK %s"
                     (:mvn-version v) (:min-jdk-major v) (:version env-jdk-version))
        (run-integration-test v)))))

