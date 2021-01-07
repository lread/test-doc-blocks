(ns lread.test-doc-blocks.kaocha.hooks)

(defn fail-on-no-tests-found [suite _test-plan]
  (if (seq (:kaocha.test-plan/tests suite))
    suite
    (throw (ex-info "No tests found." {}))))
