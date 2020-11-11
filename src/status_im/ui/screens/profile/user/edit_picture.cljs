(ns status-im.ui.screens.profile.user.edit-picture
  (:require [quo.core :as quo]
            [clojure.string :as cstr]
            [re-frame.core :as re-frame]
            [status-im.i18n :as i18n]
            [status-im.multiaccounts.core :as multiaccounts]
            [status-im.ui.components.react :as react]))

(def crop-size 1000)
(def crop-opts {:cropping             true
                :cropperCircleOverlay true
                :width                crop-size
                :height               crop-size})

(defn clean-path [^js img]
  (when-let [path (.-path ^js img)]
    (cstr/replace-first path #"file://" "")))

(defn pick-pic []
  (react/show-image-picker
   #(re-frame/dispatch [::multiaccounts/save-profile-picture (clean-path %) 0 0 crop-size crop-size])
   crop-opts))

(defn take-pic []
  (react/show-image-picker-camera
   #(re-frame/dispatch [::multiaccounts/save-profile-picture (clean-path %) 0 0 crop-size crop-size])
   crop-opts))

(defn bottom-sheet [has-picture]
  (fn []
    [:<>
     [quo/list-item {:accessibility-label :take-photo
                     :theme               :accent
                     :icon                :main-icons/camera
                     :title               (i18n/label :t/profile-pic-take)
                     :on-press            take-pic}]
     [quo/list-item {:accessibility-label :pick-photo
                     :icon                :main-icons/gallery
                     :theme               :accent
                     :title               (i18n/label :t/profile-pic-pick)
                     :on-press            pick-pic}]
     (when has-picture
       [quo/list-item {:accessibility-label :remove-photo
                       :icon                :main-icons/delete
                       :theme               :accent
                       :title               (i18n/label :t/profile-pic-remove)
                       :on-press            #(re-frame/dispatch [::multiaccounts/delete-profile-picture nil])}])]))
