(ns tst.demo.handler
  (:use demo.handler tupelo.test)
  (:require
    [demo.core :as core]
    [org.httpkit.client :as http-client]
    [org.httpkit.server :as http-server]
    [tupelo.core :as t]
    [tupelo.misc :as tm]
    [schema.core :as s]))
(t/refer-tupelo)

;-----------------------------------------------------------------------------
; Basic unit testing

(dotest
  (core/server-start)
  ;(let [resp (tm/unlazy @(http-client/get "http://localhost:9797"))]
  ;  (spyx-pretty resp))

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
    (is= (json->edn (grab :body response-get)) {:user "fred" :score 1}))

  (let [resp (tm/unlazy @(http-client/get "http://localhost:9797/reset"))
        resp (tm/unlazy @(http-client/post "http://localhost:9797/event"
                           {:form-params {:user "fred" :event-type "PushEvent"}}))
        resp (tm/unlazy @(http-client/post "http://localhost:9797/event"
                           {:form-params {:user "fred" :event-type "WatchEvent"}}))
        resp (tm/unlazy @(http-client/get "http://localhost:9797/query"
                           {:query-params {:user "fred"}}))]
    (is= (:status resp) 200)
    (is= (json->edn (grab :body resp)) {:user "fred" :score 8}))

  (let [resp (tm/unlazy @(http-client/get "http://localhost:9797/reset"))
        resp (tm/unlazy @(http-client/post "http://localhost:9797/event"
                           {:form-params {:user "fred" :event-type "PullRequestReviewCommentEvent"}}))
        resp (tm/unlazy @(http-client/post "http://localhost:9797/event"
                           {:form-params {:user "fred" :event-type "CreateEvent"}}))
        resp (tm/unlazy @(http-client/get "http://localhost:9797/query"
                           {:query-params {:user "fred"}}))]
    (is= (:status resp) 200)
    (is= (json->edn (grab :body resp)) {:user "fred" :score 6}))

  (core/server-stop))

;-----------------------------------------------------------------------------
; Automated random events + helper functions

(def min-events-per-user 2)
(def max-events-per-user 5)

(s/defn sample-events :- s/Any
  [num-events]
  (let [events-vec  (t/append (keys events->points) "OtherEvent")
        rand-idxs   (take num-events (repeatedly #(rand-int (count events-vec))))
        rand-events (mapv #(nth events-vec %) rand-idxs)]
    rand-events))

(s/defn events-score :- s/Int
  [events :- [s/Str]]
  (let [points (for [event events]
                 (get events->points event default-event-points))
        result (apply + points)]
    result))

(dotest
  (let [events (t/append (keys events->points) "OtherEvent")
        score  (events-score events)]
    (is= 15 score)))

(dotest
  (core/server-start)
  (tm/unlazy @(http-client/get "http://localhost:9797/reset"))
  (let [user-events (apply glue
                      (for [user users]
                        (let [rand-bound     (- max-events-per-user min-events-per-user)
                              num-events     (+ min-events-per-user (rand-int rand-bound))
                              events-lst     (sample-events num-events)
                              expected-score (events-score events-lst)]
                          {user {:events-lst     events-lst
                                 :expected-score expected-score}}))) ]
    ; (spyx-pretty user-events)

    ; post random events to server
    (doseq [[user {:keys [events-lst]}] user-events]
      (doseq [event events-lst]
        (tm/unlazy @(http-client/post "http://localhost:9797/event"
                      {:form-params {:user user :event-type event}}))))

    ; query user scores from server and compare to expected
    (doseq [[user props] user-events]
      (let [resp        (tm/unlazy @(http-client/get "http://localhost:9797/query"
                                      {:query-params {:user user}}))
            user-result (json->edn (grab :body resp))
            user-score  (grab :score user-result)
            user-props  (glue props {:score user-score})
            ]
        (newline) (pretty [user user-props])
        (is= user-result {:user user :score user-score}))) ; verify expected score

    (core/server-stop)))

