(ns tangerina.main.http.pedestal
  (:require [io.pedestal.http :as http]
            [com.walmartlabs.lacinia.pedestal :as lacinia-pedestal]
            [tangerina.main.lacinia.schema :as lacinia-schema]
            [tangerina.main.http.pedestal-routes :as routes]))

(defn lacinia-pedestal-confs [system-map]
  (let  [{::lacinia-pedestal/keys [graphiql env ide-path get-enabled]
          ::http/keys             [port]} system-map]
    {:graphiql    graphiql
     :ide-path    ide-path
     ;;:asset-path  (default: \"/assets/graphiql\")
     :get-enabled get-enabled
     :app-context system-map
     :port        port
     :env         env}))

(defn http-server
  [system]
  (assoc system ::server
         (-> (lacinia-schema/graphql-schema)
            (lacinia-pedestal/service-map (lacinia-pedestal-confs system))
            (update ::http/routes into routes/routes)
            (assoc ::http/resource-path "public"
                   ::http/file-path "target/public")
            http/create-server)))

(defn start-http! [system]
  (-> system
     http-server
     (update ::server http/start)))

(defn stop-http! [system]
  (-> system
     (update ::server http/stop)))
