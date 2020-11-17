(ns status-im.ui.screens.communities.views (:require-macros [status-im.utils.views :as views])
    (:require
     [reagent.core :as reagent]
     [re-frame.core :as re-frame]
     [quo.core :as quo]
     [status-im.i18n :as i18n]
     [status-im.utils.core :as utils]
     [status-im.utils.fx :as fx]
     [status-im.utils.config :as config]
     [status-im.constants :as constants]
     [status-im.communities.core :as communities]
     [status-im.ui.screens.home.views.inner-item :as inner-item]
     [status-im.ui.screens.home.styles :as home.styles]
     [status-im.ui.components.list.views :as list]
     [status-im.ui.components.copyable-text :as copyable-text]
     [status-im.react-native.resources :as resources]
     [status-im.ui.components.topbar :as topbar]
     [status-im.ui.components.icons.vector-icons :as icons]
     [status-im.ui.components.colors :as colors]
     [status-im.ui.components.chat-icon.screen :as chat-icon.screen]
     [status-im.ui.components.toolbar :as toolbar]
     [status-im.ui.components.bottom-sheet.core :as bottom-sheet]
     [status-im.ui.components.react :as react]))

(defn hide-sheet-and-dispatch [event]
  (re-frame/dispatch [:bottom-sheet/hide])
  (re-frame/dispatch event))

(defn community-list-item [{:keys [id description]}]
  (let [identity (:identity description)]
    [quo/list-item
     {:icon                       (if (= id constants/status-community-id)
                                    [react/image {:source (resources/get-image :status-logo-rainbow)
                                                  :style {:width 40
                                                          :height 40}}]

                                    [chat-icon.screen/chat-icon-view-chat-list
                                     id
                                     true
                                     (:display-name identity)
                                     ;; TODO: should be derived by id
                                     (or (:color identity)
                                         (rand-nth colors/chat-colors))
                                     false
                                     false])
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
     [topbar/topbar (cond-> {:title (i18n/label :t/communities)}
                      config/communities-enabled?
                      (assoc :right-accessories [{:icon                :main-icons/more
                                                  :accessibility-label :chat-menu-button
                                                  :on-press
                                                  #(re-frame/dispatch [:bottom-sheet/show-sheet
                                                                       {:content (fn []
                                                                                   [communities-actions])
                                                                        :height  256}])}]))]
     [react/scroll-view {:style                   {:flex 1}
                         :content-container-style {:padding-vertical 8}}
      [list/flat-list
       {:key-fn                       :id
        :keyboard-should-persist-taps :always
        :data                         (vals communities)
        :render-fn                    (fn [community] [community-list-item community])}]]
     (when config/communities-enabled?
       [toolbar/toolbar
        {:show-border? true
         :center [quo/button {:on-press #(re-frame/dispatch [::create-pressed])}
                  (i18n/label :t/create-a-community)]}])]))

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

       (when config/communities-enabled?
         [react/view {:style {:padding-top 20
                              :padding-horizontal 20}}
          [quo/button {:disabled  (not (valid? @channel-name @channel-description))
                       :on-press #(re-frame/dispatch [::create-channel-confirmation-pressed @channel-name @channel-description])}
           (i18n/label :t/create)]])])))

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
       :on-press            #(re-frame/dispatch [::invite-people-pressed id])}])
   [quo/list-item
    {:theme               :accent
     :title               (i18n/label :t/leave)
     :accessibility-label :leave
     :icon                :main-icons/close
     :on-press            #(do
                             (re-frame/dispatch [:navigate-to :home])
                             (re-frame/dispatch [:bottom-sheet/hide])
                             (re-frame/dispatch [::communities/leave id]))}]])

(defn toolbar-content [id display-name color]
  [react/view {:style  {:flex           1
                        :align-items    :center
                        :flex-direction :row}}
   [react/view {:margin-right 10}
    (if (= id constants/status-community-id)
      [react/image {:source (resources/get-image :status-logo-rainbow)
                    :style {:width 40
                            :height 40}}]
      [chat-icon.screen/chat-icon-view-toolbar
       id
       true
       display-name
       (or color
           (rand-nth colors/chat-colors))])]
   [react/view {:style {:flex 1 :justify-content :center}}
    [react/text {:style {:typography  :main-medium
                         :font-size   15
                         :line-height 22}
                 :number-of-lines     1
                 :accessibility-label :community-name-text}
     display-name]]])

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

(defn welcome-blank-page []
  [react/view {:style {:flex 1 :flex-direction :row :align-items :center :justify-content :center}}
   [react/i18n-text {:style home.styles/welcome-blank-text :key :welcome-blank-message}]])

(views/defview community-unviewed-count [id]
  (views/letsubs [unviewed-count [:communities/unviewed-count id]]
    (when-not (zero? unviewed-count)
      [react/view {:style               home.styles/public-unread
                   :accessibility-label :unviewed-messages-public}])))

(defn status-community [{:keys [id description]}]
  [quo/list-item
   {:icon                      [react/image {:source (resources/get-image :status-logo-rainbow)
                                             :style {:width 40
                                                     :height 40}}]
    :title                     [react/view {:flex-direction :row
                                            :flex           1}
                                [react/view {:flex-direction :row
                                             :flex           1
                                             :padding-right  16
                                             :align-items    :center}
                                 [quo/text {:weight              :medium
                                            :accessibility-label :chat-name-text
                                            :ellipsize-mode      :tail
                                            :number-of-lines     1}
                                  "Status"]]]
    :title-accessibility-label :chat-name-text
    :subtitle                  [react/view {:flex-direction :row}
                                [react/text-class {:style               home.styles/last-message-text
                                                   :number-of-lines     1
                                                   :ellipsize-mode      :tail
                                                   :accessibility-label :chat-message-text} (get-in description [:identity :description])]
                                [community-unviewed-count id]]
    :on-press                  #(do
                                  (re-frame/dispatch [:dismiss-keyboard])
                                  (re-frame/dispatch [:navigate-to :community id]))
    ;; TODO: actions
    :on-long-press             #(re-frame/dispatch [:bottom-sheet/show-sheet
                                                    nil])}])

(defn channel-preview-item [{:keys [id identity]}]
  [quo/list-item
   {:icon                      [chat-icon.screen/chat-icon-view-chat-list
                                id true (:display-name identity) colors/blue false false]
    :title                     [react/view {:flex-direction :row
                                            :flex           1}
                                [react/view {:flex-direction :row
                                             :flex           1
                                             :padding-right  16
                                             :align-items    :center}
                                 [icons/icon :main-icons/tiny-group
                                  {:color           colors/black
                                   :width           15
                                   :height          15
                                   :container-style {:width           15
                                                     :height          15
                                                     :margin-right   2}}]
                                 [quo/text {:weight              :medium
                                            :accessibility-label :chat-name-text
                                            :ellipsize-mode      :tail
                                            :number-of-lines     1}
                                  (utils/truncate-str (:display-name identity) 30)]]]
    :title-accessibility-label :chat-name-text
    :subtitle                  [react/view {:flex-direction :row}
                                [react/text-class {:style               home.styles/last-message-text
                                                   :number-of-lines     1
                                                   :ellipsize-mode      :tail
                                                   :accessibility-label :chat-message-text} (:description identity)]]}])

(defn community-channel-preview-list [_ description]
  (let [chats (reduce-kv
               (fn [acc k v]
                 (conj acc (assoc v :id (name k))))
               []
               (get-in description [:chats]))]
    [list/flat-list
     {:key-fn                       :id
      :keyboard-should-persist-taps :always
      :data                         chats
      :render-fn                    channel-preview-item}]))

(defn community-chat-list [chats]
  (if (empty? chats)
    [welcome-blank-page]
    [list/flat-list
     {:key-fn                       :chat-id
      :keyboard-should-persist-taps :always
      :data                         chats
      :render-fn                    (fn [home-item] [inner-item/home-list-item (assoc home-item :color colors/blue)])
      :footer                       [react/view {:height 68}]}]))

(views/defview community-channel-list [id]
  (views/letsubs [chats [:chats/by-community-id id]]
    [community-chat-list chats]))

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
       [community-channel-preview-list id description])
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

(defn- join-featured-community-pressed [community-id]
  (re-frame/dispatch [::communities/join community-id])
  (re-frame/dispatch [:set :public-group-topic nil]))

(defn render-featured-community [{:keys [name id]}]
  ^{:key id}
  [react/touchable-highlight {:on-press            #(join-featured-community-pressed id)
                              :accessibility-label :chat-item}
   [react/view {:padding-right 8 :padding-vertical 8}
    [react/view {:border-color colors/gray-lighter :border-radius 36 :border-width 1 :padding-horizontal 8 :padding-vertical 5}
     [react/text {:style {:color colors/blue :typography :main-medium}} name]]]])

