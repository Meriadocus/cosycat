(ns cleebo.handler
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]
            [environ.core :refer [env]]
            [compojure.core :refer [GET POST ANY HEAD defroutes wrap-routes]]
            [compojure.route :as route]
            [prone.middleware :refer [wrap-exceptions]]
            [cleebo.views.error :refer [error-page]]
            [cleebo.views.cleebo :refer [cleebo-page]]
            [cleebo.views.landing :refer [landing-page]]
            [cleebo.views.about :refer [about-page]]
            [cleebo.views.login :refer [login-page]]
            [ring.util.response :refer [redirect]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.anti-forgery
             :refer [wrap-anti-forgery *anti-forgery-token*]]
            [ring.middleware.transit :refer [wrap-transit-response wrap-transit-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.nested-params :refer [wrap-nested-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring-ttl-session.core :refer [ttl-memory-store]]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [cleebo.routes.auth :refer [is-logged? safe auth-backend token-backend
                                        login-route logout-route signup-route]]
            [cleebo.components.ws :refer [ws-handler-http-kit send-clients]]
            [cleebo.routes.blacklab :refer [blacklab-router]]
            [cleebo.routes.sessions :refer [session-route]]
            [cleebo.routes.projects :refer [project-route]]
            [cleebo.routes.settings :refer [settings-route]]
            [cleebo.routes.annotations :refer [annotation-route]]))

(def about-route
  (safe
   (fn [req] (about-page :logged? (is-logged? req)))
   {:login-uri "/login" :is-ok? authenticated?}))

(def cleebo-route
  (safe
   (fn [req]
     (cleebo-page :csrf *anti-forgery-token*))
   {:login-uri "/login" :is-ok? authenticated?}))

(defroutes app-routes
  (GET "/" req (landing-page :logged? (is-logged? req)))
  (GET "/login" [] (login-page :csrf *anti-forgery-token*))
  (POST "/login" [] login-route)
  (POST "/signup" [] signup-route)
  (GET "/about" [] about-route)
  (GET "/cleebo" [] cleebo-route)
  (GET "/session" [] session-route)
  (POST "/settings" [] settings-route)
  (POST "/annotation" [] annotation-route)
  (POST "/project" [] project-route)
  (ANY "/logout" [] logout-route)
  (GET "/blacklab" [] blacklab-router)
  (GET "/ws" [] ws-handler-http-kit)
  (route/resources "/")
  (route/files "/" {:root (:dynamic-resource-path env)})
  (route/not-found (error-page :status 404 :title "Page not found!!")))

(defn is-ajax
  "not sure how robust this is"
  [{headers :headers :as req}]
  ;; (timbre/debug headers)
  (boolean (= "XMLHttpRequest" (get headers "X-Requested-With"))))

;;; middleware
(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (if (is-ajax req)
          {:status 500
           :body {:message "Oops! Something bad happened!"
                  :data {:exception (str (class t)) :type :internal-error}}}
          (error-page
           {:status 500
            :title "Something very bad happened!"
            :message (str (class t))}))))))

(defn wrap-base [handler]
  (-> handler   
      wrap-reload
      (wrap-authorization auth-backend)
      (wrap-authentication auth-backend)
;      (wrap-authorization token-backend)
;      (wrap-authentication token-backend)
      (wrap-anti-forgery {:read-token (fn [req] (get-in req [:params :csrf]))})
      (wrap-session {:store (ttl-memory-store (* (env :session-expires) 60))})
      (wrap-transit-params {:encoding :json-verbose})
      wrap-keyword-params
      wrap-nested-params
      wrap-params
      (wrap-transit-response {:encoding :json-verbose})
      wrap-exceptions
      wrap-internal-error))

(def web-app (wrap-routes #'app-routes wrap-base))

(defn wrap-app-component [handler components]
  (fn [req]
    (handler (assoc req :components components))))

(defn make-handler [component]
  (let [components (select-keys component (:components component))]
    (-> app-routes
        (wrap-app-component components)
        (wrap-routes wrap-base))))
