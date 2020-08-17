(ns tangerina.main.core
  (:require
   [clojure.java.io :as io]
   [com.walmartlabs.lacinia.util :as lacinia-util]
   [com.walmartlabs.lacinia.schema :as lacinia.schema]
   [com.walmartlabs.lacinia.pedestal2 :as lp]
   [io.pedestal.http :as http]
   [tangerina.main.datascript :as tg-ds]
   [datascript.core :as ds]))

(def lacinia-schema
  {:objects   {:Task {:fields {:id          {:type 'Int}
                               :checked     {:type 'Boolean}
                               :description {:type 'String}}}}
   :queries   {:impl  {:type    'String
                       :resolve :query/impl}
               :tasks {:type    '(list Task)
                       :resolve :query/tasks}}
   :mutations {:createTask         {:type    'Task
                                    :args    {:description {:type 'String}}
                                    :resolve :mutation/create-task}
               :checkTask          {:type    'Task
                                    :args    {:id {:type 'Int}}
                                    :resolve :mutation/check-task}
               :setDescriptionTask {:type    'Task
                                    :args    {:id          {:type 'Int}
                                              :description {:type 'String}}
                                    :resolve :mutation/set-description-task}
               :deleteTask         {:type    'Task
                                    :args    {:id {:type 'Int}}
                                    :resolve :mutation/delete-task}}})

(def ^:private idx-html (slurp (io/resource "public/index.html")))

(defn index [_]
  {:status  200
   :body    idx-html
   :headers {"Content-Type" "text/html"}})

(defn routes [{:keys [app-context
                      api-path
                      ide-path
                      asset-path]
               :as   options} compiled-schema]
  (into #{[api-path :post (lp/default-interceptors compiled-schema app-context)
           :route-name ::graphql-api]
          [ide-path :get (lp/graphiql-ide-handler options)
           :route-name ::graphiql-ide]
          ["/index" :get index :route-name ::index]}
        (lp/graphiql-asset-routes asset-path)))

(defn create-system
  [{::keys [conn
            state]
    :as    config-map}]
  (let [ds-http-compiled (-> lacinia-schema
                             (lacinia-util/attach-resolvers
                              (tg-ds/datascript-impl {::conn conn}))
                             lacinia.schema/compile)]
    (-> config-map
        (assoc ::conn conn
               ::state state
               ::http/routes (routes config-map ds-http-compiled)
               ;; "Disables secure headers in the service map,
               ;;  a prerequisite for GraphiQL requests to operate."
               ::http/secure-headers nil)
        ;; TODO enable subscriptions
        #_(lp/enable-subscriptions ds-http-compiled config-map)
        http/create-server)))

(defonce sys-state (atom nil))

(defn start-system! [system-map]
  (reset! sys-state (http/start system-map)))

(defn stop-system! []
  (swap! sys-state http/stop))

(def config-map
  {:api-path            "/graphql"
   :ide-path            "/graphiql"
   :asset-path          "/assets/graphiql"
   :subscriptions-path  "/ws"
   :env                 :dev
   :app-context         {}
   ::conn               (ds/create-conn tg-ds/schema)
   ::http/resource-path "public"
   ::http/file-path     "target/public"
   ::http/port          8888
   ::http/type          :jetty
   ::http/join?         false})

(defn -main []
  (-> config-map
      create-system
      start-system!))
