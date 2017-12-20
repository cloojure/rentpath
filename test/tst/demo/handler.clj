(ns tst.demo.handler
  (:use demo.handler tupelo.test)
  (:require
    [org.httpkit.client :as http-client]
    [org.httpkit.server :as http-server]
    [tupelo.core :as t]
    [tupelo.misc :as tm]
  ))
(t/refer-tupelo)

(dotest
  (println "http-kit:  starting server")
  (let [server-shutdown-fn (http-server/run-server (var app) {:port 9797})
        resp-1             (tm/unlazy @(http-client/get "http://localhost:9797"))]

    (let [response-reset (tm/unlazy @(http-client/get "http://localhost:9797/reset"))]
      (is (re-find #"RESET" (grab :body response-reset))))

    (let [response-post (tm/unlazy @(http-client/request {:url         "http://localhost:9797/event"
                                                          :method      :post
                                                          :form-params {:user "fred" :event-type "foobar"}}))]
      (is= (:status response-post) 201))

    (let [response-get (tm/unlazy @(http-client/request {:url          "http://localhost:9797/query"
                                                         :method       :get
                                                         :query-params {:user "fred"}}))]
      (is= (:status response-get) 200)
      (is= (json->edn (grab :body response-get)) {:user "fred" :score 1}) )

    (let [resp (tm/unlazy @(http-client/get "http://localhost:9797/reset"))
          resp (tm/unlazy @(http-client/post "http://localhost:9797/event"
                             {:form-params {:user "fred" :event-type "PushEvent"}}))
          resp (tm/unlazy @(http-client/post "http://localhost:9797/event"
                             {:form-params {:user "fred" :event-type "WatchEvent"}}))
          resp (tm/unlazy @(http-client/get "http://localhost:9797/query"
                             { :query-params {:user "fred"}}))]
      (is= (:status resp) 200)
      (is= (json->edn (grab :body resp)) {:user "fred" :score 8}))

    (let [resp (tm/unlazy @(http-client/get "http://localhost:9797/reset"))
          resp (tm/unlazy @(http-client/post "http://localhost:9797/event"
                             {:form-params {:user "fred" :event-type "PullRequestReviewCommentEvent"}}))
          resp (tm/unlazy @(http-client/post "http://localhost:9797/event"
                             {:form-params {:user "fred" :event-type "CreateEvent"}}))
          resp (tm/unlazy @(http-client/get "http://localhost:9797/query"
                             { :query-params {:user "fred"}}))]
      (is= (:status resp) 200)
      (is= (json->edn (grab :body resp)) {:user "fred" :score 6}))

    (print "http-kit:  shutting down...")
    (server-shutdown-fn :timeout 1000)
    (println "   done.")))

