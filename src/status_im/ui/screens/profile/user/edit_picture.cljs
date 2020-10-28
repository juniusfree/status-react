(ns status-im.ui.screens.profile.user.edit-picture
  (:require [quo.core :as quo]
            [status-im.i18n :as i18n]
            [status-im.ui.components.react :as react]))

(def crop-size 1000)
(def crop-opts {:cropping             true
                :cropperCircleOverlay true
                :width                crop-size
                :height               crop-size})

(defn upload-pic [img]
  (prn img))

(defn pick-pic []
  (react/show-image-picker upload-pic crop-opts))

(defn take-pic []
  (react/show-image-picker-camera upload-pic crop-opts))

(defn remove-pic [])

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
                       :on-press            remove-pic}])]))
