(ns test-gen-all
  (:require [helper.clojure-versions :as clojure-versions]
            [test-gen-clj]
            [test-gen-cljs]
            [test-gen-kaocha]))

(defn task [_opts]
  (test-gen-kaocha/task {:clojure-version clojure-versions/min-version-kaocha})
  (test-gen-cljs/task {:clojure-version (-> (clojure-versions/all) first :version)})
  (test-gen-cljs/task {}))
