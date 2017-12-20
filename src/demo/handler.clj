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

(def users #{ :fred :barney :wilma :betty :dino })
(def events->points {"PushEvent"                     5
                     "PullRequestReviewCommentEvent" 4
                     "WatchEvent"                    3
                     "CreateEvent"                   2})
(def default-event-points 1)

(def db-map (atom {}))

(defn home [req]
 ;(spyx-pretty req)
  (layout/common [:h1 "Hello World!"]))

(s/defn query :- tsk/KeyMap
  [user :- s/Str]
  (try
    (if (contains? @db-map user)
      (spyx (ruhr/ok {:user user :score (grab user @db-map)}))
      (ruhr/not-found))
    (catch Exception ex (ruhr/not-found))))

(compojure/defroutes home-routes
  (compojure/GET "/" req
    (home req))     ; explicit use of request map

  (compojure/ANY "/request" [] dump/handle-dump) ; implicit use of request map

  (compojure/ANY "/reset" []
    (reset! db-map {})
    (ruhr/ok "*** DB RESET TO INITIAL STATE ***"))

  (compojure/POST "/event" [user event-type :as req]
    (nl) (println :300)
    (spyx-pretty req)
    (let [event-points (get events->points event-type default-event-points)]
      (spyxx [user event-type event-points])
      (when-not (contains? @db-map user)
        (println "*** adding user to db: " user)
        (swap! db-map assoc user 0))
      (swap! db-map (fn swap-update-fn [db-map]
                      (update db-map user #(+ % event-points)))))
    (nl) (spyx @db-map)
    (ruhr/created {}))

  (compojure/GET "/query" [user :as req]
    (nl) (println :200) (spyx-pretty req)
    (query user)))

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

(def app
  (-> (compojure/routes home-routes app-routes)
    format/wrap-restful-format
    reload/wrap-reload
    wrap-file-info
    wrap-response-headers
    handler/site     ; #todo upgrade to ring.middleware.defaults
    wrap-base-url ))
