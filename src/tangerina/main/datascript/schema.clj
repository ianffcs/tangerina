(ns tangerina.main.datascript.schema)

(defn schema
  []
  {:task/description {:valueType   :string
                      :cardinality :one}
   :task/completed   {:valueType   :boolean
                      :cardinality :one}})
