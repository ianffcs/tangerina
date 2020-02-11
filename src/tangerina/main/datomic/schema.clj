(ns tangerina.main.datomic.schema
  (:require [hodur-lacinia-schema.core :as hodur.lacinia]
            [hodur-datomic-schema.core :as hodur.datomic]
            [hodur-engine.core :as hodur]
            [tangerina.main.lacinia.queries :as queries]
            [com.walmartlabs.lacinia.util :as lacinia.util]
            [com.walmartlabs.lacinia.schema :as lacinia.schema]))


(def main-schema
  '[^{:lacinia/tag true
      :datomic/tag true}
    default

    user
    [^{:type String} name
     ^{:type ID}    id
     ^{:type        todo
       :cardinality [1 n]} to-dos]

    todo
    [^{:type String} description
     ^{:type DateTime} created-at
     ^{:type DateTime} finished-at
     ^{:type Boolean} is-finished
     ^{:type user} owner]])

(def meta-db (hodur/init-schema main-schema
                                queries/query-schema
                                queries/mutation-schema))

(def datomic-schema (hodur.datomic/schema meta-db))

(def lacinia-schema (let [resolvers-map (merge queries/query-resolvers-map
                                               queries/mutation-resolvers-map)]
                      (-> (hodur.lacinia/schema meta-db)
                         (lacinia.util/attach-resolvers resolvers-map))))

(comment

  (lacinia.schema/compile lacinia-schema)

  )
