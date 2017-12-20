(ns demo.core
  (:require
    [demo.handler :as handler]
    [org.httpkit.server :as http-server]
    [tupelo.core :as t])
)
(t/refer-tupelo)

(def server-verbose-flg false)
(def server-port 9797)
(def server-shutdown-fn ::undefined)

(defn server-start []
  (when server-verbose-flg
    (print (format "Server (http-kit) starting on port %d ..." server-port)))

  ; save a reference to the shutdown function returned by `run-server`
  (alter-var-root (var server-shutdown-fn)
    (constantly
      (http-server/run-server (var handler/app) {:port 9797})))

  (when server-verbose-flg (println "    done.")))

(defn server-stop []
  (when server-verbose-flg (print "http-kit:  shutting down..."))
  (server-shutdown-fn :timeout 1000)
  (when server-verbose-flg (println "   done.")))

(defn -main []
  (server-start)

  ; for testing
  (when false
    (Thread/sleep 9000)
    (server-stop))
  )
