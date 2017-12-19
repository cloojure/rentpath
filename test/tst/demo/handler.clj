(ns tst.demo.handler
  (:use demo.handler tupelo.test)
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.tools.reader.edn :as edn]
    [com.climate.claypoole :as cp]
    [demo.phone-number :as phone]
    [org.httpkit.client :as http-client]
    [org.httpkit.server :as http-server]
    [ring.util.http-response :as ruhr]
    [ring.mock.request :as rmr]
    [ring.util.codec :as ruc]
    [tupelo.core :as t]
    [tupelo.misc :as tm]
    [tupelo.string :as ts]
  ))
(t/refer-tupelo)

(def app-unlazy (comp tm/unlazy app))

(dotest
  (let [response (app-unlazy (rmr/request :get "/"))]
    (println "main route")
    (is= (:status response) 200)
    (is (.contains (:body response) "Hello World")))
  (let [response (app-unlazy (rmr/request :get "/invalid"))]
    (println "not-found route")
    (is= (:status response) 404))

  (is= (ruc/url-encode "+123-hello") "+123-hello")
  (is= (ruc/percent-encode "+123-hello") "%2B%31%32%33%2D%68%65%6C%6C%6F")
  (is= (ruc/form-encode "+123-hello") "%2B123-hello")

  (reset! phone/phone-db
    {"+13333" {"school" "Bart"},
     "+1666"  {"school" "Ms. CrabApple" "principal" "Seymore Skinner"},
     "1234"   {"Duff" "Homer Simpson", "Moes" "Homer J", "home" "Homer Simpson"},
     "2345"   {"home" "Marge Simpson"}})
  (let [response (app-unlazy (it-> (rmr/request :get "/query")
                                   (rmr/query-string it {:number "+1666"})))]
    (is= (set (grab :results (json->edn (grab :body response))))
      (set [{:number "+1666", :context "school", :name "Ms. CrabApple"}
            {:number "+1666", :context "principal", :name "Seymore Skinner"}])) )

  (let [willie-map    {:number "+1777" :name "Willie" :context "barn"}
        response-post (app-unlazy (it-> (rmr/request :post "/number")
                                    (rmr/body it willie-map)))
        response-get  (app-unlazy (it-> (rmr/request :get "/query")
                                    (rmr/query-string it {:number "+1777"})))]
    (is= (:status response-post) 201)
    (is= (:status response-get) 200)
    (is= (set (grab :results (json->edn (grab :body response-get))))
      #{willie-map}))
)

(dotest
  (is= {:a 1 :b 2} (edn/read-string "{:a 1 :b 2}"))
  (is= (ruhr/not-found {})
    {:status 404, :headers {}, :body {}} )

  (println "loading EDN seed data:")
  (phone/load-seed-data-edn)
  (println "EDN seed data read...  count=" (count @phone/phone-db))
  ;(spyx (clip-str 333 (with-out-str (println @phone-db))))
  (let [get-results (fn [response]
                      (only (grab :results (json->edn (grab :body response)))))]
    (let [response-get (app-unlazy (it-> (rmr/request :get "/query")
                                     (rmr/query-string it {:number "+12012238287"})))]
      (is= (get-results response-get)
        {:number "+12012238287", :context "space", :name "Council Airth"}))
    (let [response-get (app-unlazy (it-> (rmr/request :get "/query")
                                     (rmr/query-string it {:number "+12012434560"})))]
      (is= (get-results response-get)
        {:number "+12012434560" :context "work", :name "Grim Place"}))
    (let [response-get (app-unlazy (it-> (rmr/request :get "/query")
                                     (rmr/query-string it {:number "+12012586313"})))]
      (is= (get-results response-get)
        {:number "+12012586313" :context "enemies", :name "Comlongon Shivers"}))
    (let [response-get (app-unlazy (it-> (rmr/request :get "/query")
                                     (rmr/query-string it {:number "+12013263614"})))]
      (is= (get-results response-get)
        {:number "+12013263614" :context "facebook", :name "Yan Neith"})))

  ; non-existent phone number
  (let [response-get (app-unlazy (it-> (rmr/request :get "/query")
                                       (rmr/query-string it {:number "+18009991234"})))]
    (is= 404 (grab :status response-get)))
)

(dotest
  (nl)
  (println "starting server")
  (let [server-shutdown-fn (http-server/run-server (var app) {:port 9797})
        >>                 (println "pinging localhost")
        resp-1             @(http-client/get "http://localhost:9797")

        willie-map         {:number "+1777" :name "Willie" :context "barn"}
        response-post      (app-unlazy (it-> (rmr/request :post "/number")
                                         (rmr/body it willie-map)))
        response-get       (app-unlazy (it-> (rmr/request :get "/query")
                                         (rmr/query-string it {:number "+1777"})))]
    (is= (:status response-post) 201)
    (is= (:status response-get) 200)
    (is= (set (grab :results (json->edn (grab :body response-get))))
      #{willie-map})

    (server-shutdown-fn :timeout 1000)))

