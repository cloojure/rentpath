(defproject clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [instaparse "1.4.8"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.3.465"]
                 [org.clojure/test.check "0.9.0"]
                 [prismatic/schema "1.1.7"]
                 [ring/ring-codec "1.1.0"]
                 [tupelo "0.9.68"]
                ]
  :profiles {:dev     {:dependencies []}
             :uberjar {:aot :all}}
  :global-vars {*warn-on-reflection* false}

  :main ^:skip-aot demo.core
  :target-path "target/%s"
  :jvm-opts ["-Xms1g" "-Xmx2g" ]
)
