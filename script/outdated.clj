#!/usr/bin/env bb

(ns outdated
  (:require [antq.report :as antq-report]
            [antq.report.table]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(def pin-clojure-version-pom "1.9.0")

(defn check-clojure []
  (status/line :head "Checking Clojure deps")
  (if-let [outdated (->> (shell/command {:continue true :out :string} 
                                        "clojure -M:outdated")
                         :out
                         string/trim
                         edn/read-string
                         (remove #(and (= "pom.xml" (:file %))
                                       (= "org.clojure/clojure" (:name %))
                                       (= pin-clojure-version-pom (:version %))))
                         seq)]
    (antq-report/reporter outdated {:reporter "table"})
    (status/line :detail "All Clojure dependencies seem up to date.")))

(defn -main [& args]
  (when (main/doc-arg-opt args)
    (check-clojure))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
