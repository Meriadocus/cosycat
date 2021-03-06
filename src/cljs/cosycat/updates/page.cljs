(ns cosycat.updates.page
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [react-bootstrap.components :as bs]
            [cosycat.components :refer [css-transition-group]]
            [cosycat.updates.components.event-component :refer [event-component]]
            [taoensso.timbre :as timbre]))

(defn history-panel []
  (let [app-history (re-frame/subscribe [:read-history [:app-events]])
        project-history (re-frame/subscribe [:read-history [:project-events]])
        user-history (re-frame/subscribe [:read-history [:user-events]])]
    (fn []
      [bs/list-group
       [css-transition-group {:transition-name "updates"
                              :transition-enter-timeout 650
                              :transition-leave-timeout 650}
        (doall (for [{:keys [received type payload] :as event}
                     (sort-by :received > (concat @app-history @project-history @user-history))]
                 ^{:key received} [event-component event]))]])))

(defn updates-panel []
  (fn []
    [:div.container-fluid
     [:div.row
      [:div.col-lg-2.col-sm-1]
      [:div.col-lg-8.col-sm-10 [:h3 [:span.text-muted "Updates (what's new in your feed)"]]]
      [:div.col-lg-2.col-sm-1]]
     [:div.row
      [:div.col-lg-2.col-sm-1]
      [:div.col-lg-8.col-sm-10 [:hr]]
      [:div.col-lg-2.col-sm-1]]
     [:div.container-fluid
      [:div.row
       [:div.col-lg-2.col-sm-1]
       [:div.col-lg-8.col-sm-10 [history-panel]]
       [:div.col-lg-2.col-sm-1]]]]))
