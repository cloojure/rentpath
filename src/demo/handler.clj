(ns demo.handler
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [com.climate.claypoole :as cp]
    [compojure.core :as compojure]
    [compojure.handler :as handler]
    [compojure.route :as route]
    [demo.phone-number :as phone]
    [demo.views.layout :as layout]
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
    [tupelo.misc :as tm]
    [tupelo.string :as ts]
    [tupelo.schema :as tsk]))
(t/refer-tupelo)


(defn home [req]
 ;(spyx-pretty req)
  (layout/common [:h1 "Hello World!"]))

(defn query [number]
  ; (spyx :query number)
  (try
    (let [result (phone/get-phone-entry-edn number) ]
      (ruhr/ok result))
    (catch Exception ex (ruhr/not-found))))

(compojure/defroutes home-routes
  (compojure/GET "/" req (home req)) ; explicit use of request map
  (compojure/ANY "/request" [] dump/handle-dump ) ; implicit use of request map
  (compojure/GET "/query" [number :as req]
    ; (nl) (spyx-pretty req)
    (query number))
  (compojure/POST "/number" [number name context :as req]
    (let [new-entry {:phone number :context context :name name}]
      (phone/add-phone-entry new-entry)
      (ruhr/created {}))))

(compojure/defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(s/defn ^:no-doc add-headers :- {s/Str s/Str} ; #todo -> tupelo/web
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

(defn init []
  (newline)
  (println "starting")
  (println "loading EDN seed data:")
  (phone/load-seed-data-edn)
  (println "EDN seed data read...  count=" (count @phone/phone-db))
  (newline))

(defn destroy []
  (newline)
  (println "shutting down")
  (newline))

(def app
  (-> (compojure/routes home-routes app-routes)
    format/wrap-restful-format
    reload/wrap-reload
    wrap-file-info
    wrap-response-headers
    handler/site     ; #todo upgrade to ring.middleware.defaults
    wrap-base-url ))
