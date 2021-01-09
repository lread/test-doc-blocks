CommonMark test-doc-blocks Example
==========

Test-doc-blocks will find Clojure source code blocks in your documents and generate tests for them.

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
   (println "hey there!")
   ;; stdout=> hey there!
   
   ;; multiple stdout lines can be verified like so (notice the single ;):
   (println "is this right?\nor not?")
   ;; =stdout=>
   ; is this right?
   ; or not?

   ;; sometimes we might care about evaluated result, stderr and stdout
   ;; TODO: reader conditionals makes this example awkward
   (do
     (println "To out I go")
     #?(:clj
        (binding [*out* *err*]
          (println "To err is human"))
        :cljs
        (binding [*print-fn* *print-err-fn*]
          (println "To err is human")))
     (* 9 9))
   ;; => 81
   ;; =stderr=> To err is human
   ;; =stdout=> To out I go
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

It currently understands four options:

- `:test-doc-blocks/apply` - controls to what code blocks options are applied
- `:test-doc-blocks/skip` - skips the next code block
- `:test-doc-blocks/test-ns` - specifies the test namespace 
- `:test-doc-blocks/platform` - specifies Clojure file type to generate for test ns

### Applying Options

By default, new options are applied to the next Clojure code block only.

You can change this by including the `:test-doc-blocks/apply` option:

- `:next` - default - applies new options to next Clojure code block only
- `:all-next` - applies new options to all subsequent code blocks in the document until specifically overridden with new opts

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

If you don't tell test-doc-blocks what namespace you want tests in, it will come up with one based on the document filename.
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

~~~markdown
<!-- {:test-doc-blocks/test-ns example-md-new-ns.ns1-test} -->
```Clojure
;; this code block will generate tests under example-md-new-ns.ns1-test

(require '[clojure.string :as string])

(string/join ", " [1 2 3])
=> "1, 2, 3"
```
~~~

### Specifying The Platform

By default, test-doc-blocks generates `.cljc` tests.

You can override this default on the command line via `:platform` and via inline option via `test-doc-blocks/platform`.
Valid values are:

- `:cljc` - the default - generates `.cljc` test files
- `:clj` - generates `.clj` test files
- `:cljs` - generates `.cljs` test files

When specifying the platform, remember that:

- For Clojure `my-ns-file.clj` will be picked over `my-ns-file.cljc`
- For ClojureScript `my-ns-file.cljs` will be picked over `my-ns-file.cljc`

So if you are generating mixed platforms, you might want to specify the test-ns as well.

~~~markdown
<!-- #:test-doc-blocks{:platform :cljs :test-ns example-md-cljs-test} -->
```Clojure
;; this code block will generate tests under example-md-cljs-test ns to a .cljs file

(import '[goog.events EventType])
EventType.CLICK
;;=> "click"

(require '[goog.math :as math])
(math/clamp -1 0 5)
;;=> 0
```
~~~

### Specifying Metadata
Test runners support including and excluding tests based on truthy metadata.

You can attach metadata to generated tests via the `:test-doc-blocks/meta` option.

A new `:test-doc-blocks/meta` will override any and all previous meta values.

We offer two syntaxes:

- `:test-doc-blocks-meta :my-kw` generates `{:my-kw true}` metadata.
- `:test-doc-blocks-meta {:my-kw1 my-value1 :my-kw2 my-value2}` the explicit option for those that need it

Example code blocks:

~~~markdown
[source,asciidoctor]
<!-- #:test-doc-blocks{:meta :testing-meta123} -->
```clojure
;; this code block will generate a test with metadata {:testing-meta123 true}

user=> (into [] {:a 1})
[[:a 1]]
```
~~~


~~~markdown
<!-- #:test-doc-blocks{:meta {:testing-meta123 "a-specific-value" :testing-meta789 :yip}} -->
```clojure
;; this code block will generate a test with metadata:
;;  {:testing-meta123 "a-specific-value" :testing-meta789 :yip}

(reduce
   (fn [acc n]
     (str acc "!" n))
   ""
   ["oh" "my" "goodness"])
;; => "!oh!my!goodness"
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

# Indented Blocks

CommonMark syntax gives meaning to indented code blocks.


- In CommonMark, a block is indented to be attached to a list item:

    ```Clojure
    ;; we handle simple cases a-OK.
    user=> (+ 1 2 3)
    6
    ``` 

  - It indents more for deeper items:
  
      ```Clojure
      ;; we handle indented wrapped strings just fine
      (def s "my
      goodness
      gracious")
   
      (println s)
      ;; =stdout=>
      ; my
      ; goodness
      ; gracious
      ``` 

# Nuances

## When Libraries Override pr

The REPL makes use of `pr` to output what it has evaluated.
The `pr` docstring states:

> By default, pr and prn print in a way that objects can be read by the reader

Some libraries break this contract.
For example, rewrite-clj overrides `pr` to display output for its nodes that is easily digestable by humans, but not at all digestable by Clojure.

If `pr` has been overriden for your library, you have choices for test-doc-blocks:

1. Skip the block (see inline options)
2. Avoid REPL assertions that effect the overriden pr
3. Have your code blocks include call `pr` on affected evaluations and use `=stdout=>` to compare for expected output.
