(ns re-action.router
  (:require [re-streamer.core :refer [subscribe]]
            [re-action.core :refer [store select patch-state!]]
            [clojure.string :as string]))

(defrecord Router [current-route routes redirections not-found-redirection])
(defrecord Route [segments page])
(defrecord Redirection [from to])

(defonce ^:private router (store (->Router nil #{} #{} nil)))

(defonce ^:private current-route (select router :current-route))
(defonce ^:private current-page (select current-route :page))
(defonce ^:private current-segments (select current-route :segments))

(defonce ^:private routes (select router :routes))
(defonce ^:private redirections (select router :redirections))
(defonce ^:private not-found-redirection (select router :not-found-redirection))

(defonce outlet (:state current-page))

(defn- current-path [] (.. js/window -location -hash))

(defn- set-current-path [path] (set! (.. js/window -location -hash) path))

(defn- remove-hashes [path]
  (string/replace path "#" ""))

(defn- segments->path [segments]
  (string/join "/" segments))

(defn- path->segments [path]
  (filter (comp not empty?) (string/split (remove-hashes path) #"/")))

(defn- segments->route [segments]
  (->> @(:state routes)
       (filter #(= (:segments %) segments))
       (first)))

(defn defroute [path page]
  (patch-state! router {:routes (conj @(:state routes) (->Route (path->segments path) page))}))

(defn redirect [from-path to-path]
  (let [from (path->segments from-path)
        to (path->segments to-path)]
    (if (= from ["**"])
      (patch-state! router {:not-found-redirection (->Redirection from to)})
      (patch-state! router {:redirections (conj @(:state redirections) (->Redirection from to))}))))

(defn navigate [path]
  (let [segments (path->segments path)
        route (or (segments->route segments)
                  (->> @(:state redirections)
                       (filter #(= (:from %) segments))
                       (map :to)
                       (first)
                       (segments->route))
                  (segments->route (:to @(:state not-found-redirection)))
                  (throw (js/Error (str "Route: " segments " is not defined"))))]
    (patch-state! router {:current-route route})))

(defn start []
  (navigate (current-path))
  (subscribe current-segments #(set-current-path (segments->path %)))
  (set! (.-onhashchange js/window) (fn []
                                     (let [path (current-path)]
                                       (if (not (= @(:state current-segments) (path->segments path)))
                                         (navigate path))))))