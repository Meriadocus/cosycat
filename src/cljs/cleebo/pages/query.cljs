(ns cleebo.pages.query
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [cleebo.ws :refer [send-transit-msg!]]
            [ajax.core :refer [GET]]
            [goog.string :as gstr]
            [goog.dom.dataset :as gdataset]
            [goog.fx.dom :as gfx]
            [goog.fx.easing :as gfx-easing]
            [taoensso.timbre :as timbre])
  (:import [goog.fx Animation]))

(defn by-id [id]
  (.getElementById js/document id))

(defn pager-next
  ([size page-size] (pager-next size page-size 0))
  ([size page-size from]
   (let [to (+ from page-size)]
     (cond
       (= from size) [0 (min page-size size)]
       (>= to size)  [from size]
       :else         [from to]))))

(defn pager-prev
  ([size page-size] (pager-prev size page-size 0))
  ([size page-size from]
   (let [new-from (- from page-size)]
     (cond (zero? from)     [(- size page-size) size]
           (zero? new-from) [0 page-size]
           (neg?  new-from) [0 (+ new-from page-size)]
           :else            [new-from from]))))

(defn error-panel [{:keys [status status-text]}]
  [re-com/v-box
   :align :center
   :padding "40px"
   :gap "10px"
   :children
   [[:h3 [:span.text-muted status]]
    [:br]
    status-text]])

(defn error-handler [{:keys [status status-text]}]
  (re-frame/dispatch
   [:set-session [:query-results :status] {:status status :status-text status-text}]))

(defn query-results-handler [data]
  (let [{query-size :query-size} data
        data (if (zero? query-size) (assoc data :results nil) data)]
    (timbre/debug "results-handler" data)
    (re-frame/dispatch [:set-query-results data])
    (re-frame/dispatch [:stop-throbbing :results-frame])))

(defn query
  "will need to support 'from' for in-place query-opts change"
  [{:keys [query-str corpus context size from] :or {from 0}}]
  (re-frame/dispatch [:start-throbbing :results-frame])
  (GET "/query"
       {:handler query-results-handler
        :error-handler error-handler
        :params {:query-str query-str
                 :corpus corpus
                 :context context
                 :from from
                 :size size}}))

(defn query-range [{:keys [corpus from to context]}]
  (re-frame/dispatch [:start-throbbing :results-frame])
  (GET "/range"
       {:handler query-results-handler
        :error-handler error-handler
        :params {:corpus corpus
                 :from from
                 :to to
                 :context context}}))

(defn on-key [k thunk]
  (fn [e]
    (if (= (.-charCode e) k)
      (thunk))))

(defn query-field []
  (let [query-opts (re-frame/subscribe [:query-opts])
        query-str (re-frame/subscribe [:session :query-results :query-str])]
    (fn []
      [re-com/h-box
       :justify :between
       :children
       [[:h4 [:span.text-muted {:style {:line-height "15px"}} "Query Panel"]]
        [:div.form-group.has-feedback
         [:input#query-str.form-control
          {:style {:width "640px"}
           :type "text"
           :name "query"
           :placeholder (or @query-str "Example: [pos='.*\\.']") ;remove?
           :autocorrect "off"
           :autocapitalize "off"
           :spellcheck "false"
           :on-key-press
           (on-key 13
            #(let [query-str (.-value (by-id "query-str"))]
               (query (assoc @query-opts :query-str query-str :type "type"))))}
          [:i.zmdi.zmdi-search.form-control-feedback
           {:style {:font-size "1.75em" :line-height "35px"}}]]]]])))

(defn dropdown-opt [& {:keys [k placeholder choices width] :or {width "125px"}}]
  {:pre [(and k placeholder choices)]}
  [re-com/single-dropdown
   :style {:font-size "12px"}
   :width width
   :placeholder placeholder
   :choices choices
   :model @(re-frame/subscribe [:session :query-opts k])
   :label-fn #(str placeholder (:id %))
   :on-change #(re-frame/dispatch [:set-session [:query-opts k] %])])

(defn query-opts-menu []
  [re-com/h-box
   :justify :end
   :gap "5px"
   :children
   [[dropdown-opt
     :k :corpus
     :placeholder "Corpus: "
     :choices [{:id "DICKENS"} {:id "PYCCLE-ECCO"} {:id "MBG-CORPUS"}]
     :width "175px"]
    [dropdown-opt
     :k :size
     :placeholder "Page size: "
     :choices (map (partial hash-map :id) (range 1 25))]
    [dropdown-opt
     :k :context
     :placeholder "Window size: "
     :choices (map (partial hash-map :id) (range 1 10))]]])

(defn nav-button [pager-fn label]
  (let [query-opts (re-frame/subscribe [:query-opts])
        query-results (re-frame/subscribe [:query-results])]
    (fn []
      [re-com/button
       :style {:font-size "12px" :height "34px"}
       :label label
       :on-click #(let [{:keys [query-size from to]} @query-results
                        {:keys [corpus context size]} @query-opts
                        [from to] (pager-fn query-size size from to)]
                    (query-range {:corpus corpus
                                  :from from
                                  :to to
                                  :context context}))])))

(defn bordered-div [& {:keys [label]}]
  {:pre [label]}
  [:div
   {:style {:font-size "12px"
            :height "34px"
            :line-height "32px"
            :padding "0 0.3em"
            :border "1px solid #ccc"
            :border-right "none"
            :border-radius "4px 0 0 4px"
            :background-color "#ddd"
            :position "static"
            :margin "auto"}}
   label])

(defn nav-buttons []
  (let [query-opts (re-frame/subscribe [:query-opts])
        query-results (re-frame/subscribe [:query-results])]
    (fn []
      [re-com/h-box
       :gap "5px"
       :children
       [[nav-button
         (fn [query-size size from to] (pager-prev query-size size from))
         [:div [:i.zmdi.zmdi-arrow-left {:style {:margin-right "10px"}}] "prev"]] 
        [nav-button
         (fn [query-size size from to] (pager-next query-size size to))
         [:div "next" [:i.zmdi.zmdi-arrow-right {:style {:margin-left "10px"}}]]]
        [re-com/h-box
         :children
         [[bordered-div :label "Go to hit: "]
          [:input#from-hit
           {:type "number"
            :min "1"
            :default-value (inc (:from @query-results))
            :on-key-press
            (on-key 13
               #(let [{:keys [corpus context size]} @query-opts
                      {:keys [query-size]} @query-results
                      from-hit (js/parseInt (.-value (by-id "from-hit")))
                      from-hit (max 0 (min (dec from-hit) query-size))]
                 (query-range
                  {:corpus corpus
                   :from from-hit
                   :to (+ from-hit size)
                   :context context})))
            :style {:width "3.7em"
                    :padding-left "0.3em"
                    :padding-top "2px"
                    :border "1px solid #ccc"
                    :border-radius "0 4px 4px 0"
                    :border-left "none"}}]]]]])))

(defn toolbar []
  (let [query-results (re-frame/subscribe [:query-results])]
    (fn []
      [re-com/h-box
       :justify :between
       :gap "5px"
       :children
       [(if-not (:results @query-results)
          ""
          [re-com/h-box :justify :end :gap "10px"
           :children
           [[re-com/label
             :style {:line-height "30px"}
             :label
             (let [{:keys [from to query-size]} @query-results]
               (gstr/format "Displaying %d-%d of %d hits" (inc from) to query-size))]
            [nav-buttons]]])
        [query-opts-menu]]])))

(defn throbbing-panel []
  [re-com/box
   :align :center
   :padding "50px"
   :child [re-com/throbber :size :large]])

(defn result-by-id [e results-map]
  (let [id (gdataset/get (.-currentTarget e) "num")]
    (get results-map (js/parseInt id))))

(defn fade-out [e]
  (let [anim (gfx/FadeOut. (.-currentTarget e) 1 0.5 5200 gfx-easing/easeOut)]
    (.play anim)))

(defn table-results []
  (let [query-results (re-frame/subscribe [:query-results])]
    (fn []
      [:table.table.table-hover.table-results
       [:thead]
       [:tbody {:style {:font-size "11px"}}
        (for [[i [n row]] (map-indexed vector (:results @query-results))]
          ^{:key i}
          [:tr {:data-num n :on-click #(do (timbre/debug (result-by-id % (:results @query-results)))
                                           (fade-out %))}
           (into [:td (inc n)]
                 (for [{:keys [id word] :as token} row]
                   (cond
                     (:target token) ^{:key (str i "-" id)} [:td.success word]
                     (:match token) ^{:key (str i "-" id)} [:td.info word]
                     :else ^{:key (str i "-" id)} [:td word])))])]])))

(defn results-frame []
  (let [throbbing? (re-frame/subscribe [:throbbing? :results-frame])
        status (re-frame/subscribe [:session :query-results :status])
        query-size (re-frame/subscribe [:session :query-results :query-size])]
    (fn []
      (let [{:keys [status status-text]} @status]
        (cond
          @throbbing?         (throbbing-panel)
          (= status :error)   [error-panel
                              {:status "Ups! something bad happened" :status-text status-text}]
          (zero? @query-size) [error-panel
                              {:status "The query returned no matching results"}]
          :else               [:div [toolbar] [:br] [table-results]])))))

(defn annotation-frame []
  [:div "annotation frame!!!"])

(defn query-main []
  (let [annotation? (atom false)]
    (fn []
      [re-com/v-box :gap "50px"
       :children 
       [[re-com/box :align :stretch :child [results-frame]]
        (when @annotation?
          [re-com/box :align :center :child [annotation-frame]])]])))

(defn query-panel []
  [:div
   [query-field]
   [query-main]])
