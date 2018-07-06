**Show** is a minimal ClojureScript wrapper around React.js. Show operates under
the idea that less is more.

This is still a proof of concept and is undergoing breaking changes

## Simple usage

```clojure
(ns simple
  (:require [show.core :as show]
            [show.dom :as dom]))

(show/defclass App [component]
  (render [props state]
    (dom/h1 (:heading props)))

(show/render-component
  (App {:heading "App"})
  (.getElementById js/document "app"))
```

## License

Copyright © 2018 controlroom.io

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
