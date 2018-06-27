(ns show.core
  (:refer-clojure :exclude [reset! update! assoc! dissoc! swap!])
  (:require-macros show.core)
  (:require
    [goog.object :as gobj]
    [clojure.string]
    [react :refer [createElement createFactory]]
    [create-react-class :as create-react-class]
    [react-dom :refer [render findDOMNode]]
    ["react-dom/server" :refer [renderToString renderToStaticMarkup]])
  (:import [goog.ui IdGenerator]))

;; Render functions
;;
(defn render-to-string
  "Render a React element to its initial HTML"
  [element]
  (renderToString element))

(defn render-to-static-markup
  "Render a React element to its initial HTML, except this doesn’t create
   extra DOM attributes that React uses internally"
  [element]
  (renderToStaticMarkup element))

(defn render-to-dom
  "Bootstrap the component and inject it into the dom on the next render
  cycle"
  [component dom]
  (render component dom))

;; Alias for backwards compat
(def render-component render-to-dom)

;; Transitions
;;
;; (defn transition-group
;;   [opts body]
;;   (let [group (.. js/ReactTransitionGroup -TransitionGroup)]
;;     (createElement group (clj->js opts) (clj->js body))) )
;;
;; (defn css-transition
;;   "Create dom entrance and exit animations. Ensure that you have a unique :key
;;   property set on each component/dom-element that you pass as a body"
;;   [opts body]
;;   (let [group (.. js/ReactTransitionGroup -CSSTransition)]
;;     (createElement group (clj->js opts) (clj->js body))))

;; Getters and setters
;;
(defn get-node
  "Get the node of the current component, or if a name is passed in,
   lookup any refs that have been declared on child dom elements"
  ([component] (findDOMNode component))
  ([component name]
   (when-let [refs (.-refs component)]
     (findDOMNode (aget refs name)))))

(defn- props-with-defaults [component props]
  (merge (.. component -__show_default_props) props))

(defn- ensure-sequential [ks]
  (if (sequential? ks) ks [ks]))

(defn get-props
  "Returns the value of the components inherited nested associative structure.
  ks is an optional property that gives quick access to a get-in call"
  ([component] (props-with-defaults component (aget (.-props component) "__show")))
  ([component ks]
   (get-in (get-props component) (ensure-sequential ks))))

(defn get-state
  "Returns the value of the components owned state nested associative structure.
  ks is an optional property that gives quick access to a get-in call"
  ([component]
   (when-let [state (or (.-_pendingState component)
                        (.-state component))]
     (aget state "__show")))
  ([component ks]
   (get-in (get-state component) (ensure-sequential ks))))

(defn- get-state-atom [component]

  )

;; Local state management
;;
(defn reset!
  "Sets the new value of the components state to val without regard to the
  current value. Takes optional callback function to be called when value is
  merged into the render state"
  ([component val]
   (reset! component val nil))
  ([component val cb]
   (.setState component #js {"__show" val} cb)
   val))

(defn swap!
  "Apply f over the state of the included component. Use this if you want
   to make multiple changes to the state of your component"
  [component f]
  (reset! component (f (get-state component))))

(defn assoc!
  "Replaces values in the component's local state. Delegates to clojurescript's
  assoc"
  ([component & kvs]
   (reset! component
           (apply assoc (get-state component) kvs))))

(defn assoc-in!
  "Replaces a value in the component's local nested associative state, where ks
  is a sequence of keys and v is the new value. If any levels do not exist,
  hash-maps will be created. Delegates to clojurescript's assoc-in."
  [component ks v]
  (reset! component
          (assoc-in (get-state component) ks v)))

(defn dissoc!
  "Dissociates entries from the component's nested associative structure. Can
  accept multiple keys to remove. Delegates to clojurescript's dissoc"
  [component k & ks]
  (reset! component
          (apply dissoc (get-state component) k ks)))

(defn dissoc-in!
  "Dissociates an entry from the component's nested associative structure. ks
  can be a sequence of keys."
  [component ks]
  (let [ks (ensure-sequential ks)]
    (reset! component
            (update-in (get-state component) (butlast ks) dissoc (last ks)))))

(defn update!
  "Update a value in the component's local show structure."
  [component k f & args]
  (reset! component
          (apply update (get-state component) k f args)))

(defn update-in!
  "'Updates' a value in the component's local nested associative structure,
  where ks is a sequence of keys and f is a function that will take the old
  value and any supplied args and return the new value, and returns a new
  nested structure. If any levels do not exist, hash-maps will be created."
  [component ks f & args]
  (reset! component
          (update-in (get-state component) (ensure-sequential ks) #(apply f % args))))

(defn force-update!
   "Forces an update. This should only be invoked when it is known with
   certainty that we are **not** in a DOM transaction.

   You may want to call this when you know that some deeper aspect of the
   component's state has changed but any state change functions were not
   called.

   This will not invoke `should-update`, but it will invoke `will-update` and
   `did-update`."

  ([component]
   (.forceUpdate component))
  ([component cb]
   (.forceUpdate component cb)))

;; Lifecycle helpers
;;
(defn- local-method
  "Extract declared component function for lifecycle method"
  [component name]
  (when-let [props (aget component "__show_base")]
    (name (aget props "lifecycle_methods"))))

(defn- local-mixins
  "Extract declared mixin functions for lifecycle method"
  [component name]
  (when-let [props (aget component "__show_base")]
    (map name (filter name (aget props "mixins")))))

(defn- execute-mixin-methods
  "Apply state & props to all declared mixin methods for lifecycle"
  [component name & props]
  (let [mixins (local-mixins component name)]
    (mapv #(apply % component props) mixins)))

(defn- execute-local-method
  "Apply state & props to supplied function for lifecycle"
  [component name & props]
  (if-let [local (local-method component name)]
    (apply local component props)))

(defn- get-show-data [props]
  (aget props "__show"))

(def ^:private core-lifecycle-methods
  "These are the core show lifecycle methods. They are never overridden.
   If you specify a version of each method in your component declaration,
   then it will delegate to that method from within the function call in core"
  {:render
   (fn [] (this-as this
     (let [props (get-props this)
           state (get-state this)]
       (execute-local-method this :render props state))))

   :getInitialState
   (fn [] (this-as this
     (let [props        (get-props this)
           state        (execute-local-method  this :initial-state props)
           mixin-states (execute-mixin-methods this :initial-state props)]
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

(def ^:private static-methods
  "WIP: No context can be inferred in function call, so we will need another
   way to get function delegation"
  {:getDerivedStateFromProps
   (fn [component]
     (fn [props state]
       (.log js/console component)
       (println ":getDerivedStateFromProps called")))})

(defn inject-static-methods [obj]
  (doseq [[k f] static-methods]
    (gobj/set obj (name k) (f obj)))
  obj)

(defn ^:private build-component
  "Component builder. Attaches core lifecycle methods and sets up data to allow
   for user declared lifecycle methods. Returns a function that creates a component
   based on the supplied lifecycle method hashmap"
  [name lifecycle mixins]
  (let [mixins            (map #(.. % -lifecycle_methods) mixins)
        lifecycle-methods (assoc core-lifecycle-methods
                                 :displayName name
                                 :__show_base #js {:lifecycle_methods lifecycle
                                                   :mixins mixins})

        component-class   (-> (clj->js lifecycle-methods)
                              create-react-class
                              ;; inject-static-methods
                              createFactory)

        creator           #(component-class #js {:key    (or (get % :key) js/undefined)
                                                 :__show (dissoc % :key)})]

    ;; Inject lifecycle methods into the fn just for mixin loading
    (set! (.-lifecycle_methods creator) lifecycle)
    creator))
