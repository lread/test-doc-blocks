(ns lread.test-doc-blocks.runtime
  "Test time macros called from generated tests."
  (:require [clojure.string :as str]
            #?(:clj [clojure.test]
               :cljs [cljs.test :include-macros true])))

(defn cljs-env?
  "Take the &env from a macro, and tell whether we are expanding into cljs."
  [env]
  (boolean (:ns env)))

(defmacro if-cljs
  "Return then if we are generating cljs code and else for Clojure code.
   https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"
  [then else]
  (if (cljs-env? &env) then else))

(defn- assertion-token? [t]
  (some #{t} '(=> =stdout=> =clj=> =cljs=>)))

(defn- parse-testing-block-forms
  "Given a series of Clojure forms returns a vector of maps describing test:

  {:type :passthrough :form (the form to pass through as is)}

  {:type :assertion   :actual (form to eval)
                      :expected {assert-sym (expected value) }}

  Supported assert-sym keys:
  REPL Style:
  - user=>
  Editor style:
  - =>
  - =clj=>
  - =cljs=>
  - =stdout=>"
  [forms]
  (loop [acc []
         [f1 f2 f3 & more :as forms] forms]
    (cond (< (count forms) 3)
          (if (seq forms)
            (concat acc (map (fn [f] {:type :passthrough :form f}) forms))
            acc)

          (= 'user=> f1)
          (recur (conj acc {:type :assertion
                            :actual f2
                            :expected {'=> f3}})
                 more)

          (assertion-token? f2)
          (let [expected-pairs (->> forms
                                    (drop 1)
                                    (partition 2)
                                    (take-while #(assertion-token? (first %))))
                expected (->> expected-pairs (map vec) (into {}))]
            (recur (conj acc {:type :assertion
                              :actual f1
                              :expected expected})
                   (->> forms
                        (drop 1)
                        (drop (* 2 (count expected-pairs))))))
          :else
          (recur (conj acc {:type :passthrough :form f1}) (rest forms)))))

(defmacro is [form]
  `(if-cljs
     (cljs.test/is ~form)
     (clojure.test/is ~form)))

(defmacro deftest [name & body]
  `(if-cljs
     (cljs.test/deftest ~name ~@body)
     (clojure.test/deftest ~name ~@body)))

(defmacro testing-sym []
  `(if-cljs
    'cljs.test/testing
    'clojure.test/testing))

(defn- transform-to-testing-form-bodies [test]
  (reduce (fn [acc t]
            (case (:type t)
              :passthrough (conj acc (:form t))
              :assertion (let [a (:actual t)
                               assertions (->> (for [[etype e] (select-keys(:expected t)
                                                                           ['=> '=clj=> '=cljs=> '=stdout=>])]
                                                 (case etype
                                                   => `(is (~'= ~e (~'pr-str ~a)))
                                                   =clj=> `(if-cljs nil (is (~'= ~e (~'pr-str ~a))))
                                                   =cljs=> `(if-cljs (is (~'= ~e (~'pr-str ~a))) nil)
                                                   =stdout=> `(is (~'= ~e (str/split-lines (~'with-out-str ~a))))))
                                               (keep identity))]
                           (apply conj acc assertions))))
          []
          test))

(defn- add-dummy-assertion-when-none-present
  [parsed-forms]
  (if (some #(= :assertion (:type %)) parsed-forms)
    parsed-forms
    (concat parsed-forms [{:type :assertion :expected '{=> "\"dummy\""} :actual "dummy"}])))

(defmacro testing-block
  "testing macro, is a far as I can tell, no designed for wrapping, so here we'll just return body and expand appropriately in deftest-doc-blocks macro. "
  ;; TODO: I think this is actually not called
  [& body]
  `'(~@body))

(defmacro deftest-doc-blocks
  ;; TODO: Supporting clojure.test vs cljs.test is gross in here...
  ;; TODO: there must be a better way.
  "Wrapper for deftest that understands readme examples."
  [& body]
  (let [testing-forms (->> body
                           (map parse-testing-block-forms)
                           (map add-dummy-assertion-when-none-present)
                           (map transform-to-testing-form-bodies))]

    `(if-cljs
       (do (deftest ~'test-blocks ~@(map #(cons 'cljs.test/testing (rest %)) testing-forms)))
       (do (deftest ~'test-blocks ~@(map #(cons 'clojure.test/testing (rest %)) testing-forms))))))
