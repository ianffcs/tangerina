(ns tangerina.client.core
  (:require [reagent.core :as r]))


(def click-count (r/atom 0))

(defn counting-component []
  [:div
   "The atom " [:code "click-count"] " has value: "
   @click-count ". "
   [:input {:type     "button" :value "Click me!"
            :on-click #(swap! click-count inc)}]])

(defn main
  []
  (prn :ok)
  (r/render
   [counting-component]
   (.getElementById js/document "app")))

(defn after-load
  []
  (main))
