(ns tangerina.main.lacinia
  (:require [com.walmartlabs.lacinia :as lacinia]
            [tangerina.main.core :as core]
            [clojure.string :as string]))

(defn lacinia-impl
  [{::core/keys [lacinias] :as sys}]
  {:query/tasks (fn [a _ _]
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
   :mutation/create-task (fn [c a _]
                           (prn c)
                           (prn a)
                           (throw (ex-info "You can't mutate here" {})))})
