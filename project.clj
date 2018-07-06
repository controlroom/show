(defproject controlroom/show "0.8.0-SNAPSHOT"
  :description "Minimal clojurescript React implementation"
  :url "http://controlroom.io/show"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.329"]

                 [cljsjs/react "16.4.0-0"]
                 [cljsjs/react-dom "16.4.0-0"]
                 [cljsjs/react-dom-server "16.4.0-0"]
                 [cljsjs/create-react-class "15.6.3-0"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-cloverage "1.0.10"]]

  :cljsbuild
  {:builds
   [{:source-paths ["src"]
     :compiler {:main show.core-test
                :target nodejs
                :npm-deps {"react-test-renderer" "16.4.1"}
                :install-deps true}}]})
