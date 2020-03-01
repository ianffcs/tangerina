(ns tangerina.main.core
  (:require [com.walmartlabs.lacinia.pedestal :refer [service-map]]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [tangerina.main.lacinia.schema :as lacinia.schema]
            [tangerina.main.rest :as rest]))

(defn system-map [env]
  (cond (= env :dev)  {:graphql/graphiql    true
                       :graphql/app-context {}
                       :graphql/ide-path    "/"
                       :graphql/get-enabled true
                       :graphql/env         :dev
                       :http/port           8888}
        (= env :prod) {:graphql/graphiql    false
                       :graphql/app-context {}
                       :graphql/env         :prod}))

(defn lacinia-pedestal-confs
  [{:graphql/keys [graphiql app-context env ide-path get-enabled]
    :http/keys    [port]}]
  {:graphiql    graphiql
   :ide-path    ide-path
   ;;:asset-path  (default: \"/assets/graphiql\")
   :get-enabled get-enabled
   :app-context app-context
   :port        port
   :env         env})

(defn http-server
  [system]
  (-> (lacinia.schema/graphql-schema)
     (service-map (lacinia-pedestal-confs system))
     (update :io.pedestal.http/routes into rest/routes)
     http/create-server))

(defonce state
  (atom nil))

(defn start-server! [system]
  (reset! state (http/start (http-server system))))

(defn stop-server! []
  (swap! state http/stop))


#_(start-server! (system-map :dev))
#_(stop-server!)

(defn -main []
  (start-server! (http-server (system-map :prod))))
