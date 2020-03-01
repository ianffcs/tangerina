(ns tangerina.main.lacinia.schema
  (:require
   [com.walmartlabs.lacinia.schema :as schema]
   [com.walmartlabs.lacinia.util :as util]
   [tangerina.main.lacinia.queries :as queries]))

(def lacinia-edn
  `{:objects {:Task {:fields {:id          {:type ~'ID}
                              :description {:type ~'String}
                              :completed   {:type ~'Boolean}}}}
    :queries ~queries/queries-edn})


(defn graphql-schema
  []
  (-> lacinia-edn
     (util/attach-resolvers (merge queries/queries-resolvers-map))
     schema/compile))
