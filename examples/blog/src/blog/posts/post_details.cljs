(ns blog.posts.post-details
  (:require [blog.posts.resource :as resource]
            [re-action.core :as re-action]
            [re-action.router :as router]
            [re-streamer.core :as re-streamer :refer [subscribe]]))

;; === Presentational Components ===

(defn header [edit back]
  [:div.card-header.page-header
   [:div.page-title [:h5 "Post Details"]]
   [:div.page-actions
    [:span.action-button.mr-1 {:on-click #(edit)} [:i.fas.fa-pen]]
    [:span.action-button {:on-click #(back)} [:i.fas.fa-chevron-left]]]])

(defn body [post]
  [:div.card-body
   [:div.form-group
    [:label "Title"]
    [:div.card-text (:title post)]]
   [:hr]
   [:div.form-group
    [:label "Body"]
    [:div.card-text.text-justify (:body post)]]])

;; === Facade ===

(defn- facade []
  (let [init-state {:post-id nil :post nil}
        store (re-action/store init-state)
        post (re-action/select store :post)
        post-id (-> store
                    (re-action/select-distinct :post-id)
                    (re-streamer/skip 1))]

    (subscribe post-id (fn [post-id]
                         (let [post (resource/get-post (js/parseInt post-id))]
                           (if (nil? post)
                             (router/navigate "/not-found")
                             (re-action/patch-state! store {:post post})))))

    {:post           (:state post)
     :update-post-id #(re-action/patch-state! store {:post-id %})
     :edit           #(router/navigate (str "/posts/" @(:state post-id) "/edit"))
     :back           #(router/navigate "/posts")}))

;; === Container Component ===

(defn container []
  (let [facade (facade)]
    (fn [id]
      ((:update-post-id facade) id)
      [:div.row.justify-content-center
       [:div.col-md-9
        [:div.card
         [header (:edit facade) (:back facade)]
         [body @(:post facade)]]]])))
