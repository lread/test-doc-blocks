(ns ^:no-doc lread.test-doc-blocks.impl.inline-ns
  "Support for finding a removing (require ...), (import ...) and (refer-clojure ...) forms.
  We'll call these forms of interest.

  Inline requires must be toplevel in Clojure and aren't supported outside the REPL for ClojureScript.
  Our end goal is to move inline forms of interest up into our generated test ns declaration to support Clojure and ClojureScript.

  Current strategy is to search for top-level forms that contain a form of interest.
  It should mostly work.

  We'll find:

  (require ...)
  (require #?(...) ?(...)...)
  #?(:... (require ...))

  But we'll also remove these.
  And that won't always be the right thing to do when they are not top level forms.
  The containing form may contain other forms that are not of interest to us.

  We'll start with this strategy and see how it works out."

  (:require [lread.test-doc-blocks.impl.zutil :as zutil]
            [rewrite-clj.zip :as z]))

(defn- list-starting-with-sym? [zloc sym]
  (and (z/list? zloc)
       (= :token (z/tag (z/down zloc)))
       (= sym (z/sexpr (z/down zloc)))) )

(defn- inline-ns-of-interest? [sym]
  (fn pred? [zloc]
    (and (= 1 (zutil/zdepth zloc))
         (z/find (z/subzip zloc)
                 z/next
                 #(list-starting-with-sym? % sym)))))

(defn- find-ns-forms-of-interest
  "Returns forms of interest and direct parents.
  Currently also returns any other of unintest forms in tree."
  [zloc sym]
  (let [zloc-top (z/of-node* (z/root zloc))]
    (->> (z/find zloc-top z/next (inline-ns-of-interest? sym))
         (iterate #(z/find-next % z/next (inline-ns-of-interest? sym)))
         (take-while identity))))

(defn- remove-ns-forms-of-interest
  "Replace forms of interest with nil.
  We replace instead of remove to avoid having to deal with potentially empty reader conditionals."
  [zloc sym]
  (-> zloc
      z/root
      z/of-node*
      (z/prewalk #(list-starting-with-sym? % sym)
                 (fn action [zloc] (z/replace zloc 'nil)))))

(defn find-forms
  "Returns map where `:requires` is a vector of inline `(require ...)` found in `block-text`
  and `:imports` is a vector of inline `(import ...)` found in `block-text`.

  We search at depth of 1 to bring in parent forms which we assume to be do and/or reader conditionals."
  [block-text]
  (let [zloc (z/of-string block-text)]
    {:requires (->> (find-ns-forms-of-interest zloc 'require)
                    (map z/string))
     :imports (->> (find-ns-forms-of-interest zloc 'import)
                   (map z/string))
     :refer-clojures (->> (find-ns-forms-of-interest zloc 'refer-clojure)
                          (map z/string))}))

(defn remove-forms
  "Returns `block-text` with same inline elements found by [[find-forms]] removed."
  [block-text]
  (-> block-text
      z/of-string
      (remove-ns-forms-of-interest 'require)
      (remove-ns-forms-of-interest 'import)
      (remove-ns-forms-of-interest 'refer-clojure)
      z/root-string))
