(ns tst.demo.handler
  (:use demo.handler tupelo.test)
  (:require
    [clojure.tools.reader.edn :as edn]
    [org.httpkit.client :as http-client]
    [org.httpkit.server :as http-server]
    [ring.util.http-response :as ruhr]
    [ring.mock.request :as rmr]
    [ring.util.codec :as ruc]
    [tupelo.core :as t]
    [tupelo.misc :as tm]
  ))
(t/refer-tupelo)

(def unlazy-app (comp tm/unlazy app))

(dotest
  (is= (ruc/url-encode "+123-hello") "+123-hello")
  (is= (ruc/percent-encode "+123-hello") "%2B%31%32%33%2D%68%65%6C%6C%6F")
  (is= (ruc/form-encode "+123-hello") "%2B123-hello")

  (is= {:a 1 :b 2} (edn/read-string "{:a 1 :b 2}")) )

(dotest
  (let [response (unlazy-app (rmr/request :get "/"))]
    (println "main route")
    (is= (:status response) 200)
    (is (.contains (:body response) "Hello World")))

  (let [response (unlazy-app (rmr/request :get "/invalid"))]
    (println "not-found route")
    (is= (:status response) 404))

  (let-spy-pretty [
        resp-post (unlazy-app (it-> (rmr/request :post "/number")
                                         (rmr/body it {:number 5} )))
        resp-get (unlazy-app (it-> (rmr/request :get "/query")
                               (rmr/query-string it {:number 5})))
        body     (json->edn (grab :body resp-get))]
    (is= body {:found 5}))

  (is= (ruhr/not-found {})
    {:status 404, :headers {}, :body {}} )

  ; non-existent phone number
  (let [response-get (unlazy-app (it-> (rmr/request :get "/query")
                                       (rmr/query-string it {:number "666"})))]
    (is= 404 (grab :status response-get)))
)

(dotest
  (nl)
  (println "starting server")
  (let-spy-pretty [
        server-shutdown-fn (http-server/run-server (var app) {:port 9797})
        >>                 (println "pinging localhost")
        resp-1             @(http-client/get "http://localhost:9797")
                   ; >> (spyx-pretty resp-1)

        response-post      (unlazy-app (it-> (rmr/request :post "/number")
                                         (rmr/body it {:number 7} )))
        response-get       (unlazy-app (it-> (rmr/request :get "/query")
                                         (rmr/query-string it {:number 7})))]
    (is= (:status response-post) 201)
    (is= (:status response-get) 200)
    (is= (json->edn (grab :body response-get))
      {:found 7})

    (server-shutdown-fn :timeout 1000)))

