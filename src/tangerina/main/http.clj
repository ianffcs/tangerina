(ns tangerina.main.http
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [ring.util.mime-type :as mime]))

(defn handler
  [db]
  (fn [req]
    {:status 200
     :body   "You're not OK!"}))

;; TODO: Until I figure out how to do .forms on things
(defn stop-jetty
  [jetty]
  (.stop jetty)
  (.join jetty))
