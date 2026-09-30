(ns gen-to-expected
  (:require [helper.shell :as shell]))

(defn task [_opts]
  (shell/clojure "-X:test-doc-blocks:test-opts:regen-opts gen-tests"))
