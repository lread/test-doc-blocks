(ns ^:no-doc lread.test-doc-blocks.impl.validate
  "Validation support."
  (:require [malli.core :as m]
            [malli.error :as me]
            [malli.util :as mu]))

(defn errors [schema value]
  (-> schema
      (mu/closed-schema)
      (m/explain value)
      (me/with-spell-checking)
      (me/humanize)))
