(ns lread.test-doc-blocks.impl.amalg-ns
  (:require [clojure.set :as cset]
            [clojure.tools.reader :as reader]))

(defn- read-ns-ref [ns-ref feature]
  (rest (reader/read-string
         {:read-cond :allow :features #{ feature }}
         (str (format "(do %s)" ns-ref)))))

(defn- parse-requires [requires feature]
  ;; will not normalize clj shorthand
  (->> requires
       (mapcat #(read-ns-ref % feature))
       (remove empty?)
       (mapcat rest) ;; don't need 'require
       (mapcat rest) ;; don't need the quote
       set))

(defn- parse-imports [imports feature]
  ;; will normalize shorthand because it is common to clj and cljs
  (->> imports
       (mapcat #(read-ns-ref % feature))
       (remove empty?)
       (map rest) ;; don't need 'import
       (mapcat #(if (and (list? %) (list? (first %))) % [%])) ;; flatten
       (map #(if (= 'quote (first %)) (rest %) %)) ;; turf quote if present
       (map #(if (vector? (first %)) (first %) %)) ;; flatten
       (mapcat #(if (seq (rest %))
               (map
                (fn [suffix] (symbol (str (first %) "." suffix)))
                (rest %))
               %))
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
            {:common common}
            platforms)) )

(defn amalg-imports [imports]
  (amalg parse-imports imports))

(defn amalg-requires [requires]
  (amalg parse-requires requires))

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

  )
