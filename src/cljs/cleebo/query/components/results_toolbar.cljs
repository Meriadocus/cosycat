(ns cleebo.query.components.results-toolbar
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [taoensso.timbre :as timbre]
            [react-bootstrap.components :as bs]
            [goog.string :as gstr]
            [cleebo.utils :refer [->default-map by-id]]
            [cleebo.routes :refer [nav!]]
            [cleebo.components :refer [disabled-button-tooltip]]
            [cleebo.query.components.annotation-modal :refer [annotation-modal-button]]))

(defn pager-button [& {:keys [direction label]}]
  [bs/button
   {:onClick #(re-frame/dispatch [:query-range direction :results-frame])
    :style {:font-size "12px" :height "34px" :width "70px"}}
   label])

(defn pager-buttons []
  [bs/button-toolbar
   {:justified true}
   [pager-button
    :direction :prev
    :label [:div [:i.zmdi.zmdi-arrow-left {:style {:margin-right "10px"}}] "prev"]] 
   [pager-button
    :direction :next
    :label [:div "next" [:i.zmdi.zmdi-arrow-right {:style {:margin-left "10px"}}]]]])

(defn query-result-label []
  (let [results-summary (re-frame/subscribe [:project-session :query :results-summary])]
    (fn []
      (let [{{from :from to :to} :page query-size :query-size} @results-summary]
        [:label
         {:style {:line-height "35px"}}
         (gstr/format "%d-%d of %d hits" (inc from) to query-size)]))))

(defn result-label-pager []
  (fn []
    [:div.container-fluid
     [:div.row
      [:div.col-lg-3.col-sm-5 [query-result-label]]
      [:div.col-lg-9.col-sm-7 [:div.pull-left [pager-buttons]]]]]))

(defn mark-all-hits-btn []
  [bs/button
   {:onClick #(re-frame/dispatch [:mark-all-hits])
    :style {:font-size "12px" :height "34px"}}
   "Mark hits"])

(defn mark-buttons []
  [bs/button-toolbar
   [mark-all-hits-btn]
   [annotation-modal-button]])

(defn results-toolbar []
  (fn []
    [:div.row
     [:div.col-sm-6 [result-label-pager]]
     [:div.col-sm-6 [:div.pull-right [mark-buttons]]]]))