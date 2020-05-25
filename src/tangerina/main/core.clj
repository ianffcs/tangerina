(ns tangerina.main.core
  (:require
   [com.walmartlabs.lacinia :as lacinia]
   [com.walmartlabs.lacinia.util :refer [attach-resolvers]]
   [com.walmartlabs.lacinia.schema :as lacinia.schema]
   [com.walmartlabs.lacinia.pedestal2 :as lp]
   [clojure.string :as string]
   #_[datascript.core :as ds]
   [io.pedestal.http :as http]
   [tangerina.main.atom-db :as adb]
   [tangerina.main.datascript :as tg-ds]
   [datascript.core :as ds]))

(defn lacinia-impl
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
            state]
    :as    env}]
  (let [lacinia-schema      {:objects   {:Task {:fields {:id          {:type 'Int}
                                                         :checked     {:type 'Boolean}
                                                         :description {:type 'String}}}}
                             :queries   {:impl  {:type    'String
                                                 :resolve :query/impl}
                                         :tasks {:type    '(list Task)
                                                 :resolve :query/tasks}}
                             :mutations {:create_task {:type    'Task
                                                       :args    {:description {:type 'String}}
                                                       :resolve :mutation/create-task}}}
        ds-gql-schema       (-> lacinia-schema
                               (attach-resolvers (tg-ds/datascript-impl {::conn conn}))
                               lacinia.schema/compile)
        atom-http-schema    (-> lacinia-schema
                               (attach-resolvers (adb/atom-impl {::state state}))
                               lacinia.schema/compile)
        lacinia-wtf-schema  (-> lacinia-schema
                               (attach-resolvers (lacinia-impl {::lacinias [ds-gql-schema
                                                                            atom-http-schema]}))
                               lacinia.schema/compile)
        atom-http-service   (-> atom-http-schema
                               (lp/default-service {})
                               (assoc ::http/port 8888))
        lacinia-wtf-service (-> lacinia-wtf-schema
                               (lp/default-service {})
                               (assoc ::http/port 8890))
        ds-http-service     (-> ds-gql-schema
                               (lp/default-service {})
                               (assoc ::http/port 8889))]
    (assoc env ::conn conn
           ::state state
           ::http-services [::ds-http-service
                            ::lacinia-wtf-service
                            ::atom-http-service]
           ::ds-http-service ds-http-service
           ::lacinia-wtf-service lacinia-wtf-service
           ::atom-http-service atom-http-service)))

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

(defn -main []
  (-> {::conn  (ds/create-conn tg-ds/schema)
      ::state adb/state}
     create-system
     start-system))
