(ns tangerina.main.datomic
  (:require [datomic.client.api :as d]))

(def datomic-config
  {:server-type :dev-local
   :system      "dev"
   :storage-dir "/tmp"
   :db-name     "tangerina"
   :schema      [{:db/ident       :task/description
                  :db/valueType   :db.type/string
                  :db/cardinality :db.cardinality/one
                  :db/doc         "Task Description"}
                 {:db/ident       :task/checked
                  :db/valueType   :db.type/boolean
                  :db/cardinality :db.cardinality/one
                  :db/doc         "Task Checking"}]})

(defn ensure-db! [{:keys                     [client
                                    db-name] :as datomic-config}]
  (prn "db-created: "(d/create-database client {:db-name db-name}))
  datomic-config)

(defn transact-schema! [{:keys                   [client
                                         db-name
                                         schema] :as datomic-config}]
  (let [conn (d/connect client {:db-name db-name})]
    (-> datomic-config
        (assoc :conn conn)
        (assoc :last-tx-schema (d/transact conn schema)))))

(defn start-datomic-client! [datomic-config]
  (-> datomic-config
      (assoc :client (d/client datomic-config))
      ensure-db!
      transact-schema!))
