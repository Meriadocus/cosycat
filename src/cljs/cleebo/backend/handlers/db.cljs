(ns cleebo.backend.handlers.db
  (:require [re-frame.core :as re-frame]
            [schema.core :as s]
            [cleebo.app-utils :refer [deep-merge]]
            [cleebo.backend.db :refer [default-db]]
            [cleebo.localstorage :as ls]
            [cleebo.backend.middleware
             :refer [standard-middleware no-debug-middleware]]
            [cleebo.schemas.app-state-schemas :refer [db-schema]]
            [taoensso.timbre :as timbre]))

(re-frame/register-handler
 :initialize-db
 (fn [_ [_ & [overwrite-init-state]]]
   (if overwrite-init-state
     (deep-merge default-db overwrite-init-state)
     default-db)))

(re-frame/register-handler
 :reset-db
 no-debug-middleware
 (fn [_ _]
   default-db))

(re-frame/register-handler
 :load-db
 standard-middleware
 (fn [db [_ new-db]]
   (try
     (s/validate db-schema new-db)
     new-db
     (catch :default e
       (re-frame/dispatch [:notify
                           {:message "Oops! Couldn't load backup"
                            :status :error}])
       db))))

(re-frame/register-handler
 :dump-db
 standard-middleware
 (fn [db _]
   (let [now (js/Date)]
     (ls/put now db)
     (re-frame/dispatch
      [:notify
       {:message "State succesfully backed-up"
        :status :ok}]))
   db))

(re-frame/register-handler
 :print-db
 standard-middleware
 (fn [db [_ & [path]]]
   (timbre/info (if path (get-in db path) db))
   db))

