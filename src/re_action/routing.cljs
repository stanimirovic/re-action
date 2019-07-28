(ns re-action.routing
  (:require [re-streamer.core :as re-streamer :refer [subscribe emit]]))

(defrecord Route [path page])
(defrecord Redirection [from to])

(defonce ^:private routes (atom #{}))
(defonce ^:private redirections (atom #{}))
(defonce ^:private not-found-redirection (atom nil))

(defonce ^:private router (re-streamer/stream))
(defonce ^:private active-page (re-streamer/map router :page))
(defonce router-outlet (:state active-page))

(subscribe router #(set! (.. js/window -location -hash) (:path %)))

(defn route [path page]
  (swap! routes conj (->Route path page)))

(defn redirect [from to]
  (if (= from "**")
    (reset! not-found-redirection (->Redirection from to))
    (swap! redirections conj (->Redirection from to))))

(defn- path->route [path]
  (->> @routes
       (filter #(= (:path %) path))
       (first)))

(defn navigate [path]
  (let [route (or (path->route path)
                  (->> @redirections
                       (filter #(= (:from %) path))
                       (map :to)
                       (first)
                       (path->route))
                  (path->route (:to @not-found-redirection))
                  (throw (js/Error (str "Route: " path " is not defined"))))]
    (emit router route)))

(defn start-routing []
  (navigate (.. js/window -location -hash)))
