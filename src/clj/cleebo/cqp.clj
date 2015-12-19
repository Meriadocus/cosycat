(ns cleebo.cqp
  (:require [com.stuartsierra.component :as component]
            [cqp-clj.core :refer [make-cqi-client connect! disconnect!]]
            [cqp-clj.spec :refer [read-init]]
            [environ.core :refer [env]]
            [taoensso.timbre :as timbre]))

(defrecord CQiComponent [client init-file]
  component/Lifecycle
  (start [component]
    (let [client (:client (make-cqi-client (read-init init-file)))]
      (timbre/info "Connected to CQPServer")
      (assoc component :client client)))
  (stop [component]
    (timbre/info "Shutting down connection to CQPServer")
    (disconnect! (:client component))
    (assoc component :client nil)))

(defn new-cqi-client [{:keys [init-file]}]
  (map->CQiComponent {:init-file init-file}))

;; (defn create-attr [[type name]] {:attr-type type :attr-name name})

;; (def attrs (map create-attr [[:pos "word"] [:pos "pos"]]))
;; (def spec (read-init "dev-resources/cqpserver.init"))

;; (def result
;;   (cqp/with-cqi-client [client (cqp/make-cqi-client spec)]
;;     (cqp/query! client "PYCCLE-ECCO" "'goin.*' @'.*' 'to'" "latin1")
;;     (cqp/cpos-seq-handler
;;      client
;;      "PYCCLE-ECCO"
;;      (cqp/cpos-range client "PYCCLE-ECCO" 0 10)
;;      2
;;      attrs)))