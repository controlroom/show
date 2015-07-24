(ns show.dom
  (:require [show.core :refer [class-map]])
  (:refer-clojure :exclude [map meta time])
  (:require-macros [show.dom :as dom]))

(defn preprocess-opts [opts]
  (cond-> opts
    (and (:className opts) (map? (:className opts))) (update :className class-map)))

(defn process-body [body]
  body)

(defn array-map? [o]
  (instance? cljs.core/PersistentArrayMap o)
  (map? o))

(defn process-args [vs]
  (let [vs          (remove nil? vs)
        [opts body] (if (array-map? (first vs))
                      [(first vs) (rest vs)]
                      [nil        vs])
        opts        (preprocess-opts opts)
        opts        (into {} (for [[k v] opts]
                               [k (if (array-map? v) (clj->js v) v)]))]
    [opts (clojure.core/map process-body body)]))

(dom/build-tags)
