(ns ^:no-doc lread.test-doc-blocks.runtime
  "Test time macros called from generated tests.")

(defn cljs-env?
  "Take the &env from a macro, and tell whether we are expanding into cljs."
  [env]
  (boolean (:ns env)))

(defmacro if-cljs
  "Return then if we are generating cljs code and else for Clojure code.
   https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"
  [then else]
  (if (cljs-env? &env) then else))

(defmacro eval-capture
  [& body]
  `(if-cljs
     (let [out# (goog.string/StringBuffer.)
           err# (goog.string/StringBuffer.)]
       (binding [cljs.core/*print-newline* true
                 cljs.core/*print-fn* (fn [x#] (.append out# x#))
                 cljs.core/*print-err-fn* (fn [x#] (.append err# x#))]
         (let [res# ~@body]
           [res# (str out#) (str err#)])))

     (let [out# (new java.io.StringWriter)
           err# (new java.io.StringWriter)]
        (binding [*out* out#
                  *err* err#]
          (let [res# ~@body]
            [res# (str out#) (str err#)]))) ))
