(ns demo.handler
  (:require
    [clojure.tools.reader.edn :as edn]
    [compojure.core :as compojure]
    [compojure.handler :as handler]
    [compojure.route :as route]
    [demo.layout :as layout]
    [ring.util.http-response :as ruhr]
    [hiccup.middleware :refer [wrap-base-url]]
    [ring.handler.dump :as dump]
    [ring.middleware.file-info :refer [wrap-file-info]]
    [ring.middleware.resource :refer [wrap-resource]]
    [ring.middleware.reload :as reload]
    [ring.mock.request :as rmr]
    [ring.middleware.defaults :as rmd]
    [ring.middleware.format :as format]
    [schema.core :as s]
    [tupelo.core :as t]
    [tupelo.schema :as tsk]))
(t/refer-tupelo)


(defn home [req]
 ;(spyx-pretty req)
  (layout/common [:h1 "Hello World!"]))

(s/defn query
  [number-str :- s/Str]
  (spy :210 number-str)
  (try
    (let-spy-pretty [
          number (edn/read-string number-str)
          >> (spy :211 number)
          result (if (<= 0 number 9)
                   {:number  number}
                   (throw (IllegalStateException. (str "Illegal: " number))) ) ]
      (ruhr/ok result))
    (catch Exception ex (ruhr/not-found))))

(compojure/defroutes home-routes
  (compojure/GET "/" req
    (nl) (println :010)
    (spyx-pretty req)
    (spyx-pretty (home req))) ; explicit use of request map

  (compojure/ANY "/request" [] dump/handle-dump ) ; implicit use of request map

  (compojure/GET "/query" [number :as req]
    (nl) (println :200)
    (spyx-pretty req)
    (nl)
    (spyx-pretty (query number)))

  (compojure/POST "/number" [number :as req]
    (nl) (println :300)
    (spyx-pretty req)
    (nl)
    (spyx-pretty (ruhr/created {}))))

(compojure/defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(s/defn ^:no-doc add-headers :- tsk/KeyMap ; #todo -> tupelo/web
  "Merges the supplied map string key/value pairs to the request/response
  `:headers`"
  [req-resp-map  :- tsk/KeyMap
   headers-map :- {s/Str s/Str}]
  (nl) (println "add-headers:")
  (spyx-pretty req-resp-map)
  (let [result (update req-resp-map :headers glue headers-map)]
    (spyx-pretty result)))

(defn wrap-response-headers [handler] ; #todo -> tupelo/web
  (fn [req]
    (nl) (println "wrap-response-headers:")
    (-> req
      handler
      (add-headers {"Server" "EncycloPhonica 666"
                    "Author" "HAL 9000"}))))

(defn init []
  (println "init - enter") )

(defn destroy []
  (println "destroy - shutting down")
  (newline))

(def app
  (-> (compojure/routes home-routes app-routes)
    format/wrap-restful-format
    reload/wrap-reload
    wrap-file-info
    wrap-response-headers
    handler/site     ; #todo upgrade to ring.middleware.defaults
    wrap-base-url ))
