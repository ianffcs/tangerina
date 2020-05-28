(ns tangerina.main.core
  (:require
   [clojure.java.io :as io]
   [com.walmartlabs.lacinia :as lacinia]
   [com.walmartlabs.lacinia.util :refer [attach-resolvers]]
   [com.walmartlabs.lacinia.schema :as lacinia.schema]
   [com.walmartlabs.lacinia.pedestal2 :as lp]
   [clojure.string :as string]
   #_[datascript.core :as ds]
   [io.pedestal.http :as http]
   #_[io.pedestal.http.route :as route]
   [tangerina.main.atom-db :as adb]
   [tangerina.main.datascript :as tg-ds]
   [datascript.core :as ds]))

(def ^:private idx-html (slurp (io/resource "public/index.html")))

(defn index [_]
  {:status  200
   :body    idx-html
   :headers {"Content-Type" "text/html"}})

(def front-route
  #{["/index" :get index :route-name ::index]})

(defn lacinia-wtf-impl
  [{::keys [lacinias]}]
  {:query/tasks (fn [_ _ _]
                  (for [impl lacinias
                        task (-> (lacinia/execute impl
                                                 "{ tasks { id description checked } }"
                                                 {}
                                                 {})
                                :data
                                :tasks)]
                    task))

   :query/impl           (fn [_ _ _]
                           (string/join "+" (for [impl lacinias]
                                              (:impl (:data (lacinia/execute impl
                                                                             "{ impl }"
                                                                             {}
                                                                             {}))))))
   :mutation/create-task (fn [_ _ _]
                           (throw (ex-info "You can't mutate here" {})))})

(defn create-system
  [{::keys [conn
            state
            lacinia-pedestal-conf]
    :as    env}]
  (let [lacinia-schema          {:objects   {:Task {:fields {:id          {:type 'Int}
                                                             :checked     {:type 'Boolean}
                                                             :description {:type 'String}}}}
                                 :queries   {:impl  {:type    'String
                                                     :resolve :query/impl}
                                             :tasks {:type    '(list Task)
                                                     :resolve :query/tasks}}
                                 :mutations {:createTask   {:type    'Task
                                                            :args    {:description {:type 'String}}
                                                            :resolve :mutation/create-task}
                                             :completeTask {:type    'Task
                                                            :args    {:id {:type 'Int}}
                                                            :resolve :mutation/complete-task}
                                             :updateTask   {:type    'Task
                                                            :args    {:id          {:type 'Int}
                                                                      :description {:type 'String}}
                                                            :resolve :mutation/update-task}
                                             :deleteTask   {:type    'Task
                                                            :args    {:id {:type 'Int}}
                                                            :resolve :mutation/delete-task}}}
        ds-gql-schema           (-> lacinia-schema
                                   (attach-resolvers (tg-ds/datascript-impl {::conn conn}))
                                   lacinia.schema/compile)
        #_#_atom-http-schema    (-> lacinia-schema
                                    (attach-resolvers (adb/atom-impl {::state state}))
                                    lacinia.schema/compile)
        #_#_lacinia-wtf-schema  (-> lacinia-schema
                                    (attach-resolvers (lacinia-wtf-impl {::lacinias [ds-gql-schema
                                                                                     atom-http-schema]}))
                                    lacinia.schema/compile)
        #_#_atom-http-service   (-> atom-http-schema
                                    (lp/default-service lacinia-pedestal-conf)
                                    (assoc ::http/port 8889))
        #_#_lacinia-wtf-service (-> lacinia-wtf-schema
                                    (lp/default-service lacinia-pedestal-conf)
                                    (assoc ::http/port 8890))
        ds-http-service         (-> ds-gql-schema
                                   (lp/default-service lacinia-pedestal-conf)
                                   (update ::http/routes into front-route)
                                   (assoc ::http/resource-path "public"
                                          ::http/file-path "target/public")
                                   (assoc ::http/port 8888))]
    (-> env
       (assoc ::conn conn
              ::state state
              ::http-services [::ds-http-service
                               #_::lacinia-wtf-service
                               #_::atom-http-service]
              ::ds-http-service ds-http-service
              #_#_::lacinia-wtf-service lacinia-wtf-service
              #_#_::atom-http-service atom-http-service))))

(defn start-system
  "start with (-> (create-system {}) start-system)"
  [{::keys [http-services]
    :as    env}]
  (reduce
   (fn [acc k]
     (update acc k #(-> %
                       http/create-server
                       http/start)))
   env http-services))

(defn stop-system
  [{::keys [http-services]
    :as    env}]
  (reduce
   (fn [acc k]
     (update acc k #(some-> %
                            http/stop)))
   env http-services))

(defonce sys-state (atom nil))

(defn start-system! [sys-state system]
  (reset! sys-state (start-system system)))

(defn stop-system! [sys-state]
  (stop-system @sys-state))

(defn -main []
  (->> {::conn                  (ds/create-conn tg-ds/schema)
      #_#_::state             adb/state
      ::lacinia-pedestal-conf {:api-path "/graphql"
                               :ide-path "/graphiql"}}
     create-system
     (start-system! sys-state)))
