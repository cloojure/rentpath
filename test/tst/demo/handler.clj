(ns tst.demo.handler
  (:use demo.handler tupelo.test)
  (:require
    [clojure.tools.reader.edn :as edn]
    [clojure.string :as str]
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

#_(dotest
  (let [response (unlazy-app (rmr/request :get "/"))]
    (println "main route")
    (is= (:status response) 200)
    (is (.contains (:body response) "Hello World")))

  (let [response (unlazy-app (rmr/request :get "/invalid"))]
    (println "not-found route")
    (is= (:status response) 404))

  (let [
        resp-post (unlazy-app (it-> (rmr/request :post "/number")
                                         (rmr/body it {:number 5} )))
        resp-get (unlazy-app (it-> (rmr/request :get "/query")
                               (rmr/query-string it {:number 5})))
        body     (json->edn (grab :body resp-get))]

    (is= (:status resp-post) 201)
    (is= (:status resp-get) 200)
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
  (println "http-kit:  starting server")
  (let [server-shutdown-fn (http-server/run-server (var app) {:port 9797})
        >>                 (println "pinging localhost")
        resp-1             (tm/unlazy @(http-client/get "http://localhost:9797"))]
    (spyx-pretty resp-1)
    (let [response-reset (tm/unlazy @(http-client/get "http://localhost:9797/reset"))]
      (is (re-find #"RESET" (grab :body response-reset))))
    (let [
          response-post (tm/unlazy @(http-client/request {:url         "http://localhost:9797/event"
                                                          :method      :post
                                                          :form-params {:user "fred" :event-type "foobar"}}))
          >>            (spyx-pretty response-post)

          response-get  (tm/unlazy @(http-client/request {:url          "http://localhost:9797/query"
                                                          :method       :get
                                                          :query-params {:user "fred"}}))
          >>            (spyx-pretty response-get)
          ]
      (is= (:status response-post) 201)
      (is= (:status response-get) 200)
      (is= (json->edn (grab :body response-get))
        {:user "fred" :score 1})

      (println "http-kit:  shutting down...")
      (server-shutdown-fn :timeout 1000)
      (println "http-kit:     done.")
      (flush)
      )))

