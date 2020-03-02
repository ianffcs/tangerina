(ns tangerina.main.core
  (:require [com.walmartlabs.lacinia.pedestal :refer [service-map]]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [tangerina.main.lacinia.schema :as lacinia.schema]
            [tangerina.main.rest :as rest]
            [datascript.core :as ds]))

(defn ds-schema
  []
  {:task/description {:valueType   :string
                      :cardinality :one}
   :task/completion  {:valueType   :boolean
                      :cardinality :one}})

(defn system-map [env]
  (cond (= env :dev)  {:graphql/graphiql    true
                       :graphql/ide-path    "/"
                       :graphql/get-enabled true
                       :graphql/env         :dev
                       :http/port           8888
                       :datascript/schema   (ds-schema)}
        (= env :prod) {:graphql/graphiql  false
                       :graphql/env       :prod
                       :http/port         3000
                       :datascript/schema (ds-schema)}))

(defn lacinia-pedestal-confs [system-map]
  (let  [{:graphql/keys [graphiql env ide-path get-enabled]
          :http/keys    [port]} system-map]
    {:graphiql    graphiql
     :ide-path    ide-path
     ;;:asset-path  (default: \"/assets/graphiql\")
     :get-enabled get-enabled
     :app-context system-map
     :port        port
     :env         env}))

(defn init-db [system]
  (-> system
     (assoc :datascript/conn
            (ds/create-conn (system :datascript/schema)))))

(defn http-server
  [system]
  (assoc system :http/server
         (-> (lacinia.schema/graphql-schema)
            (service-map (lacinia-pedestal-confs system))
            (update :io.pedestal.http/routes into rest/routes)
            http/create-server)))

(defonce state
  (atom nil))

(defn start-server! [system]
  (reset! state (-> system
                   init-db
                   http-server
                   (update :http/server http/start))))

(defn stop-server! []
  (swap! state #(assoc % :datascript/conn nil))
  (swap! state #(update % :http/server http/stop)))

#_(start-server! (system-map :dev))
#_(stop-server!)

(defn -main []
  (start-server! (system-map :prod)))
