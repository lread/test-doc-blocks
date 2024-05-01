#!/usr/bin/env bb

(ns clean
  (:require [babashka.fs :as fs]
            [helper.main :as main]))

(defn clean! []
  (println "Deleting (d=deleted -=did not exist)")
  (run! (fn [d]
          (println (format "[%s] %s"
                           (if (fs/exists? d) "d" "-")
                           d))
          (fs/delete-tree d))
        ["target"
         ".cpcache"]))

(defn -main [& args]
  (when (main/doc-arg-opt args)
    (clean!))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
