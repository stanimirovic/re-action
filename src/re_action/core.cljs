(ns re-action.core
  (:require [re-streamer.core :as re-streamer :refer [subscribe emit]]))

(defn store [state]
  (re-streamer/behavior-stream state))

(defn select [store key & keys]
  (re-streamer/distinct
    (if (nil? keys)
      (re-streamer/map store key)
      (re-streamer/pluck store (conj keys key)))
    =))

(defn update-state! [store state]
  (emit store state))

(defn patch-state! [store partial-state]
  (update-state! store (into @(:state store) partial-state)))
