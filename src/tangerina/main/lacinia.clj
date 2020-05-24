(ns tangerina.main.lacinia
  (:require [com.walmartlabs.lacinia :as lacinia]
            [tangerina.main.lacinia :as tg-lacinia]
            [clojure.string :as string]))

(defn lacinia-impl
  [{::tg-lacinia/keys [lacinias]}]
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
   :mutation/create-task (fn [_ _ _]
                           (throw (ex-info "You can't mutate here" {})))})
