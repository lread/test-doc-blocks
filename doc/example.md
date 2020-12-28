CommonMark test-doc-blocks Example
==========

The Basics
--

Assertions are automatically generated for:

- REPL session code blocks

    ```Clojure
    ;; test-doc-block will generate an assertion to verify (+ 1 2 3) evaluates to the expected 6
    user=> (+ 1 2 3)
    6
    ``` 
- Editor style code blocks currently offer more features:

   ```Clojure
   ;; test-doc-blocks will generate an assertion to verify (+ 1 2 3 4) evaluates to the expected 10
   (+ 1 2 3 4)
   ;; => 10

   ;; it understands that Clojure and ClojureScript can evaluate differently
   \C
   ;; =clj=> \C
   ;; =cljs=> "C"

   ;; and verifies, when asked, to check what was written to stdout
   (println "is this right?\nor not?")
   ;; =stdout=>
   ;; is this right?
   ;; or not?
   
   ;; finish your stdout block with an empty or non-comment line
   ```

Test-doc-blocks will plunk your code blocks into a test even when there are no assertions.
It is sometimes just nice to know that your code snippit will run without exception.

```clojure
(->> "here we are only checking that our code will run"
     reverse
     reverse
     (apply str))
```

## Inline Options
You can, to some limited extent, communicate your intent to test-doc-blocks.

Test-doc-blocks will look for CommonMark comments that begin with `:test-doc-blocks`.

- `:test-block-opts/skip` - skips the next code block
- `:test-block-opts/test-ns` - specifies the test namespace for subsequent code blocks

### Skipping Code Blocks

By default test-doc-blocks will create tests for all Clojure code blocks it finds.

Tell test-doc-blocks to skip the next Clojure code block via the following CommonMark comment:

~~~markdown
<!-- :test-doc-blocks/skip -->
```Clojure
;; no tests will be generated for this code Clojure code block

(something we don't want to test)
```
~~~

### Specifying Test Namespace

If you don't tell test-doc-blocks what namespace you want tests in, it will do its best to come up with one based on the document filename.
For this file, test-doc-blocks, up to this point has been generating tests to `example-md-test`.

If this does not work for you, you can override this default via a CommonMark comment:

~~~markdown
<!-- {:test-doc-blocks/test-ns example-md-new-ns-test} -->
```Clojure
;; this code block will generate tests under example-md-new-ns-test

user=> (* 2 4)
8
```
~~~

:bulb: Do what you like, but test runners usually look for tests namespaces ending in `-test`.

The `:test-doc-blocks/test-ns` option applies to all subsequent code blocks in the document.

Changing the test-ns is useful for code blocks that need to be isolated.
Code blocks that require a unique set of namespaces fall under this category.

~~~markdown
<!-- {:test-doc-blocks/test-ns example-md-new-ns.ns1-test} -->
```Clojure
;; this code block will generate tests under example-md-new-ns.ns1-test

(require '[clojure.string :as string])

(string/join ", " [1 2 3])
=> "1, 2, 3"
```
~~~

# Section Titles

Test-doc-blocks will try to give each test block some context by including its filename, section title and starting line number.

This code block should be include "Section Titles" as part of the context for its generated test.

~~~markdown
```Clojure
(require '[clojure.string :as string])

(string/join "!" ["well" "how" "about" "that"])
;; => "well!how!about!that"
```
~~~
