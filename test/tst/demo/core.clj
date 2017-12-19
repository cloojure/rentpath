(ns tst.demo.core
  (:use demo.core tupelo.test)
  (:require
    [tupelo.core :as t]
  ))
(t/refer-tupelo)

(dotest
  (println "tst.demo.core" )

)
