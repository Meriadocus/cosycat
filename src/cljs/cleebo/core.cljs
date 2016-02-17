(ns cleebo.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [cleebo.backend.handlers]
            [cleebo.backend.subs]
            [cleebo.routes :as routes]
            [cleebo.ws :refer [make-ws-ch]]
            [cleebo.query.page :refer [query-panel]]
            [cleebo.annotation.page :refer [annotation-panel]]
            [cleebo.settings.page :refer [settings-panel]]
            [cleebo.updates.page :refer [updates-panel]]
            [cleebo.debug.page :refer [debug-panel]]
            [cleebo.utils :refer [notify!]]
            [taoensso.timbre :as timbre]
            [figwheel.client :as figwheel])
  (:require-macros [cleebo.env :as env :refer [cljs-env]]))

(defmulti panels identity)
(defmethod panels :query-panel [] [query-panel])
(defmethod panels :settings-panel [] [settings-panel])
(defmethod panels :debug-panel [] [debug-panel])
(defmethod panels :updates-panel [] [updates-panel])
(defmethod panels :annotation-panel [] [annotation-panel])

(defn sidelink [target href label icon]
  (let [active (re-frame/subscribe [:active-panel])]
    (fn []
      [:li {:class (when (= @active target) "active")}
       [:a {:href href :style {:color "#fff2ef"}}
        [:span [:i {:class (str "zmdi " icon)                    
                    :style {:line-height "20px"
                            :font-size "15px"
                            :margin-right "5px"}}]
         label]]])))

(defn notification [id message]
  ^{:key id}
  [:li#notification
   {:on-click #(re-frame/dispatch [:drop-notification id])}
   message])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [:div
       [:div.container-fluid
        [:div.row
         [:div.col-sm-2.col-md-1.sidebar ;sidebar
          [:ul.nav.nav-sidebar
           [sidelink :query-panel "#/query" "Query" "zmdi-search"]
           [sidelink :annotation-panel "#/annotation" "Annotation" "zmdi-edit"]
           [sidelink :updates-panel "#/updates" "Updates" "zmdi-notifications"]
           [sidelink :settings-panel "#/settings" "Settings" "zmdi-settings"]
           [sidelink :debug-panel "#/debug" "Debug" "zmdi-bug"]          
           [sidelink :exit          "#/exit" "Exit" "zmdi-power"]]]
         [:div.col-sm-10.col-sm-offset-2.col-md-11.col-md-offset-1.main
          {:style {:padding-left "0px"}}
          [:div {:style {:padding-left "15px"}}
           (panels @active-panel)]]]]])))

(defn mount-root []
  (.log js/console "Called mount-root")
  (reagent/render [#'main-panel] (.getElementById js/document "app")))

(defn set-ws-ch []
  (make-ws-ch
   (str "ws://" (.-host js/location) "/ws")
   #(re-frame/dispatch [:ws-in %])))

(defn ^:export init []
  (let [host (cljs-env :host)]
    (routes/app-routes)
    (set-ws-ch)
    (re-frame/dispatch-sync [:initialize-db])
    (mount-root)
    (figwheel/start {:websocket-url (str "ws://" host ":3449/figwheel-ws")})))
