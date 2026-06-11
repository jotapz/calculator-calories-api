(ns backend.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(def transacoes (atom [
  {:alimento "banana", :calorias 100 :typpe "ganho"}
]))

(defroutes app-routes
  (GET "/" [] "servidor da calculadora ta rodando hehe")
  (GET "/extrato" [] (str @transacoes))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
