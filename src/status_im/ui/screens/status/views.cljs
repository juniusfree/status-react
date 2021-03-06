(ns status-im.ui.screens.status.views
  (:require [status-im.ui.screens.chat.message.message :as message]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.colors :as colors]
            [status-im.utils.datetime :as datetime]
            [status-im.ui.screens.chat.message.gap :as gap]
            [status-im.constants :as constants]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [status-im.ui.components.icons.vector-icons :as icons]
            [status-im.ui.components.list.views :as list]
            [status-im.i18n :as i18n]
            [status-im.ui.screens.status.styles :as styles]
            [status-im.ui.screens.chat.views :as chat.views]
            [status-im.ui.components.plus-button :as components.plus-button]
            [status-im.ui.screens.chat.image.preview.views :as preview]
            [status-im.ui.screens.chat.photos :as photos]
            [status-im.ui.components.tabs :as tabs]
            [status-im.utils.contenthash :as contenthash]
            [status-im.multiaccounts.core :as multiaccounts]
            [status-im.ui.screens.chat.message.reactions :as reactions]
            [status-im.chat.models.reactions :as models.reactions]
            [status-im.ui.screens.chat.components.reply :as components.reply]
            [status-im.ui.screens.chat.message.link-preview :as link-preview]
            [status-im.chat.models :as chat]))

(defonce messages-list-ref (atom nil))
(def image-max-dimension 260)

(defn message-content-image [uri _]
  (let [dimensions (reagent/atom [image-max-dimension image-max-dimension])]
    (react/image-get-size
     uri
     (fn [width height]
       (let [k (/ (max width height) image-max-dimension)]
         (reset! dimensions [(/ width k) (/ height k)]))))
    (fn [uri show-close?]
      [react/view
       [react/view {:style {:width         (first @dimensions)
                            :height        (last @dimensions)
                            :border-width  1
                            :border-color  colors/black-transparent
                            :overflow      :hidden
                            :border-radius 16
                            :margin-top    8}}
        [react/image {:style       {:width (first @dimensions) :height (last @dimensions)}
                      :resize-mode :contain
                      :source      {:uri uri}}]]
       (when show-close?
         [react/touchable-highlight {:on-press            #(re-frame/dispatch [:chat.ui/cancel-sending-image])
                                     :accessibility-label :cancel-send-image
                                     :style               {:left (- (first @dimensions) 28) :top 12 :position :absolute}}
          [react/view {:width            24
                       :height           24
                       :background-color colors/black-persist
                       :border-radius    12
                       :align-items      :center
                       :justify-content  :center}
           [icons/icon :main-icons/close-circle {:color colors/white-persist}]]])])))

(defn on-long-press-fn [on-long-press content image]
  (on-long-press
   (when-not image
     [{:on-press #(react/copy-to-clipboard
                   (components.reply/get-quoted-text-with-mentions
                    (get content :parsed-text)))
       :label    (i18n/label :t/sharing-copy-to-clipboard)}])))

(defn image-message []
  (let [visible (reagent/atom false)]
    (fn [{:keys [content] :as message} on-long-press]
      [:<>
       [preview/preview-image {:message   (assoc message :cant-be-replied true)
                               :visible   @visible
                               :can-reply false
                               :on-close  #(do (reset! visible false)
                                               (reagent/flush))}]
       [react/touchable-highlight {:on-press (fn [_]
                                               (reset! visible true)
                                               (react/dismiss-keyboard!))
                                   :on-long-press #(on-long-press-fn on-long-press content true)}
        [message-content-image (:image content) false]]])))

(defn message-item [timeline? account]
  (fn [{:keys [content-type content from timestamp identicon outgoing] :as message}
       {:keys [modal on-long-press close-modal]}]
    [react/view (merge {:padding-vertical   8
                        :flex-direction     :row
                        :background-color   (if (and timeline? outgoing) colors/blue-light colors/white)
                        :padding-horizontal 16}
                       (when modal
                         {:border-radius 16}))
     [react/touchable-highlight
      {:on-press #(do (when modal (close-modal))
                      (re-frame/dispatch [:chat.ui/show-profile-without-adding-contact from]))}
      [react/view {:padding-top 2 :padding-right 8}
       (if outgoing
         [photos/member-identicon (multiaccounts/displayed-photo account)]
         [photos/member-identicon identicon])]]
     [react/view {:flex 1}
      [react/view {:flex-direction  :row
                   :justify-content :space-between}
       [react/touchable-highlight
        {:on-press #(do (when modal (close-modal))
                        (re-frame/dispatch [:chat.ui/show-profile-without-adding-contact from]))}
        (if outgoing
          [message/message-my-name {:profile? true :you? false}]
          [message/message-author-name from {:profile? true}])]
       [react/text {:style {:font-size 10 :color colors/gray}}
        (datetime/time-ago (datetime/to-date timestamp))]]
      [react/view
       (if (= content-type constants/content-type-image)
         [image-message message on-long-press]
         [react/touchable-highlight (when-not modal
                                      {:on-long-press #(on-long-press-fn on-long-press content false)})
          [message/render-parsed-text (assoc message :outgoing false) (:parsed-text content)]])
       [link-preview/link-preview-wrapper (:links content) outgoing]]]]))

(defn render-message [timeline? account]
  (fn [{:keys [type] :as message} idx]
    [(fn []
       (if (= type :datemark)
         nil
         (if (= type :gap)
           (if timeline?
             nil
             [gap/gap message idx messages-list-ref])
           ; message content
           (let [chat-id (chat/profile-chat-topic (:from message))]
             [react/view (merge {:accessibility-label :chat-item}
                                (when (:last-in-group? message)
                                  {:padding-bottom      8
                                   :margin-bottom       8
                                   :border-bottom-width 1
                                   :border-bottom-color colors/gray-lighter}))
              [reactions/with-reaction-picker
               {:message         message
                :timeline        true
                :reactions       @(re-frame/subscribe [:chats/message-reactions (:message-id message)])
                :picker-on-open  (fn [])
                :picker-on-close (fn [])
                :send-emoji      (fn [{:keys [emoji-id]}]
                                   (re-frame/dispatch [::models.reactions/send-emoji-reaction
                                                       {:message-id (:message-id message)
                                                        :chat-id    chat-id
                                                        :emoji-id   emoji-id}]))
                :retract-emoji   (fn [{:keys [emoji-id emoji-reaction-id]}]
                                   (re-frame/dispatch [::models.reactions/send-emoji-reaction-retraction
                                                       {:message-id        (:message-id message)
                                                        :chat-id           chat-id
                                                        :emoji-id          emoji-id
                                                        :emoji-reaction-id emoji-reaction-id}]))
                :render          (message-item timeline? account)}]]))))]))

(def state (reagent/atom {:tab :timeline}))

(defn tabs []
  (let [{:keys [tab]} @state]
    [react/view {:flex-direction     :row
                 :padding-horizontal 4
                 :margin-top         8}
     [tabs/tab-title state :timeline (i18n/label :t/timeline) (= tab :timeline)]
     [tabs/tab-title state :status (i18n/label :t/my-status) (= tab :status)]]))

(def image-hash "e3010170122080c27fe972a95dbb4b0ead029d2c73d18610e849fac197e91068a918755e21b2")

(defn timeline []
  (let [messages @(re-frame/subscribe [:chats/timeline-messages-stream])
        no-messages? @(re-frame/subscribe [:chats/current-chat-no-messages?])
        account @(re-frame/subscribe [:multiaccount])]
    [react/view {:flex 1}
     ;;TODO implement in the next iteration
     #_[tabs]
     [react/view {:height           1
                  :background-color colors/gray-lighter}]
     (if no-messages?
       [react/view {:padding-horizontal 32
                    :margin-top         64}
        [react/image {:style  {:width      140
                               :height     140
                               :align-self :center}
                      :source {:uri (contenthash/url image-hash)}}]
        [react/view (styles/descr-container)
         [react/text {:style {:color       colors/gray
                              :line-height 22}}
          (if (= :timeline (:tab @state))
            (i18n/label :t/statuses-descr)
            (i18n/label :t/statuses-my-status-descr))]]]
       [list/flat-list
        {:key-fn                    #(or (:message-id %) (:value %))
         :render-fn                 (render-message (= :timeline (:tab @state)) account)
         :data                      messages
         :on-viewable-items-changed chat.views/on-viewable-items-changed
         :on-end-reached            #(re-frame/dispatch [:chat.ui/load-more-messages])
         ;;don't remove :on-scroll-to-index-failed
         :on-scroll-to-index-failed #()
         :header                    [react/view {:height 8}]
         :footer                    [react/view {:height 68}]}])
     [components.plus-button/plus-button
      {:on-press #(re-frame/dispatch [:navigate-to :my-status])}]]))
