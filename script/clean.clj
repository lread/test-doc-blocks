#!/usr/bin/env bb

(ns clean
  (:require [babashka.fs :as fs]
            [helper.main :as main]))

(defn clean! []
  (doseq [dir ["target" ".cpcache"]]
    (println "Deleting" dir)
    (when (fs/exists? dir)
      (fs/delete-tree dir))))

(defn -main [& args]
  (when (main/doc-arg-opt args)
    (clean!))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
