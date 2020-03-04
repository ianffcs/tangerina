(ns tangerina.client.request-graphql
  (:require [tangerina.client.http-driver :as http]
            [clojure.core.async :as async]
            #?(:clj [jsonista.core :as j])
            [com.wsscode.pathom.connect.graphql2 :as pcgql]))

(defn decodejson
  [json]
  #?(:clj (if-not (or (empty? json) (map? json))
            (j/read-value json (j/object-mapper {:decode-key-fn true}))
            json)))

(def graphql-app
  {:url           "http://localhost:8888/graphql"
   :method        :post
   ;;:content-type  :graphql
   #_#_:insecure? true})

(defn parserHTTPResponse
  [req res]
  #?(:clj  (-> res
               :body
               decodejson
               :data
               (get (get req :query-name)))
     :cljs (-> res
               .-data
               (aget (name (get req :query-name)))
               (js->clj :keywordize-keys true))))

(def defineTaskResp
  [:id
   :description
   :completed])

(defn query<->mutation?
  [query-name]
  (if (= \: (first query-name))
    (keyword (subs query-name 1))
    (symbol query-name)))

(comment
  ((query<->mutation? ":a") => :a)
  ((query<->mutation? "a") => 'a))

(defn gqlHTTPBuilder
  [app query-name response mapvals]
  (let [query (query<->mutation? query-name)]
    (-> (->> (pcgql/query->graphql `[{(~query ~mapvals)
                                      ~response}]
                                   {})
             (assoc {} "query")
             (assoc app :query-params))
        (assoc :query-name (keyword query)))))

(defn defineTask!
  [{:keys [http-driver]} req-map]
  (let [query "defineTask"
        req   (gqlHTTPBuilder graphql-app
                              query defineTaskResp req-map)]
    (async/go
      (->> req
           http-driver
           async/<!
           (parserHTTPResponse req)))))

(defn listTasks!
  [{:keys [http-driver]} req-map]
  (let [query ":listTasks"
        req   (gqlHTTPBuilder graphql-app
                              query defineTaskResp req-map)]
    (async/go
      (->> req
           http-driver
           async/<!
           (parserHTTPResponse req)))))

(comment defineTask! funciona igual no CLJ e no CLJS
         (async/go
           (prn (async/<! (defineTask!
                            {:http-driver http/request-async}
                            {:description "oi"}))))

         (async/go
           (prn (async/<! (listTasks!
                           {:http-driver http/request-async}
                           {}))))
         )
