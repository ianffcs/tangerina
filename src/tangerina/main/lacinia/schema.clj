(ns tangerina.main.lacinia.schema
  (:require
   [com.walmartlabs.lacinia.schema :as schema]
   [com.walmartlabs.lacinia.util :as util]
   [tangerina.main.lacinia.queries :as queries]))

(def lacinia-edn
  `{:queries ~queries/queries-edn})


(defn  hello-schema
  []
  (-> lacinia-edn
     (util/attach-resolvers (merge queries/queries-resolvers-map))
     schema/compile))
