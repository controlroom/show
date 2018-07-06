(ns show.core-test
  (:require-macros
    [show.core :refer [component]])
  (:require
    [cljs.test :refer-macros [deftest testing is run-tests async]]
    [react-test-renderer :as rt]
    [show.core :as show]))

(defn print-state [comp]
  (.log js/console (clj->js (show/get-state comp))))

(defn print-props [comp]
  (.log js/console (clj->js (show/get-props comp))))

;; assoc assoc-in dissoc merge merge-with select-keys update-in update

(defn basic-component
  ([] (basic-component nil))
  ([props]
  (-> ((component "temp" [component] (render [] "noop")) props)
    rt/create
    (.-root)
    (.-instance))))

(deftest get-props-test
  (testing "getting root props"
    (let [instance (basic-component {:root :props})]
      (is (= {:root :props} (show/get-props instance)))))
  (testing "accessing root level hash key"
    (let [instance (basic-component {:root :props})]
      (is (= :props (show/get-props instance :root)))))
  (testing "accessing root nested hash keys"
    (let [instance (basic-component {:root {:props {:foo :bar}}})]
      (is (= :bar (show/get-props instance [:root :props :foo]))))))

(deftest get-state
  (testing "getting root state"
    (let [instance (basic-component)]
      (show/reset! instance {:root :state})
      (is (= {:root :state} (show/get-state instance)))))
  (testing "accessing root level hash key"
    (let [instance (basic-component)]
      (show/reset! instance {:root :state})
      (is (= :state (show/get-state instance :root)))))
  (testing "accessing root nested hash keys"
    (let [instance (basic-component)]
      (show/reset! instance {:root {:state {:foo :bar}}})
      (is (= :bar (show/get-state instance [:root :state :foo]))))))

(deftest swap!-test
  (testing "replacing state"
    (let [instance (basic-component)]
      (show/swap! instance (fn [e] {:a 12}))
      (is (= 12 (show/get-state instance :a))))))

(deftest assoc!-test
  (testing "single key"
    (let [instance (basic-component)]
      (show/assoc! instance :a :foo)
      (is (= :foo (show/get-state instance :a)))))
  (testing "multiple keys"
    (let [instance (basic-component)]
      (show/assoc! instance :go :for :it :now!)
      (is (= :now! (show/get-state instance :it))))))

(deftest assoc-in!-test
  (testing "nested keys"
    (let [instance (basic-component)]
      (show/assoc-in! instance [:a :b :c] 100)
      (is (= 100 (get-in (show/get-state instance) [:a :b :c]))))))

(deftest dissoc-in!-test
  (testing "dissoc nested keys"
    (let [instance (basic-component)]
      (show/reset! instance {:a {:b {:c 99
                                     :d :cars}}})
      (show/dissoc-in! instance [:a :b :c])
      (is (= {:a {:b {:d :cars}}} (show/get-state instance))))))

(deftest dissoc!-test
  (testing "dissocing root keys"
    (let [instance (basic-component)]
      (show/reset! instance {:a :b :c :d})
      (show/dissoc! instance :a)
      (is (= {:c :d} (show/get-state instance))))))

(deftest update!-test
  (testing "updating single level keys"
    (let [instance (basic-component)]
      (show/reset! instance {:count 100})
      (show/update! instance :count inc)
      (is (= 101 (show/get-state instance :count))))))

(deftest update-in!-test
  (testing "updateding in nested keys"
    (let [instance (basic-component)]
      (show/reset! instance {:count {:apples 1}})
      (show/update-in! instance [:count :apples] inc)
      (is (= 2 (show/get-state instance [:count :apples]))))))

(defn main []
  (run-tests))

(set! *main-cli-fn* main)
