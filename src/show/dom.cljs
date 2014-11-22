(ns show.dom
  (:require [plug2.core :refer [pluggable?]])
  (:refer-clojure :exclude [map meta time])
  (:require-macros [show.dom :as dom]))

(defn process-opts [opts]
  opts)

(defn process-body [body]
  (if (pluggable? body) @body body))

(defn array-map? [o]
  (instance? cljs.core/PersistentArrayMap o))

(defn process-args [vs]
  (let [vs          (remove nil? vs)
        [opts body] (if (array-map? (first vs))
                      [(first vs) (rest vs)]
                      [nil        vs])
        opts        (into {} (for [[k v] opts]
                               [k (if (array-map? v) (clj->js v) v)]))]
    [(process-opts opts)
     (clojure.core/map process-body body)]))

(dom/build-tags)
