(ns tangerina.main.core
  (:require
   [clojure.java.io :as io]
   [com.walmartlabs.lacinia.util :refer [attach-resolvers]]
   [com.walmartlabs.lacinia.schema :as lacinia.schema]
   [com.walmartlabs.lacinia.pedestal2 :as lp]
   [io.pedestal.http :as http]
   [tangerina.main.datascript :as tg-ds]
   [datascript.core :as ds]))

(def ^:private idx-html (slurp (io/resource "public/index.html")))

(defn index [_]
  {:status  200
   :body    idx-html
   :headers {"Content-Type" "text/html"}})

(def front-route
  #{["/index" :get index :route-name ::index]})

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

(defn create-system
  [{::keys [conn
            state
            lacinia-pedestal-conf]
    :as    env}]
  (let [ds-http-service (-> lacinia-schema
                           (attach-resolvers (tg-ds/datascript-impl {::conn conn}))
                           lacinia.schema/compile
                           (lp/default-service lacinia-pedestal-conf)
                           (update ::http/routes into front-route)
                           (assoc ::http/resource-path "public"
                                  ::http/file-path "target/public")
                           (assoc ::http/port 8888))]
    (-> env
       (assoc ::conn conn
              ::state state
              ::http-services [::ds-http-service]
              ::ds-http-service ds-http-service))))

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
      ::lacinia-pedestal-conf {:api-path "/graphql"
                               :ide-path "/graphiql"}}
     create-system
     (start-system! sys-state)))
