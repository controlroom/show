(ns show.core-test
  (:require
    [clojure.test :refer [deftest testing is run-tests]]
    [show.core :as show]))

(println (macroexpand '(show/component "k-thing" [component]
                                       (render [a b]
                                         "n"))))
