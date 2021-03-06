(ns cosycat.autosuggest
  (:require [reagent.core :as reagent]
            [goog.string :as gstr]
            [react-bootstrap.components :as bs]
            [react-autosuggest.core :refer [autosuggest]]))

(defn dekeywordize [k]
  (apply str (rest (str k))))

(defn denormalize-tagset [{:keys [tags]}]
  (->> (seq tags)
       (mapcat (fn [[tag keyvals]] (map #(assoc % :tag (dekeywordize tag)) keyvals)))))

(defn denormalize-tagsets [tagsets]
  (mapv (fn [{:keys [name] :as tagset}]
          {:name name
           :tags (denormalize-tagset tagset)})
        tagsets))

(defn unique-by [f coll]
  (->> (group-by f coll) (reduce-kv (fn [m k v] (assoc m k (first v))) {}) vals flatten))

(defn tags-by [by-name & {:keys [by] :or {by [:scope :tag]}}]
  (map (fn [{:keys [name tags]}]
         {:name name
          :tags (unique-by #(select-keys % by) tags)})
       by-name))

(defn filter-suggs [suggs f & {:keys [dedup-by]}]
  (->> suggs
       (map (fn [{:keys [tags] :as section}]
              (when-let [ftags (seq (filter f tags))]
                (assoc section :tags (if-not dedup-by ftags (unique-by dedup-by ftags))))))
       (filter identity)
       vec))

(defn startswith [s prefix]
  (gstr/caseInsensitiveStartsWith s prefix))

(defn tag->val
  "remap :tag key to :val key"
  [{:keys [tags] :as sugg}]
  (assoc sugg :tags (mapv (fn [{:keys [tag] :as t}]
                            (-> t (assoc :val tag) (dissoc :tag))) tags)))

(defn find-keys
  "returns suggested annotation key(s) per section"
  [s suggs & {:keys [dedup-by]}]
  (->> (if (empty? s)
         suggs
         (filter-suggs suggs (fn [{:keys [tag]}] (startswith tag s)) :dedup-by dedup-by))
       (mapv tag->val)))

(defn find-vals
  "returns suggested annotation val(s) per section"
  [skey sval suggs]
  (if (empty? sval)
    (filter-suggs suggs (fn [{:keys [tag]}] (= tag skey)))
    (filter-suggs suggs (fn [{:keys [tag val]}] (and (= tag skey) (startswith val sval))))))

(defn has-key?
  "user input has key"
  [s]
  (re-find #".*=.*" s))

(defn fetch-requested
  "general suggestion f"
  [tag-suggestions suggestions & {:keys [scope]}]
  (fn [value]
    (if-not (has-key? value)
      (find-keys value tag-suggestions :dedup-by :tag)
      (let [[key val] (clojure.string/split value #"=")]
        (if (empty? (find-keys key tag-suggestions :dedup-by :tag))
          []
          (find-vals key val suggestions))))))

(defn wrap-react [my-atom f]
  (fn [arg]
    (reset! my-atom (f (.-value arg)))))

(defn on-change-suggest
  ([value-atom] (on-change-suggest value-atom identity))
  ([value-atom f]
   (fn [e new-val]
     (let [callback (or f identity)]
       (callback (.-newValue new-val))
       (reset! value-atom (.-newValue new-val))))))

(defn get-suggestion-value [value-atom]
  (fn [arg]
    (let [val (aget arg "val")]
      (if-not (has-key? @value-atom)
        val
        (str (aget arg "tag") "=" val)))))

(defn render-suggestion [value-atom]
  (fn [arg]
    (let [val (aget arg "val")]
      (if (has-key? @value-atom)
        (reagent/as-element [:p val])
        (reagent/as-element [:p val])))))

(defn suggest-annotations
  [tagsets {:keys [value on-change on-key-press display-suggestions] :as props}]
  (let [sugg-atom (reagent/atom [])
        value-atom (or value (reagent/atom ""))]
    (fn [tagsets {:keys [on-change on-key-press] :as props}]
      (let [suggs (denormalize-tagsets tagsets)
            tag-suggs (tags-by suggs)]
        [:div.container-fluid
         [:div.row
          [autosuggest
           {:suggestions @sugg-atom
            :multiSection true
            :onSuggestionsUpdateRequested (wrap-react sugg-atom (fetch-requested tag-suggs suggs))
            :onSuggestionsClearRequested #(reset! sugg-atom [])
            :getSuggestionValue (get-suggestion-value value-atom)
            :renderSuggestion (render-suggestion value-atom)
            :getSectionSuggestions #(aget % "tags")
            :renderSectionTitle #(reagent/as-element [:strong (aget % "name")])
            :inputProps (merge (dissoc props :display-suggestions)
                               {:onChange (on-change-suggest value-atom on-change)
                                :value @value-atom})}]]]))))

(defn suggest-users [])
