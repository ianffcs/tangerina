(ns tangerina.client.core
  (:require [reagent.core :as r]))

(defn main
  []
  (prn :ok)
  (r/render
   [:div "Olaaaaá mundo!"]
   (.getElementById js/document "app")))

(defn after-load
  []
  (main))
