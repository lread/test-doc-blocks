(ns lread.test-doc-blocks.impl.inline-ns
  (:require [rewrite-cljc.zip :as z]))

(defn- zdepth [zloc]
  (->> (z/up zloc)
       (iterate z/up)
       (take-while identity)
       count))

(defn- list-starting-with-sym? [zloc sym]
  (and (z/list? zloc)
       (= :token (z/tag (z/down zloc)))
       (= sym (z/sexpr (z/down zloc)))) )

(defn- inline-ns-of-interest? [sym]
  (fn pred? [zloc]
    (and (= 1 (zdepth zloc))
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



(defn find-forms [block-text]
  (let [zloc (z/of-string block-text)]
    {:requires (->> (find-ns-forms-of-interest zloc 'require)
                    (map z/string))
     :imports (->> (find-ns-forms-of-interest zloc 'import)
                   (map z/string))}))

(defn remove-forms [block-text]
  (-> block-text
      z/of-string
      (remove-ns-forms-of-interest 'require)
      (remove-ns-forms-of-interest 'import)
      z/root-string))
