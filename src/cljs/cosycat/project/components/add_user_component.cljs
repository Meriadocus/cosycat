(ns cosycat.project.components.add-user-component
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [react-bootstrap.components :as bs]
            [cosycat.components :refer [user-profile-component]]
            [cosycat.roles :refer [project-user-roles]]
            [cosycat.utils :refer [human-time by-id]]
            [cosycat.app-utils :refer [ceil pending-users]]
            [taoensso.timbre :as timbre]))

(defn remove-project-users [users project-users]
  (remove #(contains? (apply hash-set (map :username project-users)) (:username %)) users))

(defn add-user-btn [username-input-show]
  (fn [username-input-show]
    [:div.container-fluid
     [:div.row
      [:div.col-sm-4.col-md-4]
      [:div.col-sm-4.col-md-4
       [bs/button {:bsSize "large" :onClick #(swap! username-input-show not)}
        [:h4 [:img.img-rounded.img-responsive {:src "img/add_user_icon.png"}]]]]
      [:div.col-sm-4.col-md-4]]]))

(defn on-user-select [users selected-user-atom]
  (fn [target]
    (when (= 13 (.-charCode target))
      (let [selected-user (->> users (filter #(= (by-id "username-input") (:username %))) first)]
        (reset! selected-user-atom selected-user)))))

(defn username-input [username-input-show selected-user-atom project-users]
  (let [users (re-frame/subscribe [:users])]
    (fn [username-input-show selected-user-atom project-users]
      (let [eligible-users (remove-project-users @users project-users)]
        [:div.container-fluid
         [:div.row
          ;; [users-autocomplete
          ;;  {:id "username-input"
          ;;   :class "form-control form-control-no-border"
          ;;   :users eligible-users
          ;;   :on-key-press (on-user-select eligible-users selected-user-atom)
          ;;   :on-blur #(swap! username-input-show not)}]
          ]
         [:div.row
          [:span.text-muted.pull-right
           "Insert username"]]]))))

(defn add-user-component [project-users]
  (let [active-project (re-frame/subscribe [:active-project :name])
        selected-user-atom (reagent/atom nil)
        username-input-show (reagent/atom false)]
    (fn [project-users]
      (if @selected-user-atom
        [user-profile-component @selected-user-atom project-user-roles
         :editable? true
         :on-dismiss #(reset! selected-user-atom nil)
         :on-submit (fn [user role]
                      (re-frame/dispatch
                       [:project-add-user
                        {:user {:username (:username user) :role role}
                         :project-name @active-project}])
                      (reset! selected-user-atom nil))]
        [:div.container-fluid
         [:div.row {:style {:height "85px"}}
          [:h4.text-center "Invite someone to " [:span.text-muted @active-project]]]
         [:div.row {:style {:height "92px"}}
          (cond
            (not @username-input-show) [add-user-btn username-input-show]
            (not @selected-user-atom) [username-input
                                       username-input-show selected-user-atom project-users])]
         [:div.row {:style {:height "85px"}}]]))))
