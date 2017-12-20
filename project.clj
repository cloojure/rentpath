(defproject demo "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [com.climate/claypoole "1.1.4"]
                 [compojure "1.6.0"]
                 [hiccup "2.0.0-alpha1"]
                ;[hiccup "1.0.5"]
                 [http-kit "2.2.0"]
                 [instaparse "1.4.8"]
                 [metosin/ring-http-response "0.9.0"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.3.465"]
                 [org.clojure/test.check "0.9.0"]
                 [org.clojure/tools.reader "1.1.1"]
                 [prismatic/schema "1.1.7"]
                 [ring "1.6.3"]
                ;[ring-server "0.5.0"]
                 [ring-middleware-format "0.7.2"]
                 [ring/ring-codec "1.1.0"]
                 [ring/ring-defaults "0.3.1"]
                 [ring/ring-mock "0.3.2"]
                 [tupelo "0.9.68"]
                ]

  :plugins [[lein-ring "0.12.1"]]
  :ring {:handler demo.handler/app
       ; :init    demo.handler/init
       ; :destroy demo.handler/destroy
        }
  :profiles {:uberjar {:aot :all}
             :production
                      {:ring
                       {:open-browser? false, :stacktraces? false, :auto-reload? false}}
             :dev     {:dependencies [[ring-mock "0.1.5"]
                                      [ring/ring-devel "1.6.3"]]}}
  :global-vars {*warn-on-reflection* false}

  :main ^:skip-aot demo.core
  :target-path "target/%s"
  :jvm-opts ["-Xms1g" "-Xmx2g"]
  )
