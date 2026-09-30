(ns test-gen-kaocha
  (:require [generate-tests]
            [helper.clojure-versions :as clojure-versions]
            [helper.jdk :as jdk]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(def target-clojure-versions (clojure-versions/for-kaocha))

(defn run-generated-tests
  [{:keys [mvn-version alias] :as _clojure-version}]
  (status/line :head "Running generated tests using kaocha with clojure v%s" mvn-version)
  (shell/clojure (format "-M:isolated/kaocha:%s generated" alias)))

(defn task
  {:org.babashka/cli {:spec (clojure-versions/cli-opt
                              (conj (mapv :version target-clojure-versions) "all"))}}
  
  [{:keys [clojure-version]}]
  (generate-tests/task {:clojure-version clojure-versions/min-version-test-generation})
  (let [env-jdk-version (jdk/version)
        clojure-versions (if (= "all" clojure-version)
                           target-clojure-versions
                           [(clojure-versions/lookup clojure-version)])]
    (doseq [v clojure-versions]
      (if (and (= "all" clojure-version)
               (< (:major env-jdk-version) (:min-jdk-major v)))
        (status/line :warn "Skipping testing clojure version %s\nIt requires min JDK %s, found JDK %s"
                     (:mvn-version v) (:min-jdk-major v) (:version env-jdk-version))
        (run-generated-tests v)))))

