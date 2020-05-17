(ns tangerina.main.lacinia.schema
  (:require
   [com.walmartlabs.lacinia.schema :as schema]
   [com.walmartlabs.lacinia.util :as util]
   [tangerina.main.lacinia.queries :as queries]
   [tangerina.main.lacinia.mutations :as mutations]
   [com.walmartlabs.lacinia :as lacinia]))

(def lacinia-edn
  `{:objects     {:Task {:fields {:id          {:type ~'ID}
                                  :description {:type ~'String}
                                  :completed   {:type ~'Boolean}
                                  :delete      {:type ~'Boolean}}}}
    #_#_:queries ~queries/queries-edn
    #_#_         :mutations ~mutations/mutations-edn})


(defn graphql-schema
  []
  (-> lacinia-edn
     schema/compile))

#_(defn datomic-schema)
