(require '[cljs.build.api :as b])

(b/build (b/inputs "src" "test")
  {:target :nodejs
   :main 'show.core-test
   :output-to "target/test/test.js"
   :output-dir "target/test/out"
   :parallel-build true
   :compiler-stats true
   :npm-deps
     {"react" "16.4.1"
      "react-dom" "16.4.1"
      "react-test-renderer" "16.4.1"
      "create-react-class" "15.6.3" }
   :install-deps true
   :static-fns true
   ;; :verbose true
   })

(System/exit 0)
