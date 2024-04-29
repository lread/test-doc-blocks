(ns build
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.tools.build.api :as b]))

(def lib 'com.github.lread/test-doc-blocks)
(def version (let [version-template (-> "version.edn" slurp edn/read-string)
                   patch (b/git-count-revs nil)]
               (str (:major version-template) "."
                    (:minor version-template) "."
                    patch
                    (cond->> (:qualifier version-template)
                      true (str "-")))))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s.jar" (name lib)))
(def built-jar-version-file "target/built-jar-version.txt")

(defn jar
  "Build library jar file.

   Also writes built version to target/built-jar-version.txt for easy peasy pickup by any interested downstream operation.

  We use the optional :version-suffix to distinguish local installs from production releases.
  For example, when preview for cljdoc, we use the suffix: cjdoc-preview."
  [{:keys [version-suffix]}]
  (b/delete {:path class-dir})
  (b/delete {:path jar-file})
  (let [version (if version-suffix (format "%s-%s" version version-suffix)
                    version)]
    (b/write-pom {:class-dir class-dir
                  :lib lib
                  :version version
                  :basis basis
                  :src-dirs ["src"]
                  :scm {:url "https://github.com/lread/test-doc-blocks"
                        :connection "scm:git:git@github.com:lread/test-doc-blocks.git"
                        :developerConnection "scm:git:git@github.com:lread/test-doc-blocks.git"
                        :tag (format "v%s" version)}
                  :pom-data [[:description "Generate Clojure tests from AsciiDoc and CommonMark doc code blocks"]
                             [:url "https://github.com/lread/test-doc-blocks"]
                             [:licenses
                              [:license
                               [:name "Eclipse Public License"]
                               [:url "http://www.eclipse.org/legal/epl-v10.html"]]]
                             [:properties
                              [:project.build.sourceEncoding "UTF-8"]]]})
    (b/copy-dir {:src-dirs ["src" "resources"]
                 :target-dir class-dir})
    (b/jar {:class-dir class-dir
            :jar-file jar-file})
    (spit built-jar-version-file version)))

(defn- built-version* []
  (when (not (.exists (io/file built-jar-version-file)))
    (throw (ex-info (str "Built jar version file not found: " built-jar-version-file) {})))
  (slurp built-jar-version-file))

(defn built-version
  ;; NOTE: Used by release script and github workflow
  "Spit out version of jar built (with no trailing newline).
  A separate task because I don't know what build.tools might spit to stdout."
  [_]
  (print (built-version*))
  (flush))

(defn install
  "Install built jar to local maven repo"
  [_]
  (b/install {:class-dir class-dir
              :lib lib
              :version (built-version*)
              :basis basis
              :jar-file jar-file}))

(defn deploy
  "Deploy built jar to clojars"
  [_]
  ((requiring-resolve 'deps-deploy.deps-deploy/deploy)
    {:installer :remote
     :artifact jar-file
     :pom-file (b/pom-path {:lib lib :class-dir class-dir})}))

(defn project-lib
  "Returns project groupid/artifactid"
  [_]
  (println lib))

(defn download-deps
  "Download all deps for all aliases"
  [_]
  (let [aliases (->> "deps.edn"
                     slurp
                     edn/read-string
                     :aliases
                     keys
                     sort)]
    ;; one at a time because aliases with :replace-deps will... well... you know.
    (println "Bring down default deps")
    (b/create-basis {})
    (doseq [a (sort aliases)]
      (println "Bring down deps for alias" a)
      (b/create-basis {:aliases [a]}))))
