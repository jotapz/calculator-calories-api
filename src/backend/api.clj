(ns backend.api
  (:require [clj-http.client :as client]))


(def usda-key "SUA API KEY")

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

(def api-key "SUA API KEY")
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