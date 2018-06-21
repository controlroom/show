(defproject controlroom/show "0.8.0-SNAPSHOT"
  :description "Minimal clojurescript React implementation"
  :url "http://controlroom.io/show"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src"]

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [cljsjs/react "16.4.0-0"]
                 [cljsjs/react-dom "16.4.0-0"]
                 [cljsjs/create-react-class "15.6.3-0"]
                 [cljsjs/react-transition-group "2.3.1-0"]]

  :plugins [[lein-cljsbuild "1.1.7"]]
  :cljsbuild {:builds []})
