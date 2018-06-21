(ns show.dom-test
  (:require
    [clojure.test :as test :refer [deftest testing is]]
    [show.dom :as dom]))

(defn args-eql [process-args-args expected]
  (is (= (apply dom/process-args process-args-args) expected)))

(deftest process-args-test
  (testing "no options in arglist"
    (args-eql '("Foo")
              [{} '("Foo")]))
  (testing "nil options"
    (args-eql '(nil nil)
              [{} nil]))
  (testing 'preproces-opts
    (testing "rewriting :class to :className"
      (args-eql '({:class "bar"})
                [{:className "bar"} nil]))
    (testing "implicit class-map in options"
      (args-eql '({:className {"foo" true}})
                [{:className "foo"} nil]))
    (testing "nested preprocessing (class then class-map)"
      (args-eql '({:class {"Yes" true}})
                [{:className "Yes"} nil]))))

(defn test-creator [& args] args)
(intern 'show.dom 'element-creator test-creator)

(deftest element-test
  (testing "Delegates to element-creator"
    (is (= (dom/element :h2 {:className "foo"} "Bar")
           ["h2" {:className "foo"} '("Bar")]))))
