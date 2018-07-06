(ns show.core
  (:require [clojure.set :refer [difference union]]))

(def ^:private valid-lifecycle-fns
  '#{initial-state default-props render will-mount did-mount
     will-receive-props should-update will-update did-update
     will-unmount})

(def ^:private valid-lifecycle-props
  '#{mixins})

(defn- allow-blank-arg-list [args body]
  (if (and (not (nil? body)) (vector? args))
    [args body]
    [[] (conj body args)]))

(defn- filter-mixins
  [lifecycles]
  (let [[m l] (split-with #(= (first %) 'mixins) lifecycles)
        mixins (second (first m))]
    (if mixins
      [(if (sequential? mixins) mixins [mixins]) l]
      [nil lifecycles])))

(defn- build-lifecycle
  "Build lifecycle functions"
  [component-arg [name args & body]]
  (let [key-name    (keyword name)
        [args body] (allow-blank-arg-list args body)
        final-args  (vec (concat component-arg args))]
    (hash-map key-name (concat '(fn) [final-args] body))))

(defn- extract-forms [forms]
  (if (string? (first forms))
    [(first forms)         (second forms) (drop 2 forms)]
    [(gensym "component")  (first forms)  (rest forms)]))

(defn- assert-lifecycles [lifecycles]
  (let [misses (difference (set (map first lifecycles))
                           (union valid-lifecycle-props valid-lifecycle-fns))]
    (assert (empty? misses)
            (str "Invalid lifecycle names: ( " (clojure.string/join ", " misses) " )"))))

(defmacro component
  "Build anonoymous component class"
  [& forms]
  (let [[n component lifecycles] (extract-forms forms)
        component-arg       (if (empty? component) ['this] component)
        [mixins lifecycles] (filter-mixins lifecycles)
        lifecycle-builder   (partial build-lifecycle component-arg)
        fn-map              (into {} (map lifecycle-builder lifecycles))]
    (assert-lifecycles lifecycles)
    `(show.core/build-component ~(name n) ~fn-map ~mixins)))

(defn- extract-def-forms [forms]
  (if (string? (first forms))
    [(first forms) (rest forms)]
    [""            forms]))

(defmacro defcomponent
  "Defines a fn to create a React component"
  [name & forms]
  (let [[docstr component-form] (extract-def-forms forms)]
    `(def ~name ~docstr ~`(component ~@(conj component-form (str name))))))
