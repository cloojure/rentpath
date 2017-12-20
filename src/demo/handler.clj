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

(def demo-db (atom #{}))

(defn home [req]
 ;(spyx-pretty req)
  (layout/common [:h1 "Hello World!"]))

(s/defn query
  [number-str :- s/Str]
  (try
    (let [number (edn/read-string number-str)
          result (if (contains? @demo-db number)
                   {:found  number}
                   (throw (IllegalStateException. (str "Not in DB: " number))) ) ]
      (ruhr/ok result))
    (catch Exception ex (ruhr/not-found))))

(compojure/defroutes home-routes
  (compojure/GET "/" req
    (home req)) ; explicit use of request map

  (compojure/ANY "/request" [] dump/handle-dump ) ; implicit use of request map

  (compojure/GET "/query" [number :as req]
    ; (nl) (println :200) (spyx-pretty req) (nl)
    (query number))

  (compojure/POST "/number" [number :as req]
    (nl) (println :300)
    (spyxx number)
    (spyx-pretty req)
    (let [number (edn/read-string number) ]
      (spyxx number)
      (swap! demo-db glue #{number}))
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
  (update req-resp-map :headers glue headers-map))

(defn wrap-response-headers [handler] ; #todo -> tupelo/web
  (fn [req]
    (-> req
      handler
      (add-headers {"Server" "EncycloPhonica 666"
                    "Author" "HAL 9000"}))))

;(defn init []
;  (println "init - enter") )
;
;(defn destroy []
;  (newline)
;  (println "***********************")
;  (println "destroy - shutting down")
;  (println "***********************")
;  (newline))

(def app
  (-> (compojure/routes home-routes app-routes)
    format/wrap-restful-format
    reload/wrap-reload
    wrap-file-info
    wrap-response-headers
    handler/site     ; #todo upgrade to ring.middleware.defaults
    wrap-base-url ))
