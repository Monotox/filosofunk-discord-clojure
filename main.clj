(require '[discljord.connections :as c])
(require '[discljord.messaging :as m])
(require '[discljord.events :as e])
(require '[clojure.core.async :as a])
(require '[clj-http.client :as client])
(require '[cheshire.core :refer :all])

(def token "token")

(defn format-message
  [v]
  (str "\"" (get v "estrofe") "\" -" (get v "poeta")))

(def generate-message
  (format-message
    (rand-nth
      (parse-string
        (:body
          (client/get "https://raw.githubusercontent.com/IgorRozani/filosofunk/master/poesias.json"))))))

(def state (atom nil))

(defn greet-or-disconnect
  [event-type {{bot :bot} :author :keys [channel-id content]}]
  (if (= content "!disconnect")
    (a/put! (:connection @state) [:disconnect])
    (when-not bot
      (m/create-message! (:messaging @state) channel-id :content gerar-mensagem))))

(defn send-emoji
  [event-type {:keys [channel-id emoji]}]
  (when (:name emoji)
    (m/create-message! (:messaging @state) channel-id
                       :content (if (:id emoji)
                                  (str "<:" (:name emoji) ":" (:id emoji) ">")
                                  (:name emoji)))))

(def handlers
  {:message-create       [#'greet-or-disconnect]
   :message-reaction-add [#'send-emoji]})

(let [event-ch (a/chan 100)
      connection-ch (c/connect-bot! token event-ch)
      messaging-ch (m/start-connection! token)
      init-state {:connection connection-ch
                  :event      event-ch
                  :messaging  messaging-ch}]
  (reset! state init-state)
  (try (e/message-pump! event-ch (partial e/dispatch-handlers #'handlers))
       (finally
         (m/stop-connection! messaging-ch)
         (c/disconnect-bot! connection-ch))))
