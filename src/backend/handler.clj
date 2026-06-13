(ns backend.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [cheshire.core :as json]
            [clj-http.client :as client]
            [backend.apiIgnore :as api]
            [backend.db :as db]))

(def usda-key "E8YESy0vfvPpVxhFfhQIfYgok5Kkl1yeil5yT9cy")

(defn buscar-calorias-alimento [nome quantidade]
  (let [busca     (client/get "https://api.nal.usda.gov/fdc/v1/foods/search"
                              {:query-params {:query    nome
                                             :pageSize 1
                                             :dataType "Foundation,SR Legacy"
                                             :api_key  usda-key}
                               :as :json})
        alimento  (first (get-in busca [:body :foods]))
        nutriente (first (filter #(= (:nutrientId %) 1008)
                                 (:foodNutrients alimento)))
        calorias-por-100g (or (:value nutriente) 0)]
    (* calorias-por-100g (/ quantidade 100.0))))

(def api-key "FPMcg4KM6VTDqQMW8uHSYrATmbnNo4UEFVuohsHR")
(def headers {"X-Api-Key" api-key})

(defn buscar-calorias-atividade [nome duracao-minutos]
  (let [resposta (client/get "https://api.api-ninjas.com/v1/caloriesburned"
                             {:headers      headers
                              :query-params {:activity nome}
                              :as           :json})
        itens    (:body resposta)]
    (if (empty? itens)
      0
      (let [calorias-por-hora (:calories_per_hour (first itens))]
        (* (/ calorias-por-hora 60.0) duracao-minutos)))))

(defn jsonn
  ([retorno]
   (jsonn retorno 200))
  ([retorno status]
   {:status status :headers {"Content-Type" "application/json"} :body (json/generate-string retorno)}))

(defn invalida [retorno]
  (jsonn {:erro retorno} 400))

(defroutes app-routes

  (POST "/usuario" requisicao
    (let [dados (:body requisicao)]
      (jsonn (db/cadastrar-usuario dados) 201)))

  (GET "/usuario" []
    (let [usuario (db/consultar-usuario)]
      (if (empty? usuario)
        (invalida "Nenhum usuário cadastrado.")
        (jsonn usuario))))

  (POST "/alimento" requisicao
    (let [dados     (:body requisicao)
          calorias  (api/buscar-calorias-alimento (:nome dados) (:quantidade dados))
          transacao {:tipo       "ganho"
                     :categoria  "alimento"
                     :nome       (:nome dados)
                     :data       (:data dados)
                     :quantidade (:quantidade dados)
                     :calorias   calorias}]
      (jsonn (db/registrar transacao) 201)))

  (POST "/atividade" requisicao
    (let [dados     (:body requisicao)
          calorias  (api/buscar-calorias-atividade (:nome dados) (:duracao dados))
          transacao {:tipo      "perda"
                     :categoria "atividade"
                     :nome      (:nome dados)
                     :data      (:data dados)
                     :duracao   (:duracao dados)
                     :calorias  calorias}]
      (jsonn (db/registrar transacao) 201)))

  (GET "/extrato" {params :params}
    (if (and (contains? params :data-inicio) (contains? params :data-fim))
      (jsonn {:transacoes (db/transacoes-por-periodo (:data-inicio params) (:data-fim params))})
      (jsonn {:transacoes (db/transacoes)})))

  (GET "/saldo" {params :params}
    (if (and (contains? params :data-inicio) (contains? params :data-fim))
      (jsonn {:saldo (db/saldo-por-periodo (:data-inicio params) (:data-fim params))})
      (jsonn {:saldo (db/saldo)})))

  (route/not-found "Rota nao encontrada!"))

(def app
  (-> (wrap-defaults app-routes api-defaults)
      (wrap-json-body {:keywords? true :bigdecimals? true})))