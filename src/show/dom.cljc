(ns show.dom
  #?(:cljs (:require [react :refer [createElement Fragment]])))

;; DOM Utils
;;
(defn class-map
  "Simple helper function for quickly adding/removing classNames to components.
  It is analgous to React's classSet helper. The map argument should be a map
  containing a key for the className and an expression that evaluates truthy or
  falsey."
  [cmap]
  (clojure.string/join " " (map first (filter #(second %) cmap))))

;; DOM Element Creation
;;

(defn- class-to-className
  "Allow for using :class or :className"
  [opts]
  (if (:class opts)
    (-> opts
        (assoc :className (:class opts))
        (dissoc :class))
    opts))

(defn- implicit-class-map
  "Delegate to class-map when a hashmap is passed in for the DOM class"
  [opts]
  (if (and (:className opts) (map? (:className opts)))
    (update opts :className class-map)
    opts))

(def preprocess-opts
  "Allow for any opts rewriting"
  (comp
    implicit-class-map
    class-to-className))

(defn- array-map? [o]
  (or (instance? #?(:clj clojure.lang.PersistentArrayMap
                    :cljs cljs.core.PersistentArrayMap) o)
      (map? o)))

(def convert-to-js
  #?(:clj identity
     :cljs clj->js))

(def element-creator
  "Top level function for creation of elements"
  #?(:cljs createElement))

(defn fragment
  "Apply all body elements into a Fragment element"
  [body]
  #?(:cljs (apply element-creator Fragment nil body)))

(defn- process-body
  "Utilize React.Fragment when multiple children are passed into a dom element.
   If an array is supplied, then we pass it direct into createElement. This
   will help with reducing the amount of key warnings"
  [body]
  (let [bodyct (count body)]
    (cond
      (= bodyct 1) (first body)
      (> bodyct 1) (fragment body)
      :else body)))

(defn process-args
  "Massage element arguments into a format React.createElement will accept"
  [& vs]
  (let [vs          (remove nil? vs)
        [opts body] (if (array-map? (first vs))
                      [(first vs) (rest vs)]
                      [nil        vs])
        opts        (preprocess-opts opts)
        opts        (into {} (for [[k v] opts]
                               [k (if (array-map? v) (convert-to-js v) v)]))]
    [opts (process-body (seq body))]))

(defn element
  "Create DOM element with tag name and any options & children"
  [tag & args]
  (let [tag (if (keyword? tag) (name tag) tag)
        [opts body] (->> (apply process-args args)
                         (map convert-to-js))]
    (element-creator tag opts body)))
