(ns demo.phone-number
  (:require
    [clojure.string :as str]
    [clojure.tools.reader.edn :as edn]
    [com.climate.claypoole :as cp]
    [hiccup.middleware :refer [wrap-base-url]]
    [ring.middleware.format :as format]
    [schema.core :as s]
    [tupelo.core :as t]
    [tupelo.misc :as tm]
    [clojure.java.io :as io]))
(t/refer-tupelo)

(def seed-data-csv-filename "interview-callerid-data.csv")
(def seed-data-edn-filename "interview-callerid-data.edn")

(def seed-data-csv-filename "interview-callerid-data-10k.csv")
(def seed-data-edn-filename "interview-callerid-data-10k.edn")

(def PhoneEntry
  {:context s/Str
   :name    s/Str
   :phone   s/Str})
(def Context->Name-map
  "A map from context string ('home', 'work', 'zendesk', etc) to name
  ('Joe Blow', 'Rumplestilskin', 'Jack Sparrow', etc)"
  {s/Str s/Str} )
(def Phone->Context-map
  "A map from phone number in E.164 format to a Context->Name-map"
  {s/Str Context->Name-map})

(def phone-db
  "A phone number can have multiple contexts"
  (atom {}))
(defn reset-db
  "Reset the phone DB to be empty"
  []
  (reset! phone-db (sorted-map)))

(defn glue-map-with-default
  "Like tupelo.core/glue, but with a default `(sorted-map)` if the first arg is nil"
  [map-arg & args]
  (let [base-map (if (nil? map-arg)
                   (sorted-map)
                   map-arg)]
    (apply glue base-map args)))

(defn strip-leading-1
  "Turns a number like +1xxx or 1-xxx => xxx"
  [phone-str]
  (cond
    (= "+1" (strcat (take 2 phone-str))) (strcat (drop 2 phone-str))
    (= "1-" (strcat (take 2 phone-str))) (strcat (drop 2 phone-str))
    :else phone-str))

(defn strip-non-digits
  "Removes all non-digits from a string"
  [phone-str]
  (str/replace phone-str #"\D" ""))

(s/defn phone-number->E164 :- s/Str
  [raw-phone :- s/Str]
  (strcat "+1"
    (-> raw-phone
      strip-leading-1
      strip-non-digits)))

(s/defn csv-lines->entries
  [csv-lines :- [s/Str]]
  (tm/dots-config! {:decimation 1000})
  (tm/with-dots
    (let [entry-maps (vec (cp/upfor 8 [line csv-lines]
                            (let [[phone-str, context, name] (mapv str/trim (str/split line #","))]
                              (tm/dot)
                              {:context context :name name :phone (phone-number->E164 phone-str)})))]
      entry-maps)))

(s/defn add-phone-entry :- Phone->Context-map
  "Adds a phone entry map into the phone-db"
  [phone-entry :- PhoneEntry]
  (let [phone-e164      (grab :phone phone-entry)
        context-map     {(grab :context phone-entry)
                         (grab :name phone-entry)}
        fn-upsert-phone (fn fn-upsert-phone
                          [db-map-in]
                          (let [curr-context-map (get db-map-in phone-e164 (sorted-map))
                                new-context-map  (glue curr-context-map context-map)
                                db-map-out       (assoc db-map-in phone-e164 new-context-map)]
                            db-map-out))]
    (swap! phone-db fn-upsert-phone)))

(s/defn get-phone-entry-edn
  "Return a phone number info as JSON"
  [phone-str :- s/Str]
  (let [context-map (grab phone-str @phone-db)
        result-edn  {:results (forv [[context name] context-map]
                                {:number phone-str :context context :name name})}]
    result-edn))

(s/defn add-phone-entry-json
  "Adds a JSON-formatted entry to the DB"
  [data-json :- s/Str]
  (let [data-edn (json->edn data-json)
        entry    (glue (t/submap-by-keys data-edn #{:context :name})
                   {:phone (phone-number->E164 (grab :number data-edn))})]
    (add-phone-entry entry)))

(defn load-seed-data-csv []
  (let [>>                (print "reading csv data...  ")
        csv-txt           (slurp (io/resource seed-data-csv-filename))
        csv-lines         (str/split-lines csv-txt)
        >>                (println "lines read:" (count csv-lines))
        >>                (println "parsing csv data...  ")
        seed-data-entries (time (csv-lines->entries csv-lines))
        >>                (println "  csv entries:" (count seed-data-entries))

        ]
    (println "indexing csv data...")
    (reset-db)
    (cp/pdoseq 8 [entry seed-data-entries]
      (add-phone-entry entry)))
)

(defn load-seed-data-edn []
  (let [>>       (println "  reading edn data...")
        edn-data (slurp (io/resource seed-data-edn-filename))
        >>       (println "  parsing edn data...")
        seed-db  (edn/read-string edn-data)]
    (reset-db)
    (swap! phone-db glue seed-db)))

(defn -main [& args]
  (load-seed-data-csv)
  ; (spyx-pretty (into {} (take 22 @phone-db)))
  (println "saving edn data...")
  (time (spit seed-data-edn-filename
          (pr-str @phone-db)))
  (shutdown-agents) ; Needed to shutdown thread pools created by Claypoole
)
