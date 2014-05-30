(ns show.core
  (:require [clojure.set :refer [difference]]))

(def valid-lifecycle-methods
  '#{initial-state default-props will-mount did-mount
     will-receive-props should-update will-update did-update
     will-unmount})

;; To allow for a little sugar, we inject the symbol passed into defclass
;; to be mapped to this in the final lifecycle function.
(defn- build-lifecycle-fn [component-arg [name args & body]]
  (let [name       (keyword name)
        final-args (vec (concat component-arg args))]
    (hash-map name (concat '(fn) [final-args] body))))

(defn- extract-forms [forms]
  (if (string? (first forms))
    [(first forms) (second forms) (drop 2 forms)]
    [""            (first forms)  (rest forms)]))

(defn- assert-lifecycles [lifecycles]
  (let [misses (difference (set (map first lifecycles)) valid-lifecycle-methods)]
    (assert (empty? misses)
            (str "Invalid lifecycle method names: ( " (clojure.string/join ", " misses) " )"))))


(defmacro defclass
  "Defines a new React component class"
  [name & forms]
  (let [[docstr component lifecycles] (extract-forms forms)
        component-arg (if (empty? component) ['this] component)
        lifecycle-fn  (partial build-lifecycle-fn component-arg)
        fn-map        (into {} (map lifecycle-fn lifecycles))]
    (assert-lifecycles lifecycles)
    `(def ~name ~docstr (show.core/build-component ~(str name) ~fn-map))))
