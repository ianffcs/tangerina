(ns tangerina.client.http-driver
  (:require #?@(:clj  [[clj-http.client :as c]
                       [cheshire.core :as j]]
                :cljs [["url" :as url]])
            [clojure.core.async :as async]
            [clojure.string :as string]))

(defn request-async
  [req]
  (let [chan    (async/promise-chan)
        raise   (fn request-async-raise [ex]
                  (async/put! chan (if (nil? ex)
                                     ::nil
                                     ex)))
        respond (fn request-async-respond [response]
                  (try
                    (async/put! chan response)
                    (catch #?(:clj  Throwable
                              :cljs :default) e
                      (raise e))))]
    #?(:clj  (-> req
                 (assoc :async? true)
                 (c/request respond raise))
       :cljs (let [{:keys [url method query-params mode headers]} req
                   search                                         (new js/URLSearchParams)]
               (doseq [[k v] query-params]
                 (.append search k v))
               (-> (js/fetch (str url "?" (.toString search))
                             #js{:method      (string/upper-case (name method))
                                 :queryParams (clj->js query-params)
                                 :mode        mode
                                 :headers     headers})
                   (.then (fn [response]
                            (-> response
                                .json
                                (.then (fn [response]
                                         (respond response)))
                                (.catch (fn [err]
                                          (raise err))))))
                   (.catch (fn [err]
                             (raise err))))))
    chan))
