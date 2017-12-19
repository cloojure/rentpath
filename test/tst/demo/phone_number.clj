(ns tst.demo.phone-number
  (:use demo.phone-number tupelo.test)
  (:require
    [clojure.string :as str]
    [com.climate.claypoole :as cp]
    [demo.phone-number :as phone]
    [ring.mock.request :as rmr]
    [schema.core :as s]
    [tupelo.core :as t]
    [tupelo.misc :as tm]
    [tupelo.schema :as tsk]
    [tupelo.string :as ts]
  ))
(t/refer-tupelo)

(dotest
  ; verify phone # formats
  (is= "+1234" (re-find #"\+1234" "+1234"))
  (is= nil (re-find #"\+1234" "+1299")) ; match fail -> nil (falsey)

  (is=  (re-find #"\+?\d{3,5}" "123") "123") ; leading + is optional

  (isnt (re-find #"\+?\d{3,5}" "+12")) ; 2 digits is too short
  (is= (re-find #"\+?\d{3,5}" "+123") "+123")
  (is= (re-find #"\+?\d{3,5}" "+1234") "+1234")
  (is= (re-find #"\+?\d{3,5}" "+12345") "+12345")
  (is= (re-find #"\+?\d{3,5}" "+123456") "+12345") ; oops
  (isnt (re-find #"^\+?\d{3,5}$" "+123456")) ; need to anchor bol & eol
)

(dotest
  (is= '(["+1234" "+1" "234"]) (re-seq #"(\+1|1\-)(.*)" "+1234"))
  (is= '(["1-234" "1-" "234"]) (re-seq #"(\+1|1\-)(.*)" "1-234"))
  (is= "234" (xthird (only (re-seq #"(\+1|1\-)(.*)" "+1234"))))
  (is= "234" (xthird (only (re-seq #"(\+1|1\-)(.*)" "1-234"))))
  (is= "234" (strip-leading-1 "+1234"))
  (is= "234" (strip-leading-1 "1-234"))
  (is= "1234" (strip-leading-1 "1234"))

  (is= "1234" (strip-non-digits "(12)3+4"))
  (is= "1234" (strip-non-digits "1-2)3. 4")))

(dotest
  (is= "+13058224036" (phone-number->E164 "+13058224036"))
  (is= "+13058224036" (phone-number->E164 "+1 305.822.4036"))
  (is= "+13058224036" (phone-number->E164 "+1 305-822-4036"))
  (is= "+13058224036" (phone-number->E164 "+1 (305) 822-4036"))
  (is= "+13058224036" (phone-number->E164 "305.822.4036"))
  (is= "+13058224036" (phone-number->E164 "(305)822.4036"))
  (is= "+13058224036" (phone-number->E164 "(305) 822.4036"))
  (is= "+13058224036" (phone-number->E164 "(305)822-4036"))
  (is= "+13058224036" (phone-number->E164 "(305) 822-4036"))
  (is= "+13058224036" (phone-number->E164 "(305)8224036"))
  (is= "+13058224036" (phone-number->E164 "(305) 8224036"))
  (is= "+13058224036" (phone-number->E164 "(305)822-4036"))
  (is= "+13058224036" (phone-number->E164 "(305) 822-4036"))
  (is= "+13058224036" (phone-number->E164 "305-822-4036"))
  (is= "+13058224036" (phone-number->E164 "1-305-822-4036"))
)

(def csv-data-1
  "+13058224036,zendesk,Nowlin Saul
   (609) 491-4267,home,Rubino Lennoxlove
   +17157767000,zendesk,Deluna Mcginley
   (520) 361-1642,blah,Spearman Mccreary
   +16575044762,desk.com,Briley Hunterstone
   (216) 433-7234,facebook,Luffness Mattison
   +19898108815,work,Pitsligo Pomeroy
   (984) 990-5710,party,Caprington Lor
   +17193346351,home,Brims Kilbirnie Place
   (810) 335-6905,space,Min Pitcon
   +15098659789,desk.com,Armijo Ramsay")

(dotest
  (let [entries (set (csv-lines->entries (str/split-lines csv-data-1)))
        results #{{:context "blah", :name "Spearman Mccreary", :phone "+15203611642"}
                  {:context "desk.com", :name "Armijo Ramsay", :phone "+15098659789"}
                  {:context "desk.com", :name "Briley Hunterstone", :phone "+16575044762"}
                  {:context "facebook", :name "Luffness Mattison", :phone "+12164337234"}
                  {:context "home", :name "Brims Kilbirnie Place", :phone "+17193346351"}
                  {:context "home", :name "Rubino Lennoxlove", :phone "+16094914267"}
                  {:context "party", :name "Caprington Lor", :phone "+19849905710"}
                  {:context "space", :name "Min Pitcon", :phone "+18103356905"}
                  {:context "work", :name "Pitsligo Pomeroy", :phone "+19898108815"}
                  {:context "zendesk", :name "Deluna Mcginley", :phone "+17157767000"}
                  {:context "zendesk", :name "Nowlin Saul", :phone "+13058224036"}} ]
    (is= entries results) ))

(dotest
  (is= {} (glue-map-with-default nil))
  (is= {:a 1}      (glue-map-with-default nil {:a 1}))
  (is= {:a 1}      (glue-map-with-default     {:a 1}))
  (is= {:a 1 :b 2} (glue-map-with-default nil {:a 1} {:b 2}))
  (is= {:a 1 :b 2} (glue-map-with-default     {:a 1} {:b 2}))
  (is= {:a 1 :b 2} (glue-map-with-default {:a 9} {:b 2} {:a 1}))
  (is= {:a 1 :b 2} (glue-map-with-default {:b 2} {:a 1})))

(dotest
  (let [entries [{:phone "1234" :context "home", :name "Homer Simpson"}
                 {:phone "1234" :context "Moes", :name "Homer J"}
                 {:phone "1234" :context "Duff", :name "Homer Simpson"}
                 {:phone "1234" :context "Duff", :name "Mo"}
                 {:phone "1234" :context "Duff", :name "Bart Simpson"}
                 {:phone "1234" :context "Duff", :name "Homer Simpson"}
                 {:phone "2345" :context "home", :name "Maggie"}
                 {:phone "2345" :context "home", :name "Marge Simpson"}]]
    (phone/reset-db)
    (doseq [entry entries]
      (add-phone-entry entry))
    (is= @phone-db
      {"1234" {"Duff" "Homer Simpson",
               "home" "Homer Simpson"
               "Moes" "Homer J"},
       "2345" {"home" "Marge Simpson"}}
      )
    (is= (set (grab :results (get-phone-entry-edn "1234")))
      (set [{:number "1234", :context "Duff", :name "Homer Simpson"}
            {:number "1234", :context "Moes", :name "Homer J"}
            {:number "1234", :context "home", :name "Homer Simpson"}]))

    (add-phone-entry-json (edn->json {:number "3333" :context "school" :name "Bart"}))
    (is= @phone-db
      {"+13333" {"school" "Bart"},
       "1234"   {"Duff" "Homer Simpson",
                 "Moes" "Homer J",
                 "home" "Homer Simpson"},
       "2345"   {"home" "Marge Simpson"}} )
    (add-phone-entry-json (edn->json {:number "666" :context "school" :name "Ms. CrabApple"}))
    (is= @phone-db
      {"+13333" {"school" "Bart"},
       "+1666"  {"school" "Ms. CrabApple"},
       "1234"   {"Duff" "Homer Simpson", "Moes" "Homer J", "home" "Homer Simpson"},
       "2345"   {"home" "Marge Simpson"}})
    (add-phone-entry-json (edn->json {:number "666" :context "principal" :name "Seymore Skinner"}))
    (is= @phone-db
      {"+13333" {"school" "Bart"},
       "+1666"  {"school" "Ms. CrabApple"  "principal" "Seymore Skinner"},
       "1234"   {"Duff" "Homer Simpson", "Moes" "Homer J", "home" "Homer Simpson"},
       "2345"   {"home" "Marge Simpson"}})
    )

  )



