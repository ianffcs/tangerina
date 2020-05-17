(ns tangerina.main.datascript.schema)

(defn schema
  []
  {:task/description {:valueType   :string
                      :cardinality :one}
   :task/completion  {:valueType   :boolean
                      :cardinality :one}})
