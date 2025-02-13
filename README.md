# Chattercook

The `home-routes` handler in the `luminusdiff.routes.home` namespace
defines the route that invokes the `home-page` function whenever an HTTP
request is made to the `/` URI using the `GET` method.

```
(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/about" {:get about-page}]])
```

The `home-page` function will in turn call the `luminusdiff.layout/render` function
to render the HTML content:

```
(defn home-page [request]
  (layout/render
    request 
    "home.html" {:docs (-> "docs/docs.md" io/resource slurp)}))
```

The `render` function will render the `home.html` template found in the `resources/html`
folder using a parameter map containing the `:docs` key. This key points to the
contents of the `resources/docs/docs.md` file containing these instructions.

The HTML templates are written using [Selmer](https://github.com/yogthos/Selmer) templating engine.

```
<div class="content">
  {{docs|markdown}}
</div>
```

<a class="level-item button" href="https://luminusweb.com/docs/html_templating.html">learn more about HTML templating »</a>



#### Organizing the routes

The routes are aggregated and wrapped with middleware in the `luminusdiff.handler` namespace:

```
(mount/defstate app-routes
  :start
  (ring/ring-handler
    (ring/router
      [(home-routes)])
    (ring/routes
      (ring/create-resource-handler
        {:path "/"})
      (wrap-content-type
        (wrap-webjars (constantly nil)))
      (ring/create-default-handler
        {:not-found
         (constantly (error-page {:status 404, :title "404 - Page not found"}))
         :method-not-allowed
         (constantly (error-page {:status 405, :title "405 - Not allowed"}))
         :not-acceptable
         (constantly (error-page {:status 406, :title "406 - Not acceptable"}))}))))
```

The `app` definition groups all the routes in the application into a single handler.
A default route group is added to handle the `404` case.

<a class="level-item button" href="https://luminusweb.com/docs/routes.html">learn more about routing »</a>

The `home-routes` are wrapped with two middleware functions. The first enables CSRF protection.
The second takes care of serializing and deserializing various encoding formats, such as JSON.

#### Managing your middleware

Request middleware functions are located under the `luminusdiff.middleware` namespace.

This namespace is reserved for any custom middleware for the application. Some default middleware is
already defined here. The middleware is assembled in the `wrap-base` function.

Middleware used for development is placed in the `luminusdiff.dev-middleware` namespace found in
the `env/dev/clj/` source path.

<a class="level-item button" href="https://luminusweb.com/docs/middleware.html">learn more about middleware »</a>

#### Database configuration is required

If you haven't already, then please follow the steps below to configure your database connection and run the necessary migrations.

* Create the database for your application.
* Update the connection URL in the `dev-config.edn` and `test-config.edn` files with your database name and login credentials.
* Run `lein run migrate` in the root of the project to create the tables.
* Let `mount` know to start the database connection by `require`-ing `luminusdiff.db.core` in some other namespace.
* Restart the application.

<a class="btn btn-primary" href="http://www.luminusweb.net/docs/database.md">learn more about database access »</a>

#### Generate migrations 
`lein run create-migration create-user-table`


### Need some help?

Visit the [official documentation](https://luminusweb.com/docs/guestbook) for examples
on how to accomplish common tasks with Luminus. The `#luminus` channel on the [Clojurians Slack](http://clojurians.net/) and [Google Group](https://groups.google.com/forum/#!forum/luminusweb) are both great places to seek help and discuss projects with other users.
