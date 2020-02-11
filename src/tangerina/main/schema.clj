(ns tangerina.main.schema
  (:require
   [hodur-engine.core :as hodur]
   [hodur-datomic-schema.core :as hodur-datomic]
   [datascript.core :as d]
   [br.com.souenzzo.eql-as.alpha :as eql-as]
   [com.wsscode.pathom.connect :as pc]
   [com.wsscode.pathom.core :as p]
   [camel-snake-kebab.core :refer [->PascalCase
                                   ->kebab-case
                                   ->PascalCaseKeyword
                                   ->kebab-case-keyword
                                   ->camelCaseKeyword
                                   ->snake_case_keyword]]
   [medley.core :as medley]
   [clojure.test :refer [deftest is testing]]
   [clojure.set :as set]))

(def hodur-db
  (hodur/init-schema
   '[User
     [^{:type String} name
      ^{:type Email} email]
     Email
     [^{:type String} Name
      ^{:type DateTime} RegisteredAt]]))

(defn dsl->tx-data
  [& _]
  [])

(defn init-db [conn schema]
  (d/transact! conn (#'hodur/create-primitive-types []))
  (d/transact! conn (dsl->tx-data schema))
  ,,,,, ; transacionar aqui o schema
  conn)

(def ian-schema1
  '{:user/name            string
    :user/email           email
    :email/name           string
    :email/resgistered-at date-time})

;;@hodur-db
;; quero transacionar o debaixo com a minha dsl
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; [7 :node/type :type 536870913]            ;;
;; [7 :type/PascalCaseName :User 536870913]  ;;
;; [7 :type/camelCaseName :user 536870913]   ;;
;; [7 :type/kebab-case-name :user 536870913] ;;
;; [7 :type/name "User" 536870913]           ;;
;; [7 :type/nature :user 536870913]          ;;
;; [7 :type/snake_case_name :user 536870913] ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn schema->Types
  [schema]
  (set (map (comp ->PascalCase namespace) (keys schema))))

(defn schema->Fields
  [schema]
  (set (map (comp ->PascalCase name) (keys schema))))

(comment

  (let []
    (reduce (fn [a i]
              (conj a {:db/id                (get-temp-id! i)
                       :node/type            :type
                       :type/name            (str i)
                       :type/kebab-case-name (->kebab-case-keyword i)
                       :type/camelCaseName   (->camelCaseKeyword i)
                       :type/PascalCaseName  (->PascalCaseKeyword i)
                       :type/snake_case_name (->snake_case_keyword i)
                       :type/nature          :primitive}))
            accum '[String Float Integer Boolean DateTime ID]))

  #_(let [data1     '{:user/name            string
                      :user/email           email
                      :email/name           string
                      :email/resgistered-at date-time}
          #_#_data2 '{:user/name      string
                      :user/address   address
                      :address/street [street]
                      :street/name    string
                      :street/number  integer}
          Types     (schema->Types data1)
          Fields    (schema->Fields data1)
          #_#_db    (deref (hodur/init-schema '[]))
          #_#_conn  (d/conn-from-db db)]
      (->> data1
           (reduce-kv (fn [coll k v]
                        (conj coll
                              [[(namespace k) (name k)] v])) [])
           (group-by ffirst)
           #_(reduce-kv (fn [coll entity [[[_ field] type]]]
                          (conj coll field)
                          #_(conj coll [entity
                                        (with-meta field type)])) [])
           )
      #_(d/transact! conn )))


(defn fields-in-a-type [db type]
  (d/q '[:find ?e ?type ?a
         :in $ ?type
         :where
         [?e :type/name ?type]
         [?f :field/parent ?e]
         [?f :field/name ?a]]
       @db type))

;; query-db-thiago = query-db-ian
(deftest fields-in-a-type-test
  (let [conn       (d/create-conn @#'hodur/meta-schema)
        ian-db     (init-db conn {})
        data1      '{:user/name            string
                     :user/email           email
                     :email/name           string
                     :email/resgistered-at date-time}
        hodur1     '[User
                     [^{:type String} name
                      ^{:type Email} email]
                     Email
                     [^{:type String} Name
                      ^{:type DateTime} RegisteredAt]]
        db-hodur   (hodur/init-schema hodur1)
        #_#_db-dsl (schema->db data1)]
    (is (= ian-db db-hodur))
    (is (= #{[7 "User" "email"] [7 "User" "name"]}
           (fields-in-a-type db-hodur "User")))))

(comment

  (def ^:private temp-id-counter (atom 0))

  (def ^:private temp-id-map (atom {}))
  (defn ^:private reset-temp-id-state!
    []
    (reset! temp-id-counter 0)
    (reset! temp-id-map {}))

  (defn ^:private next-temp-id!
    []
    (swap! temp-id-counter dec))

  (defn ^:private set-temp-id!
    [i]
    (swap! temp-id-map assoc i (next-temp-id!)))

  (defn ^:private get-temp-id!
    ([t i r]
     (get-temp-id! (str t "-" i "-" r)))
    ([i]
     (if-let [out (get @temp-id-map i)]
       out
       (get (set-temp-id! i) i)))))
