(ns tangerina.main.rest
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [clojure.java.io :as io]
            [ring.util.mime-type :as mime]))

(defn respond-hello [request]
  {:status 200 :body "Hello, world!"})

(def ^:private idx-html (slurp (io/resource "public/index.html")))

(defn index [_]
  {:status  200
   :body    idx-html
   :headers {"Content-Type" "text/html"}})


(def routes
  #{["/greet" :get respond-hello :route-name :greet]
    ["/index" :get index :route-name :index]})
