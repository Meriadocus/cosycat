(ns cleebo.views.cleebo
  (:require [hiccup.core :refer [html]]
            [cleebo.views.layout :refer [bootstrap-css]]))

(defn cleebo-page [& {:keys [csrf]}]
  (html
   [:html
    {:lang "en"}
    [:head
     [:meta {:charset "utf-8"}]
     [:link {:href bootstrap-css :rel "stylesheet"}]
     [:link {:href "vendor/css/material-design-iconic-font.min.css" :rel "stylesheet"}]
     [:link {:href "vendor/css/dashboard.css" :rel "stylesheet"}]
     [:link {:href "vendor/css/re-com.css", :rel "stylesheet"}]
     [:link {:href "css/main.css" :rel "stylesheet"}]
     [:link
      {:type "text/css" :rel "stylesheet"
       :href "http://fonts.googleapis.com/css?family=Roboto:300,400,500,700,400italic"}]
     [:link
      {:type "text/css" :rel "stylesheet"
       :href "http://fonts.googleapis.com/css?family=Roboto+Condensed:400,300"}]]
    [:body
     [:div#app]
     [:script (str "var csrf =\"" csrf "\";")]
     [:script {:src "js/compiled/app.js"}]
     [:script "cleebo.core.init();"]]]))
