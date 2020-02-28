(ns tangerina.main.lacinia.schema
  (:require
   [com.walmartlabs.lacinia.schema :as schema]
   [com.walmartlabs.lacinia.util :as util]))

(def hello-edn
  '{:queries {:hello {:type    String
                      :resolve :resolve-hello}}})

(defn resolve-hello
  [context args value]
  "Hello, Clojurians!")

(defn  hello-schema
  []
  (-> hello-edn
     (util/attach-resolvers {:resolve-hello resolve-hello})
     schema/compile))
