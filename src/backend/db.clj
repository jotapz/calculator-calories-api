(ns backend.db)

(def dados-usuario (atom {}))

(def registros (atom '()))

(defn cadastrar-usuario [dados]
  (reset! dados-usuario dados)
  @dados-usuario)

(defn consultar-usuario []
  @dados-usuario)

(defn transacoes []
  @registros)

(defn registrar [transacao]

  (let [colecao-atualizada (swap! registros conj transacao)]
    (merge transacao {:id (count colecao-atualizada)})))


(defn transacoes-por-periodo [data-inicio data-fim]
  (filter (fn [t]
            (let [data (:data t)]
              (and (>= (compare data data-inicio) 0)
                   (<= (compare data data-fim) 0))))
          (transacoes)))

(defn saldo []
  (reduce (fn [acumulado transacao]
            (let [calorias (:calorias transacao)]
              (if (= (:tipo transacao) "ganho")
                (+ acumulado calorias)
                (- acumulado calorias))))
          0
          (transacoes)))

(defn saldo-por-periodo [data-inicio data-fim]
  (reduce (fn [acumulado transacao]
            (let [calorias (:calorias transacao)]
              (if (= (:tipo transacao) "ganho")
                (+ acumulado calorias)
                (- acumulado calorias))))
          0
          (transacoes-por-periodo data-inicio data-fim)))

(defn limpar []
  (reset! registros '()))