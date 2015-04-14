(defproject controlroom/show "0.5.0"
  :description "Minimal clojurescript React implementation"
  :url "http://controlroom.io/show"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2760"]
                 [cljsjs/react "0.12.2-5"]]

  :plugins [[lein-cljsbuild "1.0.4"]])
