(ns tangerina.main.rest
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [ring.util.mime-type :as mime]))

(defn respond-hello [request]
  {:status 200 :body "Hello, world!"})

(def routes
  #{["/greet" :get respond-hello :route-name :greet]})
