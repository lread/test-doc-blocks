(ns example
  "Cljdoc supports CommonMark in docstrings.

   Test doc block will scan docstrings for CommonMark Clojure code blocks and generate tests for them. 

   Test doc blocks currently ONLY looks at doc strings, so if your code block need specific requires, 
   you'll have to include in your at least your first block.

   In this namespace doc string we'll include 2 blocks:

   ```Clojure
   (+ 1 2 3)
   ;; => 6
   ```

   Escape chars hinder readability a bit in plain text, but should look good under cljdoc.

   ```Clojure
   (require '[clojure.string :as string])

   (string/replace \"what,what\" \"what\" \"who\")
   ;; => \"who,who\"
   ```")

(defn fn1 
  "No code blocks so will be skipped" [])

(defn fn2
  "This code block will be found:
   
   ```Clojure
   user=> (* 8 8)
   64
   ```" 
  [])

(defn fn3
  "All the test-doc-block inline options are available, let's try one:

   <!-- :test-doc-blocks/skip -->
   ```Clojure
   (brace yourself
   ```
   
   If the block were not skipped it would not parse and cause a failure."
  [])

(defn fn4
  "Let's try and use that `string` alias that we required in the namespace docstring.

   Escaped embedded quotes are not pretty in plain text, they are handled.   

  ```Clojure 
  (string/upper-case \"all \\\"good\\\"?\")
  => \"ALL \\\"GOOD\\\"?\"
  ```" [_x _y _z])


(comment
  (println "\"hey\\\" dude\"")
  
  
  
  )
