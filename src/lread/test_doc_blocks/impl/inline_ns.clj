(ns lread.test-doc-blocks.impl.inline-ns
  "Support for finding a removing (require ...) and (import ...) forms."
  (:require [lread.test-doc-blocks.impl.zutil :as zutil]
            [rewrite-cljc.zip :as z]))

(defn- list-starting-with-sym? [zloc sym]
  (and (z/list? zloc)
       (= :token (z/tag (z/down zloc)))
       (= sym (z/sexpr (z/down zloc)))) )

;; TODO: think about this: here were find top level forms that include lists starting with require or import
;; the idea was to pick up reader conditonal wrapped requires and imports
;; but those reader conditional may also contain other things
(defn- inline-ns-of-interest? [sym]
  (fn pred? [zloc]
    (and (= 1 (zutil/zdepth zloc))
         (z/find (z/subzip zloc)
                 z/next
                 #(list-starting-with-sym? % sym)))))

(defn- find-ns-forms-of-interest
  [zloc sym]
  (let [zloc-top (z/edn* (z/root zloc))]
    (->> (z/find zloc-top z/next (inline-ns-of-interest? sym))
         (iterate #(z/find-next % z/next (inline-ns-of-interest? sym)))
         (take-while identity))) )

(defn- remove-ns-forms-of-interest
  [zloc sym]
  (-> zloc
      z/root
      z/edn*
      (z/prewalk (inline-ns-of-interest? sym)
                 (fn action [zloc] (z/remove zloc)))))

(defn find-forms
  "Returns map where `:requires` is a vector of inline `(require ...)` found in `block-text`
  and `:imports` is a vector of inline `(import ...)` found in `block-text`.

  We search a max depth of 1 deep to catch requires that are wrapped by reader conditionals."
  [block-text]
  (let [zloc (z/of-string block-text)]
    {:requires (->> (find-ns-forms-of-interest zloc 'require)
                    (map z/string))
     :imports (->> (find-ns-forms-of-interest zloc 'import)
                   (map z/string))}))

(defn remove-forms
  "Returns `block-text` with same inline elements found by [[find-forms]] removed."
  [block-text]
  (-> block-text
      z/of-string
      (remove-ns-forms-of-interest 'require)
      (remove-ns-forms-of-interest 'import)
      z/root-string))
