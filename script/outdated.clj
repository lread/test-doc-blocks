#!/usr/bin/env bb

(ns outdated
  (:require [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn check-clojure []
  (status/line :head "Checking Clojure deps")
  (shell/command {:continue true}
                 "clojure -M:outdated"))

(defn -main [& args]
  (when (main/doc-arg-opt args)
    (check-clojure))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
