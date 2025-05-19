(ns dev-repl)

(ns dev-repl
  (:require [babashka.cli :as cli]
            [babashka.process :as process]
            [clojure.string :as str]
            [helper.main :as main]
            [lread.status-line :as status]))

(def cli-spec {:help {:desc "This usage help"}

               :flowstorm {:alias :f
                           :coerce :boolean
                           :desc "Enable flowstorm"}

               ;; cider nrepl pass through opts
               :host {:ref "<ADDR>"
                      :alias :h
                      :default "127.0.0.1"
                      :desc "Host address"}
               :bind {:ref "<ADDR>"
                      :alias :b
                      :default "127.0.0.1"
                      :desc "Bind address"}
               :port {:ref "<symbols>"
                      :coerce :int
                      :default 0
                      :alias :p
                      :desc "Port, 0 for auto-select"}})

(defn- usage-help[]
  (status/line :head "Usage help")
  (status/line :detail (cli/format-opts {:spec cli-spec :order [:flowstorm :host :bind :port :help]})))

(defn- usage-fail [msg]
  (status/line :error msg)
  (usage-help)
  (System/exit 1))

(defn- parse-opts [args]
  (let [opts (cli/parse-opts args {:spec cli-spec
                                   :restrict true
                                   :error-fn (fn [{:keys [msg]}]
                                               (usage-fail msg))})]
    (when-let [extra-gunk (-> (meta opts) :org.babashka/cli)]
      (usage-fail (str "unrecognized on the command line: " (pr-str extra-gunk))))
    opts))


(defn launch-repl [args]
  (let [{:keys [flowstorm host bind port help]} (parse-opts args)]
    (if help
      (usage-help)
      (let [aliases (cond-> []
                      flowstorm (conj "flowstorm"))]
        (status/line :head "Launching Clojure nREPL")
        (when flowstorm
          (status/line :detail "Flowstorm support is enabled"))
        (process/exec "clj" (str "-M:1.12:kaocha:nrepl:" (str/join ":" aliases))
                      "-h" host
                      "-b" bind
                      "-p" port)))))

;; Entry points
(defn -main [& args]
  (launch-repl args))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
