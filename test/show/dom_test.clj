(ns show.dom-test
  (:require
    [clojure.test :as test :refer [deftest testing is]]
    [show.dom :as dom]))

(defn args-eql [process-args-args expected]
  (is (= (apply dom/process-args process-args-args) expected)))

(defn test-convert-to-js [args]
  (with-meta args {:js true}))
(intern 'show.dom 'convert-to-js test-convert-to-js)

(defn test-fragment [body]
  (with-meta body {:fragment true}))
(intern 'show.dom 'fragment test-fragment)

(deftest process-args-test
  (testing "no options in arglist"
    (args-eql '("Foo")
              [{} "Foo"]))
  (testing "nil options"
    (args-eql '(nil nil)
              [{} nil]))
  (testing "passes multiple body items"
    (args-eql '({} "foo" "bar")
              [{} '("foo" "bar")]))
  (testing "wraps multiple body objects in React.Fragment element"
    (let [[_ body] (dom/process-args {} {:a :a} {:b :b})]
      (is (:fragment (meta body)))))
  (testing "passes along array bodies"
    (let [[_ body] (dom/process-args {} [{:a :a} {:b :b}])]
      (is (not (:fragment (meta body))))))
  (testing "calls 'convert-to-js on nested hashes"
    (let [[{:keys [nested-hash]} _] (dom/process-args {:nested-hash {:b "string"}})]
      (is (= (meta nested-hash) {:js true}))))
  (testing 'preproces-opts
    (testing "rewriting :class to :className"
      (args-eql '({:class "bar"})
                [{:className "bar"} nil]))
    (testing "implicit class-map in options"
      (args-eql '({:className {"foo" true}})
                [{:className "foo"} nil]))
    (testing "nested preprocessing (class then class-map)"
      (args-eql '({:class {"Yes" true}})
                [{:className "Yes"} nil]))
    (testing "returns nil class if no classname clauses are true"
     (args-eql '({:class {"here" false}})
                [{:className nil} nil]))))

(defn test-creator [& args] args)
(intern 'show.dom 'element-creator test-creator)

(deftest element-test
  (testing "calls 'convert-to-js on opts & body"
    (let [[_ arg1 arg2] (dom/element :noop {:foo :bar} {:body "string"})]
      (every? #(true? (:js (meta %))) [arg1 arg2])) )
  (testing "Delegates to element-creator"
    (is (= (dom/element :h2 {:className "foo"} {:body "Bar"})
           ["h2" {:className "foo"} {:body "Bar"}]))))
