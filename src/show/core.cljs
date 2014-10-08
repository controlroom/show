(ns show.core
  (:refer-clojure :exclude [reset! update! assoc! dissoc!])
  (:require [clojure.string])
  (:import [goog.ui IdGenerator]))

(enable-console-print!)

;; Utils
(defn class-map
  "Simple helper function for quickly adding/removing classNames to components.
  It is analgous to React's classSet helper. The map argument should be a map
  containing a key for the className and an expression that evaluates truthy or
  falsey."
  [cmap]
  (clojure.string/join " " (map first (filter #(second %) cmap))))

(defn css-transition-group
  "Create dom entrance and exit animations. Ensure that you have a unique :key
  property set on each component/dom-element that you pass as a body"
  [transition-name body]
  (let [group (.. js/React -addons -CSSTransitionGroup)]
    (group #js {:transitionName transition-name} (clj->js body))))

;; Getters and setters
(defn get-node
  ([component] (.getDOMNode component))
  ([component name]
   (when-let [refs (.-refs component)]
     (.getDOMNode (aget refs name)))))

(defn- props-with-defaults [component props]
  (merge (.. component -__show_default_props) props))

(defn get-props
  "Returns the value of the components inherited nested associative structure.
  ks is an optional property that gives quick access to a get-in call"
  ([component] (props-with-defaults component (aget (.-props component) "__show")))
  ([component ks]
   (let [ks (if (sequential? ks) ks [ks])]
     (get-in (get-props component) ks))))

(defn get-state
  "Returns the value of the components owned state nested associative structure.
  ks is an optional property that gives quick access to a get-in call"
  ([component]
  (if-let [state (or (.-_pendingState component)
                     (.-state component))]
    (aget state "__show")))
  ([component ks]
   (let [ks (if (sequential? ks) ks [ks])]
     (get-in (get-state component) ks))))

(defn reset!
  "Sets the new value of the components state to val without regard to the
  current value. Takes optional callback function to be called when value is
  merged into the render state"
  ([component val]
   (reset! component val nil))
  ([component val cb]
   (.replaceState component #js {"__show" val} cb)
   val))

(defn assoc-in!
  "Replaces a value in the component's local nested associative state, where ks
  is a sequence of keys and v is the new value. If any levels do not exist,
  hash-maps will be created. Delegates to clojurescript's assoc-in."
  [component ks v]
  (reset! component
          (assoc-in (get-state component) ks v)))

(defn assoc!
  "Replaces values in the component's local state. Delegates to clojurescript's
  assoc"
  ([component & kvs]
   (reset! component
           (apply assoc (get-state component) kvs))))

(defn dissoc-in!
  "Dissociates an entry from the component's nested associative structure. ks
  can be a sequence of keys."
  [component ks]
  (let [ks (if (sequential? ks) ks [ks])]
    (reset! component
            (update-in (get-state component) (butlast ks) dissoc (last ks)))))

(defn dissoc!
  "Dissociates entries from the component's nested associative structure. Can
  accept multiple keys to remove. Delegates to clojurescript's dissoc"
  [component k & ks]
  (reset! component
          (apply dissoc (get-state component) k ks)))

(defn update-in!
  "'Updates' a value in the component's local nested associative structure,
  where ks is a sequence of keys and f is a function that will take the old
  value and any supplied args and return the new value, and returns a new
  nested structure. If any levels do not exist, hash-maps will be created."
  [component ks f & args]
  (let [ks (if (sequential? ks) ks [ks])]
    (reset! component
            (update-in (get-state component) ks #(apply f % args)))))

(defn force-update!
   "Forces an update. This should only be invoked when it is known with
   certainty that we are **not** in a DOM transaction.

   You may want to call this when you know that some deeper aspect of the
   component's state has changed but any state change functions were not
   called.

   This will not invoke `should-update`, but it will invoke `will-update` and
   `did-update`."

  [component]
  (.forceUpdate component))

(defn render-component
  "Bootstrap the component and inject it into the dom on the next render
  cycle"
  [component dom]
  (js/React.renderComponent component dom))

(defn- local-method [component name]
  (when-let [props (.. component -__show_base)]
    (name (.-lifecycle_methods props))))

(defn- local-mixins [component name]
  (when-let [props (.. component -__show_base)]
    (map name (filter name (.-mixins props)))))

(defn- execute-mixin-methods [component name & props]
  (let [mixins (local-mixins component name)]
    (mapv #(apply % component props) mixins)))

(defn- execute-local-method [component name & props]
  (if-let [local (local-method component name)]
    (apply local component props)))

(defn get-show-data [props]
  (aget props "__show"))

(def base-lifecycle-methods
  {:render
   (fn [] (this-as this
     (let [props (get-props this)
           state (get-state this)]
       (execute-local-method this :render props state))))

   :getInitialState
   (fn [] (this-as this
     (let [state        (execute-local-method  this :initial-state)
           mixin-states (execute-mixin-methods this :initial-state)]
       #js {"__show" (merge (into {} (apply merge mixin-states)) state)})))

   :getDefaultProps
   (fn [] (this-as this
     (let [this-proto    (.-prototype this) ; Must use prototype since component is not loaded yet
           props         (execute-local-method this-proto :default-props)
           mixin-props   (into {} (apply merge (execute-mixin-methods this-proto :default-props)))
           default-props (merge mixin-props props)]
       ;; React will only merge prop data on the root level, and since there is
       ;; already a __show property, it does not merge new props. This is why
       ;; we dont rely on React for this merge
       (set! (.. this-proto -__show_default_props) default-props))
     nil))

   :componentWillMount
   (fn [] (this-as this
     (execute-mixin-methods this :will-mount)
     (execute-local-method  this :will-mount)))

   :componentDidMount
   (fn [] (this-as this
     (execute-mixin-methods this :did-mount)
     (execute-local-method  this :did-mount)))

   :componentWillReceiveProps
   (fn [next-props] (this-as this
     (let [next-props (props-with-defaults this (get-show-data next-props))]
       (execute-mixin-methods this :will-receive-props next-props)
       (execute-local-method  this :will-receive-props next-props))))

   :shouldComponentUpdate
   (fn [next-props next-state] (this-as this
     (let [next-props (props-with-defaults this (get-show-data next-props))
           next-state (get-show-data next-state)
           ;; Need to explicitly check for the method since a nil return is
           ;; acceptable from should-update
           local-update?       (local-method this :should-update)
           local-should-update (execute-local-method this :should-update next-props next-state)
           mixin-update?       (not (empty? (local-mixins this :should-update)))
           mixin-should-update (every? identity (execute-mixin-methods this :should-update next-props next-state))]
       (cond
         (and local-update? mixin-update?)
           (and local-should-update mixin-should-update)
         mixin-update?
           mixin-should-update
         local-update?
           local-should-update
         :else
           (or (not= (get-props this) next-props)
               (not= (get-state this) next-state))))))

   :componentWillUpdate
   (fn [next-props next-state] (this-as this
     (let [next-props (props-with-defaults this (get-show-data next-props))
           next-state (get-show-data next-state)]
       (execute-mixin-methods this :will-update next-props next-state)
       (execute-local-method  this :will-update next-props next-state))))

   :componentDidUpdate
   (fn [prev-props prev-state] (this-as this
     (let [prev-state (get-show-data prev-state)
           prev-props (props-with-defaults this (get-show-data prev-props))]
       (execute-mixin-methods this :did-update prev-props prev-state)
       (execute-local-method  this :did-update prev-props prev-state))))

   :componentWillUnmount
   (fn [] (this-as this
     (execute-mixin-methods this :will-unmount)
     (execute-local-method  this :will-unmount)))})

(defn ^:private build-component [name lifecycle mixins]
  (let [mixins            (map #(.. % -lifecycle_methods) mixins)
        lifecycle-methods (assoc base-lifecycle-methods
                                 :displayName name
                                 :__show_base #js {:lifecycle_methods lifecycle
                                                   :mixins mixins})

        component-class   (js/React.createClass (clj->js lifecycle-methods))
        ret-fn            #(component-class #js {:key    (get % :key)
                                                 :__show (dissoc % :key)})]

    ;; Inject lifecycle methods into the fn just for mixin loading
    (set! (.-lifecycle_methods ret-fn) lifecycle)
    ret-fn))
