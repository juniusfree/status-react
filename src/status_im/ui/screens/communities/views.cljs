(ns status-im.ui.screens.communities.views (:require-macros [status-im.utils.views :as views])
    (:require
     [reagent.core :as reagent]
     [re-frame.core :as re-frame]
     [quo.core :as quo]
     [status-im.i18n :as i18n]
     [status-im.utils.core :as utils]
     [status-im.utils.fx :as fx]
     [status-im.communities.core :as communities]
     [status-im.ui.screens.home.views :as home.views]
     [status-im.ui.components.list.views :as list]
     [status-im.ui.components.copyable-text :as copyable-text]
     [status-im.ui.components.topbar :as topbar]
     [status-im.ui.components.colors :as colors]
     [status-im.ui.components.chat-icon.screen :as chat-icon.screen]
     [status-im.ui.components.toolbar :as toolbar]
     [status-im.ui.components.bottom-sheet.core :as bottom-sheet]
     [status-im.ui.components.react :as react]))

(defn hide-sheet-and-dispatch [event]
  (re-frame/dispatch [:bottom-sheet/hide])
  (re-frame/dispatch event))

(defn community-channel-preview-list-item [{:keys [id identity]}]
  [quo/list-item
   {:icon                      [chat-icon.screen/chat-icon-view-chat-list
                                id
                                true
                                (:display-name identity)
                                ;; TODO: should be derived by id
                                (or (:color identity)
                                    (rand-nth colors/chat-colors))
                                false
                                false]
    :title                     [react/view {:flex-direction :row
                                            :flex           1}
                                [react/view {:flex-direction :row
                                             :flex           1
                                             :padding-right  16
                                             :align-items    :center}
                                 [quo/text {:weight              :medium
                                            :accessibility-label :community-name-text
                                            :ellipsize-mode      :tail
                                            :number-of-lines     1}
                                  (utils/truncate-str (:display-name identity) 30)]]]
    :title-accessibility-label :community-name-text
    :subtitle                  [react/view {:flex-direction :row}
                                [react/view {:flex 1}
                                 [quo/text
                                  (utils/truncate-str (:description identity) 30)]]]}])

(defn community-list-item [{:keys [id description]}]
  (let [identity (:identity description)]
    [quo/list-item
     {:icon                      [chat-icon.screen/chat-icon-view-chat-list
                                  id
                                  true
                                  (:display-name identity)
                                  ;; TODO: should be derived by id
                                  (or (:color identity)
                                      (rand-nth colors/chat-colors))
                                  false
                                  false]
      :title                     [react/view {:flex-direction :row
                                              :flex           1}
                                  [react/view {:flex-direction :row
                                               :flex           1
                                               :padding-right  16
                                               :align-items    :center}
                                   [quo/text {:weight              :medium
                                              :accessibility-label :community-name-text
                                              :ellipsize-mode      :tail
                                              :number-of-lines     1}
                                    (utils/truncate-str (:display-name identity) 30)]]]
      :title-accessibility-label :community-name-text
      :subtitle                  [react/view {:flex-direction :row}
                                  [react/view {:flex 1}
                                   [quo/text
                                    (utils/truncate-str (:description identity) 30)]]]
      :on-press                  #(do
                                    (re-frame/dispatch [:dismiss-keyboard])
                                    (re-frame/dispatch [:navigate-to :community id]))}]))

(defn communities-actions []
  [react/view
   [quo/list-item
    {:theme               :accent
     :title               (i18n/label :t/import-community)
     :accessibility-label :community-import-community
     :icon                :main-icons/check
     :on-press            #(hide-sheet-and-dispatch [::import-pressed])}]
   [quo/list-item
    {:theme               :accent
     :title               (i18n/label :t/create-community)
     :accessibility-label :community-create-community
     :icon                :main-icons/check
     :on-press            #(hide-sheet-and-dispatch [::create-pressed])}]])

(views/defview communities []
  (views/letsubs [communities [:communities]]
    [react/view {:flex 1}
     [topbar/topbar {:title (i18n/label :t/communities)
                     :right-accessories [{:icon                :main-icons/more
                                          :accessibility-label :chat-menu-button
                                          :on-press
                                          #(re-frame/dispatch [:bottom-sheet/show-sheet
                                                               {:content (fn []
                                                                           [communities-actions])
                                                                :height  256}])}]}]
     [react/scroll-view {:style                   {:flex 1}
                         :content-container-style {:padding-vertical 8}}
      [list/flat-list
       {:key-fn                       :id
        :keyboard-should-persist-taps :always
        :data                         (vals communities)
        :render-fn                    (fn [community] [community-list-item community])}]]
     [toolbar/toolbar
      {:show-border? true
       :center [quo/button {:on-press #(re-frame/dispatch [::create-pressed])}
                (i18n/label :t/create-a-community)]}]]))

(fx/defn import-pressed
  {:events [::import-pressed]}
  [cofx]
  (bottom-sheet/show-bottom-sheet cofx {:view :import-community}))

(fx/defn create-pressed
  {:events [::create-pressed]}
  [cofx]
  (bottom-sheet/show-bottom-sheet cofx {:view :create-community}))

;; TODO: that's probably a better way to do this
(defonce community-id (atom nil))

(fx/defn invite-people-pressed
  {:events [::invite-people-pressed]}
  [cofx id]
  (reset! community-id id)
  (bottom-sheet/show-bottom-sheet cofx {:view :invite-people-community}))

(fx/defn create-channel-pressed
  {:events [::create-channel-pressed]}
  [cofx id]
  (reset! community-id id)
  (bottom-sheet/show-bottom-sheet cofx {:view :create-community-channel}))

(fx/defn community-created
  {:events [::community-created]}
  [cofx response]
  (fx/merge cofx
            (bottom-sheet/hide-bottom-sheet)
            (communities/handle-response response)))

(fx/defn community-imported
  {:events [::community-imported]}
  [cofx response]
  (fx/merge cofx
            (bottom-sheet/hide-bottom-sheet)
            (communities/handle-response response)))

(fx/defn people-invited
  {:events [::people-invited]}
  [cofx response]
  (fx/merge cofx
            (bottom-sheet/hide-bottom-sheet)
            (communities/handle-response response)))

(fx/defn community-channel-created
  {:events [::community-channel-created]}
  [cofx response]
  (fx/merge cofx
            (bottom-sheet/hide-bottom-sheet)
            (communities/handle-response response)))

(fx/defn import-confirmation-pressed
  {:events [::import-confirmation-pressed]}
  [cofx community-key]
  (communities/import-community
   cofx
   community-key
   #(re-frame/dispatch [::community-imported %])))

(fx/defn create-confirmation-pressed
  {:events [::create-confirmation-pressed]}
  [cofx community-name community-description membership]
  (communities/create
   cofx
   community-name
   community-description
   membership
   ::community-created
   ::failed-to-create-community))

(fx/defn create-channel-confirmation-pressed
  {:events [::create-channel-confirmation-pressed]}
  [cofx community-channel-name community-channel-description]
  (communities/create-channel
   @community-id
   community-channel-name
   community-channel-description
   ::community-channel-created
   ::failed-to-create-community-channel))

(fx/defn invite-people-confirmation-pressed
  {:events [::invite-people-confirmation-pressed]}
  [cofx user-pk]
  (communities/invite-user
   cofx
   @community-id
   user-pk
   ::people-invited
   ::failed-to-invite-people))

(defn valid? [community-name community-description]
  (and (not= "" community-name)
       (not= "" community-description)))

(defn import-community []
  (let [community-key (reagent/atom "")]
    (fn []
      [react/view {:style {:padding-left    16
                           :padding-right   8}}
       [react/view {:style {:padding-horizontal 20}}
        [quo/text-input
         {:label          (i18n/label :t/community-key)
          :placeholder    (i18n/label :t/community-key-placeholder)
          :on-change-text #(reset! community-key %)
          :auto-focus     true}]]
       [react/view {:style {:padding-top 20
                            :padding-horizontal 20}}
        [quo/button {:disabled  (= @community-key "")
                     :on-press #(re-frame/dispatch [::import-confirmation-pressed @community-key])}
         (i18n/label :t/import)]]])))

(defn create []
  (let [community-name (reagent/atom "")
        membership  (reagent/atom 1)
        community-description (reagent/atom "")]
    (fn []
      [react/view {:style {:padding-left    16
                           :padding-right   8}}
       [react/view {:style {:padding-horizontal 20}}
        [quo/text-input
         {:label          (i18n/label :t/name-your-community)
          :placeholder    (i18n/label :t/name-your-community-placeholder)
          :on-change-text #(reset! community-name %)
          :auto-focus     true}]]
       [react/view {:style {:padding-horizontal 20}}
        [quo/text-input
         {:label           (i18n/label :t/give-a-short-description-community)
          :placeholder     (i18n/label :t/give-a-short-description-community)
          :multiline       true
          :number-of-lines 4
          :on-change-text  #(reset! community-description %)}]]
       [react/view {:style {:padding-horizontal 20}}
        [quo/text-input
         {:label           (i18n/label :t/membership-type)
          :placeholder     (i18n/label :t/membership-type-placeholder)
          :on-change-text  #(reset! membership %)}]]

       [react/view {:style {:padding-top 20
                            :padding-horizontal 20}}
        [quo/button {:disabled  (not (valid? @community-name @community-description))
                     :on-press #(re-frame/dispatch [::create-confirmation-pressed @community-name @community-description @membership])}
         (i18n/label :t/create)]]])))

(def create-sheet
  {:content create})

(def import-sheet
  {:content import-community})

(defn create-channel []
  (let [channel-name (reagent/atom "")
        channel-description (reagent/atom "")]
    (fn []
      [react/view {:style {:padding-left    16
                           :padding-right   8}}
       [react/view {:style {:padding-horizontal 20}}
        [quo/text-input
         {:label          (i18n/label :t/name-your-channel)
          :placeholder    (i18n/label :t/name-your-channel-placeholder)
          :on-change-text #(reset! channel-name %)
          :auto-focus     true}]]
       [react/view {:style {:padding-horizontal 20}}
        [quo/text-input
         {:label           (i18n/label :t/give-a-short-description-channel)
          :placeholder     (i18n/label :t/give-a-short-description-channel)
          :multiline       true
          :number-of-lines 4
          :on-change-text  #(reset! channel-description %)}]]

       [react/view {:style {:padding-top 20
                            :padding-horizontal 20}}
        [quo/button {:disabled  (not (valid? @channel-name @channel-description))
                     :on-press #(re-frame/dispatch [::create-channel-confirmation-pressed @channel-name @channel-description])}
         (i18n/label :t/create)]]])))

(def create-channel-sheet
  {:content create-channel})

(defn invite-people []
  (let [user-pk (reagent/atom "")]
    (fn []
      [react/view {:style {:padding-left    16
                           :padding-right   8}}
       [react/view {:style {:padding-horizontal 20}}
        [quo/text-input
         {:label          (i18n/label :t/enter-user-pk)
          :placeholder    (i18n/label :t/enter-user-pk)
          :on-change-text #(reset! user-pk %)
          :auto-focus     true}]]
       [react/view {:style {:padding-top 20
                            :padding-horizontal 20}}
        [quo/button {:disabled  (= "" user-pk)
                     :on-press #(re-frame/dispatch [::invite-people-confirmation-pressed @user-pk])}
         (i18n/label :t/invite)]]])))

(def invite-people-sheet
  {:content invite-people})

(fx/defn handle-export-pressed
  {:events [::export-pressed]}
  [cofx community-id]
  (communities/export cofx community-id
                      #(re-frame/dispatch [:show-popover {:view  :export-community
                                                          :community-key %}])))
(defn community-actions [id admin]
  [react/view
   (when admin
     [quo/list-item
      {:theme               :accent
       :title               (i18n/label :t/export-key)
       :accessibility-label :community-export-key
       :icon                :main-icons/check
       :on-press            #(hide-sheet-and-dispatch [::export-pressed id])}])
   (when admin
     [quo/list-item
      {:theme               :accent
       :title               (i18n/label :t/create-channel)
       :accessibility-label :community-create-channel
       :icon                :main-icons/check
       :on-press            #(hide-sheet-and-dispatch [::create-channel-pressed id])}])
   (when admin
     [quo/list-item
      {:theme               :accent
       :title               (i18n/label :t/invite-people)
       :accessibility-label :community-invite-people
       :icon                :main-icons/close
       :on-press            #(re-frame/dispatch [::invite-people-pressed id])}])])

(defn toolbar-content [id display-name color]
  [react/view {:style  {:flex           1
                        :align-items    :center
                        :flex-direction :row}}
   [react/view {:margin-right 10}
    [chat-icon.screen/chat-icon-view-toolbar
     id
     true
     display-name
     (or color
         (rand-nth colors/chat-colors))]]])

(defn topbar [id display-name color admin]
  [topbar/topbar
   {:content           [toolbar-content id display-name color]
    :navigation        {:on-press #(re-frame/dispatch [:navigate-to :home])}
    :right-accessories [{:icon                :main-icons/more
                         :accessibility-label :community-menu-button
                         :on-press
                         #(re-frame/dispatch [:bottom-sheet/show-sheet
                                              {:content (fn []
                                                          [community-actions id admin])
                                               :height  256}])}]}])

(views/defview community-channel-list [id]
  (views/letsubs [chats [:chats/by-community-id id]]
    [home.views/chats-list-2 chats false nil true]))

(defn community-channel-preview-list [_ chats]
  [react/view {:flex 1}
   [list/flat-list
    {:key-fn                       :chat-id
     :keyboard-should-persist-taps :always
     :data                         chats
     :render-fn                    (fn [chat] [community-channel-preview-list-item chat])}]])

(views/defview community [route]
  (views/letsubs [{:keys [id description joined admin]} [:communities/community (get-in route [:route :params])]]
    [react/view {:style {:flex 1}}
     [topbar
      id
      (get-in description [:identity :display-name])
      (get-in description [:identity :color])
      admin]
     (if joined
       [community-channel-list id]
       [community-channel-preview-list id (map (fn [[k v]] (assoc v :id k)) (:chats description))])
     (when-not joined
       [react/view {:style {:padding-top 20
                            :padding-horizontal 20}}
        [quo/button {:on-press #(re-frame/dispatch [::communities/join id])}
         (i18n/label :t/join)]])]))

(views/defview export-community []
  (views/letsubs [{:keys [community-key]}     [:popover/popover]]
    [react/view {}
     [react/view {:style {:padding-top 16 :padding-horizontal 16}}
      [copyable-text/copyable-text-view
       {:label           :t/community-key
        :container-style {:margin-top 12 :margin-bottom 4}
        :copied-text     community-key}
       [quo/text {:number-of-lines     1
                  :ellipsize-mode      :middle
                  :accessibility-label :chat-key
                  :monospace           true}
        community-key]]]]))

