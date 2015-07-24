(set-env!
 :resource-paths #{"src"}
 :dependencies '[[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "0.0-3308"]
                 [cljsjs/react-with-addons "0.13.3-0"]])

(def +version+
  "0.6.0-SNAPSHOT")

(def +description+
  "Minimal clojurescript React implementation")

(task-options!
  pom {:project     'controlroom/show
       :version     +version+
       :description +description+
       :url          "http://controlroom.io/show"
       :license     {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}
       :scm         {:url "https://github.com/controlroom/show"}})

(deftask watch-install []
  (comp (watch) (pom) (jar) (install)))
