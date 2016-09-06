(ns cosycat.routes.blacklab
  (:require [compojure.core :refer [routes GET]]
            [cosycat.utils :refer [->int ->keyword]]
            [cosycat.routes.utils :refer [safe]]
            [cosycat.db.annotations :as ann-db]
            [cosycat.db.projects :as proj-db]
            [cosycat.components.blacklab :refer
             [bl-query bl-query-range bl-sort-query bl-sort-range bl-snippet remove-hits!]]
            [taoensso.timbre :as timbre]))

(defn bl-query-route
  [{{{username :username} :identity} :session 
    {blacklab :blacklab} :components
    {corpus :corpus query-str :query-str
     context :context size :size from :from} :params}]
  (let [from (->int from)
        to (+ from (->int size))
        context (->int context)
        query-id username]
    (bl-query blacklab corpus query-str from to context query-id)))

(defn bl-query-range-route
  [{{{username :username} :identity} :session 
    {blacklab :blacklab} :components
    {corpus :corpus context :context to :to from :from} :params}]
  (let [from (->int from)
        to (->int to)
        context (->int context)
        query-id username]
    (bl-query-range blacklab corpus from to context query-id)))

(defn bl-sort-query-route
  [{{{username :username} :identity} :session 
    {blacklab :blacklab} :components
    {corpus :corpus query-str :query-str context :context to :to from :from
     {criterion :criterion attribute :attribute} :sort-map} :params}]
  (let [from (->int from)
        to (->int to)
        context (->int context)
        sort-map {:criterion (keyword criterion) :attribute attribute}
        query-id username]
    (bl-sort-query blacklab corpus from to context sort-map query-id)))

(defn bl-sort-range-route
  [{{{username :username} :identity} :session 
    {blacklab :blacklab} :components
    {corpus :corpus query-str :query-str context :context to :to from :from
     {criterion :criterion attribute :attribute} :sort-map} :params}]
  (let [from (->int from)
        to (->int to)
        context (->int context)
        sort-map {:criterion (keyword criterion) :attribute attribute}        
        query-id username]
    (bl-sort-range blacklab corpus from to context sort-map query-id)))

(defn bl-snippet-route
  [{{{username :username} :identity} :session
    {blacklab :blacklab} :components
    {hit-idx :hit-idx snippet-size :snippet-size} :params}]
  (let [query-id username
        hit-idx (->int hit-idx)
        snippet-size (->int snippet-size)]
    (bl-snippet blacklab hit-idx snippet-size query-id)))

;; (defn with-results-annotations [body db username project-name]
;;   (let [{:keys [results] :as out} body]
;;     (assoc body :results (ann-db/merge-annotations db results project-name))))

(defn with-results-annotations [body db username project-name]
  body)

(defmulti blacklab-router
  (fn [{{route :route} :params :as req}]
    (->keyword route)))

(defmethod blacklab-router :query
  [{{db :db} :components {{username :username} :identity} :session :as req}]
  (with-results-annotations (bl-query-route req) db username))

(defmethod blacklab-router :query-range
  [{{db :db} :components {{username :username} :identity} :session :as req}]
  (with-results-annotations (bl-query-range-route req) db username))

(defmethod blacklab-router :sort-query
  [{{db :db} :components {{username :username} :identity} :session :as req}]  
  (with-results-annotations (bl-sort-query-route req) db username))

(defmethod blacklab-router :sort-range 
  [{{db :db} :components {{username :username} :identity} :session :as req}]
  (with-results-annotations (bl-sort-range-route req) db username))

(defmethod blacklab-router :snippet [req]
  (bl-snippet-route req))

(defn blacklab-routes []
  (routes (GET "/blacklab" [] (safe (fn [req] {:status 200 :body (blacklab-routes req)})))))