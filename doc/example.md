CommonMark test-doc-blocks Example
==========

If you are writing your docs in CommonMark, read on.
If you are writing your docs in AsciiDoc, you'll want to read [AsciiDoc test-doc-blocks Example](/doc/example.adoc) instead.

Test-doc-blocks will find Clojure source code blocks in your docstrings and `.md` files and generate tests for them.

You tell test-doc-blocks which files to scan on the [command line](/doc/01-user-guide.adoc#command-line).

The Basics
--
Test-doc-blocks generates a test for each Clojure code block.  An example of a CommonMark Clojure code block:
~~~markdown
```Clojure
;; Some valid Clojure code here
```
~~~

Here we used `Clojure` as the code block language, but all code blocks with a (case insensitive) language of `clj` `cljs` `cljc` or starting with `Clojure` are recognized as Clojure code blocks.

Within your Clojure code blocks, test-doc-blocks automatically generates assertions for:

- REPL session style prompts:

    ```Clojure
    ;; test-doc-block will generate an assertion to verify (+ 1 2 3) evaluates to the expected 6
    user=> (+ 1 2 3)
    6
    ```
- Editor style eval to comment, with some extras:

   ```cljc
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

   ;; multiple stdout lines can be verified like so (notice the use of single ;):
   (println "is this right?\nor not?")
   ;; =stdout=>
   ; is this right?
   ; or not?
   ```

   Sometimes we might care about evaluated result, stderr and stdout.
   <!-- #:test-doc-blocks {:platform :clj :test-ns example-md-out-test} -->
   ```clj
   ;; A snippit of Clojure where we check result, stderr and stdout
   (do
     (println "To out I go")
     (binding [*out* *err*] (println "To err is human"))
     (* 9 9))
   ;; => 81
   ;; =stderr=> To err is human
   ;; =stdout=> To out I go
   ```

   <!-- #:test-doc-blocks {:platform :cljs :test-ns example-md-out-test} -->
   ```cljs
   ;; And the same idea for ClojureScript
   (do
     (println "To out I go")
     (binding [*print-fn* *print-err-fn*] (println "To err is human"))
     (* 9 9))
   ;; => 81
   ;; =stderr=> To err is human
   ;; =stdout=> To out I go
   ```

Test-doc-blocks will plunk your code blocks into a test even when there are no assertions.
It is sometimes just nice to know that your code snippet will run without exception.

```clojure
(->> "here we are only checking that our code will run"
     reverse
     reverse
     (apply str))
```

## Inline Options
You can, to some limited extent, communicate your intent to test-doc-blocks.

Test-doc-blocks will look for CommonMark comments that begin with `:test-doc-blocks`.

It currently understands the following options:

- `:test-doc-blocks/apply` - controls to what code blocks options are applied
- `:test-doc-blocks/skip` - skips the next code block
- `:test-doc-blocks/reader-cond` - wraps your code block in a reader conditional
- `:test-doc-blocks/test-ns` - specifies the output test namespace
- `:test-doc-blocks/platform` - specifies Clojure file type to generate for test ns
- `:test-doc-blocks/meta` - attach metadata to generated tests

### Applying Options - :apply

By default, new options are applied to the next Clojure code block only.

You can change this by including the `:test-doc-blocks/apply` option:

- `:next` - default - applies new options to next Clojure code block only
- `:all-next` - applies new options to all subsequent code blocks in the document until specifically overridden with new opts

### Skipping Code Blocks - :skip

By default, test-doc-blocks will create tests for all Clojure code blocks it finds in the files you specified.

Tell test-doc-blocks to skip the next Clojure code block via the following CommonMark comment:

~~~markdown
<!-- :test-doc-blocks/skip -->
```Clojure
;; no tests will be generated for this code Clojure code block

(something we don't want to test)
```
~~~

### Wrap Test in a Reader Conditional - :reader-cond

A cljc library might want to explain ClojureScript vs Clojure usage without using reader conditionals in the code block.

To wrap the generated test for your code block in a reader conditional use the `:test-doc-blocks/reader-conditional` inline option.

This can be especially handy to show differences in `(requires ...)` for clj and cljs in separate code blocks.
Here's a contrived example:

Clojure specific code:
~~~markdown
<!-- #:test-doc-blocks {:reader-cond :clj} -->
```Clojure
;; This code block will be wrapped in a #?(:clj (do ...))
(refer-clojure :exclude '[read-string])
(require '[clojure.edn :refer [read-string]])
```
~~~

ClojureScript specific code:
~~~markdown
<!-- #:test-doc-blocks {:reader-cond :cljs} -->
```Clojure
;; This code block will be wrapped in a #?(:cljs (do ...))
(require '[cljs.reader :refer [read-string]])
```
~~~

Later in doc, cross-platform cljc code that relies on the above:
~~~markdown
```Clojure
;; And our generic cljc code:
(read-string "[1 2 3]")
=> [1 2 3]
```
~~~

Test-doc-blocks does no special checking, but `:reader-cond` only makes sense for `:cljc` platform code blocks and when your code block contains no reader conditionals.

<a name="test-ns"></a>
### Specifying Test Namespace - :test-ns

By default, test-doc-blocks will generate tests to namespaces based on document filenames.
This file is named `example.md`. Test-doc-blocks, up to this point has been generating tests to the `example-md-test` namespace.

If this does not work for you, you can override this default via a CommonMark comment:

~~~markdown
<!-- {:test-doc-blocks/test-ns example-md-new-ns-test} -->
```Clojure
;; this code block will generate tests under example-md-new-ns-test

user=> (* 2 4)
8
```
~~~

💡 Do what you like, but test runners usually look for tests namespaces ending in `-test`.

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

### Specifying The Platform - :platform

By default, test-doc-blocks generates `.cljc` tests.

You can override this default on the command line via `:platform` and via an inline `:test-doc-blocks/platform`.
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

### Specifying Metadata - :meta
Test runners support including and excluding tests based on truthy metadata.

You can attach metadata to generated tests via the `:test-doc-blocks/meta` option.

A new `:test-doc-blocks/meta` will override any previous meta values.

We offer two syntaxes:

- `:test-doc-blocks-meta :my-kw` generates `{:my-kw true}` metadata.
- `:test-doc-blocks-meta {:my-kw1 my-value1 :my-kw2 my-value2}` the explicit option for those that need it

Example code blocks:

~~~markdown
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

This code block should include "Section Titles" as part of the context for its generated test.

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

## Inline requires and imports

It is common for REPL style code block examples to include inline requires and imports.

Test-doc-blocks will make an honest attempt to lift these inline requires up into the ns declaration of the generated test.
This allows the generated tests to be run by ClojureScript which only supports inline requires in the REPL.

Test-doc-blocks should be able to handle common import and require formats.
If we've missed some, let us know.

<!-- #:test-doc-blocks{:test-ns example-md-inline-ns-test} -->
```Clojure
;; Stick the basics for requires, shorthand notation isn't supported

;; Some examples:
(require '[clojure.string :as string])
(require '[clojure.string])
(require 'clojure.string)
(require '[clojure.string :as string] '[clojure.set :as cset])

;; For cljc code examples it is fine for your requires and imports to contain, or be wrapped by, reader conditionals

;; Some examples of supported imports
#?@(:clj [(import 'java.util.List)
          (import '[java.util List Queue Set])]
    :cljs [(import 'goog.math.Long '[goog.math Vec2 Vec3])])
```

It is important to remember that inline requires and imports are amalgamated across all code blocks in a doc to the target test namespace.

If you need to, you can control your target test namespace for code blocks via the [:test-ns](#test-ns) inline option.

## Inline refer-clojure

Sometimes your Clojure code blocks will make use of inline `refer-clojure` calls.
Like `require` and `import` calls, test-doc-blocks will attempt to lift these up to the ns declaration of the generated test.

A library that encourages the use of `:refers` for its API will often include a code block with the suggested `(refer-clojure :exclude [...])` near the top of its documents.

<!-- #:test-doc-blocks{:test-ns example-md-inline-refer-clojure-test} -->
```Clojure
;; a contrived example that uses uses clojure.edn/read-string in place of clojure.core/read-string
;; and excludes clojure.core/for
(refer-clojure :exclude '[for read-string])
(require '[clojure.edn :refer [read-string]])

;; our own for
(defn for [x]
  (* 4 x))

(-> #'read-string meta :ns ns-name str)
;; => "clojure.edn"

(read-string "[1 2 3]")
;; => [1 2 3]

(for 4)
;; => 16
```

It is important to remember than inline `refer-clojure` calls are amalgamated across all code blocks in a doc to the target test namespace.

Test-doc-blocks will fail test generation if it finds more than one `refer-clojure` call per target test namespace per platform (i.e. :clj :cljs).

If you need to, you can control your target test namespace for code blocks via the [:test-ns](#test-ns) inline option.

## Test Run Order
In the general case, running tests in no specific or random order is a good thing.
In the case of test-doc-blocks, this might not be what you want.

If your code blocks are self-contained examples, then test run order won't be an issue for you.
If your separate code blocks represent a larger flow, then order is important.

If we start in one code block...

```clojure
(defn fn-block1 [] (+ 1 2 3))
```

...and continue in another:
```clojure
;; and we continue in this block

(def var-block2 (+ 4 5 6))

(+ (fn-block1) var-block2)
;; => 21
```

...and then maybe another:
```clojure
(+ (fn-block1) var-block2 79)
;; => 100
```

... then run order is important to your generated tests.

Test-doc-blocks makes use of `test-ns-hook` in generated tests to specify the run order be the same as the doc blocks order in your documents.

Kaocha does not support `test-ns-hook`.
It will by default randomize the order of tests for each test run.
For Kaocha, randomization can be disabled from the command line via `--no-randomize` or in its `tests.edn` via `:randomize? false`.

## When Libraries Override pr

The REPL makes use of `pr` to output what it has evaluated.
The `pr` docstring states:

> By default, pr and prn print in a way that objects can be read by the reader

Some libraries break this contract.
For example, rewrite-clj overrides `pr` to display output for its nodes that is easily digestible by humans, but not at all digestible by Clojure.

If `pr` has been overridden for your library, you have choices for test-doc-blocks:

1. Skip the block (see inline options)
2. Avoid REPL assertions that affect the overridden pr
3. Have your code blocks include call `pr` on affected evaluations and use `=stdout=>` to compare for expected output.
