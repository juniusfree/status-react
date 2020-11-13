(ns status-im.multiaccounts.core
  (:require [re-frame.core :as re-frame]
            [status-im.ethereum.stateofus :as stateofus]
            [status-im.multiaccounts.update.core :as multiaccounts.update]
            [status-im.ui.components.bottom-sheet.core :as bottom-sheet]
            [status-im.native-module.core :as native-module]
            [status-im.utils.fx :as fx]
            [status-im.utils.gfycat.core :as gfycat]
            [status-im.utils.identicon :as identicon]
            [status-im.utils.theme :as utils.theme]
            [status-im.utils.types :as types]
            [status-im.theme.core :as theme]
            [status-im.utils.utils :as utils]
            [quo.platform :as platform]
            [clojure.string :as string]))

(defn contact-names
  "Returns map of all existing names for contact"
  [{:keys [name preferred-name alias public-key ens-verified nickname]}]
  (let [ens-name (or preferred-name
                     name)]
    (cond-> {:nickname         nickname
             :three-words-name (or alias (gfycat/generate-gfy public-key))}
      ;; Preferred name is our own otherwise we make sure it's verified
      (or preferred-name (and ens-verified name))
      (assoc :ens-name (str "@" (or (stateofus/username ens-name) ens-name))))))

(defn contact-two-names
  "Returns vector of two names in next order nickname, ens name, three word name, public key"
  [{:keys [names public-key] :as contact} public-key?]
  (let [{:keys [nickname ens-name three-words-name]} (or names (contact-names contact))
        short-public-key (when public-key? (utils/get-shortened-address public-key))]
    (cond (not (string/blank? nickname))
          [nickname (or ens-name three-words-name short-public-key)]
          (not (string/blank? ens-name))
          [ens-name (or three-words-name short-public-key)]
          (not (string/blank? three-words-name))
          [three-words-name short-public-key]
          :else
          (when public-key?
            [short-public-key short-public-key]))))

(defn contact-with-names
  "Returns contact with :names map "
  [contact]
  (assoc contact :names (contact-names contact)))

(defn displayed-name
  "Use preferred name, name or alias in that order"
  [{:keys [name preferred-name alias public-key ens-verified]}]
  (let [ens-name (or preferred-name
                     name)]
    ;; Preferred name is our own otherwise we make sure it's verified
    (if (or preferred-name (and ens-verified name))
      (let [username (stateofus/username ens-name)]
        (str "@" (or username ens-name)))
      (or alias (gfycat/generate-gfy public-key)))))

(defn displayed-photo
  "If a photo-path is set use it, otherwise fallback on identicon or generate"
  [{:keys [profile-picture identicon public-key]}]
  (or (get profile-picture :url)        ; Optimistic update, show local image while uploading
      (get profile-picture :uri)
      identicon
      (identicon/identicon public-key)))

(re-frame/reg-fx
 ::chaos-mode-changed
 (fn [on]
   (native-module/chaos-mode-update on (constantly nil))))

(re-frame/reg-fx
 ::webview-debug-changed
 (fn [value]
   (when platform/android?
     (native-module/toggle-webview-debug value))))

(re-frame/reg-fx
 ::blank-preview-flag-changed
 (fn [flag]
   (native-module/set-blank-preview-flag flag)))

(fx/defn confirm-wallet-set-up
  [cofx]
  (multiaccounts.update/multiaccount-update cofx
                                            :wallet-set-up-passed? true {}))

(fx/defn confirm-home-tooltip
  [cofx]
  (multiaccounts.update/multiaccount-update cofx
                                            :hide-home-tooltip? true {}))

(fx/defn switch-dev-mode
  [cofx dev-mode?]
  (multiaccounts.update/multiaccount-update cofx
                                            :dev-mode? dev-mode?
                                            {}))

(fx/defn switch-webview-debug
  {:events [:multiaccounts.ui/switch-webview-debug]}
  [{:keys [db] :as cofx} value]
  (fx/merge cofx
            {::webview-debug-changed value}
            (multiaccounts.update/multiaccount-update
             :webview-debug (boolean value)
             {})))

(fx/defn switch-chaos-mode
  [{:keys [db] :as cofx} chaos-mode?]
  (when (:multiaccount db)
    (fx/merge cofx
              {::chaos-mode-changed chaos-mode?}
              (multiaccounts.update/multiaccount-update
               :chaos-mode? (boolean chaos-mode?)
               {}))))

(fx/defn switch-preview-privacy-mode
  [{:keys [db] :as cofx} private?]
  (fx/merge cofx
            {::blank-preview-flag-changed private?}
            (multiaccounts.update/multiaccount-update
             :preview-privacy? (boolean private?)
             {})))

(fx/defn switch-webview-permission-requests?
  [{:keys [db] :as cofx} enabled?]
  (multiaccounts.update/multiaccount-update
   cofx
   :webview-allow-permission-requests? (boolean enabled?)
   {}))

(fx/defn switch-preview-privacy-mode-flag
  [{:keys [db]}]
  (let [private? (get-in db [:multiaccount :preview-privacy?])]
    {::blank-preview-flag-changed private?}))

(re-frame/reg-fx
 ::switch-theme
 (fn [theme-id]
   (let [theme (if (or (= 2 theme-id) (and (= 0 theme-id) (utils.theme/is-dark-mode)))
                 :dark
                 :light)]
     (theme/change-theme theme))))

(fx/defn switch-appearance
  {:events [:multiaccounts.ui/appearance-switched]}
  [cofx theme]
  (fx/merge cofx
            {::switch-theme theme}
            (multiaccounts.update/multiaccount-update :appearance theme {})))

(re-frame/reg-fx
 ::save-profile-picture
 (fn [[path ax ay bx by]]
   (native-module/save-profile-image path ax ay bx by
                                     #(re-frame/dispatch [::update-local-picture %]))))

(re-frame/reg-fx
 ::delete-profile-picture
 (fn [name]
   (native-module/delete-profile-image name println)))

(fx/defn save-profile-picture
  {:events [::save-profile-picture]}
  [cofx path ax ay bx by]
  (fx/merge cofx
            {::save-profile-picture [path ax ay bx by]}
            (multiaccounts.update/optimistic :profile-picture {:url path})
            (bottom-sheet/hide-bottom-sheet)))

(fx/defn delete-profile-picture
  {:events [::delete-profile-picture]}
  [cofx name]
  (fx/merge cofx
            {::delete-profile-picture name}
            (multiaccounts.update/optimistic :profile-picture {})
            (bottom-sheet/hide-bottom-sheet)))

(fx/defn store-profile-picture
  {:events [::update-local-picture]}
  [cofx pics]
  (multiaccounts.update/optimistic cofx :profile-picture (first (types/json->clj pics))))
