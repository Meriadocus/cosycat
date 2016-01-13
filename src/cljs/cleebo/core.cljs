(ns cleebo.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [cleebo.handlers]
            [cleebo.subs]
            [cleebo.routes :as routes]
            [cleebo.ws :refer [make-ws-ch]]
            [cleebo.pages.query :refer [query-panel]]
            [cleebo.pages.settings :refer [settings-panel]]            
            [taoensso.timbre :as timbre]
;            [environ.core :refer [env]]
            [figwheel.client :as figwheel]))

(defmulti panels identity)
(defmethod panels :query-panel [] [query-panel])
(defmethod panels :settings-panel [] [settings-panel])
(defmethod panels :default [] [:div])

(defn sidelink [target href label icon]
  (let [active (re-frame/subscribe [:active-panel])]
    (fn []
      [:li {:class (when (= @active target) "active")}
       [:a {:href href :style {:color "#fff2ef"}}
        [re-com/h-box 
         :gap "7px"
         :children
         [[:i
           {:class (str "zmdi " icon)
            :style {:line-height "20px" :font-size "15px"}}]
          [re-com/label :label label]]]]])))

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [:div.container-fluid
       [:div.row 
        [:div.col-sm-2.col-md-1.sidebar ;sidebar
         [:ul.nav.nav-sidebar
          [sidelink :home-panel "#/home" "Home" "zmdi-home"]
          [sidelink :query-panel "#/query" "Query" "zmdi-search"]
          [sidelink :updates-panel "#/updates" "Updates" "zmdi-notifications"]
          [sidelink :settings-panel "#/settings" "Settings" "zmdi-settings"]      
          [sidelink :exit          "#/exit" "Exit" "zmdi-power"]]]
        [:div.col-sm-10.col-sm-offset-2.col-md-11.col-md-offset-1.main
         (panels @active-panel)]]])))

(defn mount-root []
  (.log js/console "Called mount-root")
  (reagent/render [#'main-panel] (.getElementById js/document "app")))

(defn set-ws-ch []
  (make-ws-ch
   (str "ws://" (.-host js/location) "/ws")
   #(re-frame/dispatch [:ws-in %])))

(defn ^:export init []
  (let [host "localhost";(get env :host "146.175.15.30")
        ]
    (routes/app-routes)
    (set-ws-ch)
    (re-frame/dispatch-sync [:initialize-db])
    (mount-root)
    (figwheel/start {:websocket-url (str "ws://" host ":3449/figwheel-ws")})))
