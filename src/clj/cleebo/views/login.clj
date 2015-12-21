(ns cleebo.views.login
  (:require [cleebo.views.layout :refer [base]]))

(declare tabs)

(defn alert [title]
  [:div.alert.alert-danger
   {:style "border-right:none;
            color:#333;
            background-color:rgba(255, 0, 0, 0.1);
            padding: 7px;
            border-left:4px solid rgba(255, 0, 0, 0.8);
            border-top:none;
            border-radius:0px;
            border-bottom:none;"}
   title])

(defn login-page [& {:keys [csrf error-msg]}]
  (base
   {:left  [:p.lead "Please login to access this resource"]
    :right (tabs csrf error-msg)
    :logged? false}))

(defn tabs [csrf & [error-msg]]
  [:div.panel.with-nav-tabs.panel-default
   [:div.panel-heading
    [:ul.nav.nav-tabs
     [:li.active [:a {:href "#login"  :data-toggle "tab"} "Logins"]]
     [:li        [:a {:href "#signup" :data-toggle "tab"} "Join"]]]]
   [:div.panel-body
    [:div.tab-content
     [:div#login.tab-pane.fade.in.active ;login
      [:form.form-horizontal {:action "/login" :method :post}
       [:input {:style "display:none;" :name "csrf" :value csrf}]
       [:div.input-group {:style "margin-bottom: 25px"}
        [:span.input-group-addon [:i.glyphicon.glyphicon-user]]
        [:input.form-control
         {:placeholder "username or email"
          :value ""
          :name "username"
          :type "text"}]]
       [:div.input-group
        {:style "margin-bottom: 25px"}
        [:span.input-group-addon [:i.glyphicon.glyphicon-lock]]
        [:input.form-control
         {:placeholder "password"
          :value ""
          :name "password"
          :type "password"}]]
       [:div.form-group
        {:style "margin-top:10px"}
        [:div {:style "text-align: right;"}
         [:div.col-sm-12.controls
          [:div.row
           [:div.col-md-8 {:style "text-align: left;"}
            (if error-msg (alert error-msg) "")]
           [:div.col-md-4
            [:button.btn-login.btn.btn-success {:type "submit"} "Login"]]]]
         [:br]
         [:div {:style "font-size: 12px; padding: 25px 10px 0 0;"}
          [:a {:href "#"} "forgot password?"]]]]]]
     [:div#signup.tab-pane              ;signup
      [:form.form-horizontal {:action "/signup" :method :post}
       [:input {:style "display:none;" :name "csrf" :value csrf}]
       [:div.input-group {:style "margin-bottom: 25px"}
        [:span.input-group-addon [:i.glyphicon.glyphicon-user]]
        [:input.form-control
         {:placeholder "username or email"
          :value ""
          :name "username"
          :type "text"}]]
       [:div.input-group
        {:style "margin-bottom: 25px"}
        [:span.input-group-addon [:i.glyphicon.glyphicon-lock]]
        [:input.form-control
         {:placeholder "password"
          :value ""
          :name "password"
          :type "password"}]]
       [:div.input-group
        {:style "margin-bottom: 25px"}
        [:span.input-group-addon [:i.glyphicon.glyphicon-lock]]
        [:input.form-control
         {:placeholder "repeat password"
          :value ""
          :name "repeatpassword"
          :type "password"}]]
       [:div.form-group
        {:style "margin-top:10px"}
        [:div.pull-right
         [:div.col-sm-12.controls
          [:button.btn-login.btn.btn-success {:type "submit"} "Join"]]]]]]]]])
