(ns show.dom
  (:refer-clojure :exclude [map meta time]))

(def tags
  '[a abbr address area article aside audio b base bdi bdo big blockquote body
    br button canvas caption cite code col colgroup data datalist dd del dfn
    div dl dt em embed fieldset figcaption figure footer form h1 h2 h3 h4 h5 h6
    head header hr html i iframe img input ins kbd keygen label legend li link
    main map mark menu menuitem meta meter nav noscript object ol optgroup
    option output p param pre progress q rp rt ruby s samp script section
    select small source span strong style sub summary sup table tbody td
    textarea tfoot th thead time title tr track u ul video wbr

    ;; svg
    circle g line path polyline rect svg text defs linearGradient polygon
    radialGradient stop])

(defn tag [tag]
  `(defn ~tag [& vs#]
     (let [vs#           (remove nil? vs#)
           [opts# body#] (if (map? (first vs#)) [(first vs#) (rest vs#)]
                                                [nil         vs#])
           opts# (into {} (for [[k# v#] opts#] [k# (if (map? v#) (cljs.core.clj->js v#) v#)]))]
       (. js/React.DOM ~tag (cljs.core.clj->js opts#) (cljs.core.clj->js body#)))))

(defmacro build-tags []
  `(do ~@(clojure.core/map tag tags)))
