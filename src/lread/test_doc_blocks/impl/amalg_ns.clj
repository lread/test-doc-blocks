(ns ^:no-doc lread.test-doc-blocks.impl.amalg-ns
  "Support for amalgamating refs from found (require ...), (import ...) and (refer-clojure ...) forms.

  Here we try to amalgate the forms of interest we found via lread.test-doc-blocks.impl.inline-ns.
  The end goal is to plunk these into the generated test ns declaration.

  Some attempt is made to cover popular syntaxes.
  But none yet to support, for example libspec for require.
  For example we understand:

  (require 'clojure.string)
  (require '[clojure.string :as str])

  But do not handle:
  (require ['clojure.string :as 'str])
  (require '(clojure string))

  Amalgation is not too ambitious, it will remove duplicates, but the following, for example, will remain distinct:

  '[clojure.string :as string]
  '[clojure.string]

  As long as aliases do not clash, the code should compile.

  Clashes will occur for things like the following contrived example:

  '[clojure.string :as string]
  '[clojure.set :as string]"
  (:require [clojure.set :as cset]
            [clojure.tools.reader :as reader]))

(defn- read-ns-ref [ns-ref feature]
  (rest (reader/read-string
         {:read-cond :allow :features #{ feature }}
         (format "(do %s)" ns-ref))))

(defn- parse-requires [requires feature]
  ;; will not normalize clj shorthand at this time
  (->> requires
       (mapcat #(read-ns-ref % feature))
       (tree-seq seq? identity)
       (filter list?)
       (filter #(= 'require (first %)))
       (remove empty?)
       (mapcat rest) ;; don't need 'require
       (mapcat rest) ;; don't need the quote
       set))

(defn- parse-imports [imports feature]
  ;; will normalize shorthand because it is common to clj and cljs
  (->> imports
       (mapcat #(read-ns-ref % feature))
       (tree-seq seq? identity)
       (filter list?)
       (filter #(= 'import (first %)))
       (remove empty?)
       (map rest) ;; don't need 'import
       (mapcat #(if (and (list? %) (list? (first %))) % [%])) ;; flatten
       (map #(if (= 'quote (first %)) (rest %) %)) ;; turf quote if present
       (map #(if (vector? (first %)) (first %) %)) ;; flatten
       (mapcat #(if (seq (rest %)) ;; normalize shorthand to fully qualified
               (map
                (fn [suffix] (symbol (str (first %) "." suffix)))
                (rest %))
               %))
       set))

(defn- parse-refer-clojures [refers feature]
  (->> refers
       (mapcat #(read-ns-ref % feature))
       (tree-seq seq? identity)
       (filter list?)
       (filter #(= 'refer-clojure (first %)))
       set))

(defn- amalg [parse-fn inputs]
  (let [platforms [:clj :cljs :cljr]
        m (reduce (fn [acc platform]
                    (assoc acc platform (parse-fn inputs platform)))
                  {}
                  platforms)
        common (apply cset/intersection (vals m))]
    (reduce (fn [acc platform]
              (let [d (cset/difference (platform m) common)]
                (if (seq d)
                  (assoc acc platform d)
                  acc)))
            {:default common}
            platforms)) )

(defn amalg-imports [imports]
  (amalg parse-imports imports))

(defn amalg-requires [requires]
  (amalg parse-requires requires))

(defn amalg-refer-clojures [refer-clojures]
  (amalg parse-refer-clojures refer-clojures))


(comment

  ;; ok, next (import ...)
  (def imports ["(import java.util Date)"
                "(#?(:clj (import java.util.logging Logger
Level)))"
                "(import [org.apache.commons.codec.digest DigestUtils])"
                "#?(:cljs (import 'goog.math.Long
        '[goog.math Vec2 Vec3]
        '[goog.math Integer]))"])

  (amalg-imports imports)

  (def requires ["(require '[boo :as ba])"
                 "(#?(:clj (require '[foo :as fa])))"
                 "(require 'moo.dog.hog)"
                 "(require '[coo :as ca])"
                 "(require '[x :as y] #?(:cljs '[w :as w] :clj '[w1 :as :w1]))"
                 "(require '[doo :as da])"
                 "(require '[coo :as ca])"])

  (amalg-requires requires)
  (amalg-requires [])

  (def refers [#_#_#_"(refer-clojure :excludes '[map for filter])"
               "(refer-clojure :excludes '[map for filter])"
               "(refer-clojure :excludes '[for])"
               "(#?(:clj  (refer-clojure :excludes '[blap])))"])

  (amalg-refer-clojures refers))
