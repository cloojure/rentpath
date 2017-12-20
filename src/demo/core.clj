(ns demo.core
  (:require
    [demo.handler :as handler]
    [org.httpkit.server :as http-server]
    [tupelo.core :as t])
)
(t/refer-tupelo)

(def server-port 9797)
(def server-shutdown-fn ::undefined)

(def users #{ :fred :barney :wilma :betty :dino })
(def events->points {:PushEvent                     5
                     :PullRequestReviewCommentEvent 4
                     :WatchEvent                    3
                     :CreateEvent                   2
                     :other                         1} )

(defn -main []
  (nl)
  (println (format "Starting http-kit server on port %d server-port..." server-port))

  ; save a reference to the shutdown function returned by `run-server`
  (alter-var-root (var server-shutdown-fn)
    (constantly
      (http-server/run-server (var handler/app) {:port 9797})))
  (println "  ...server running.")

  ; for testing
  (when false
    (Thread/sleep 9000)
    (println "killing server")
    (println "http-kit:  shutting down...")
    (server-shutdown-fn :timeout 1000)
    (println "http-kit:     done.")
    (flush))
  )
