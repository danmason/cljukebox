(ns cljukebox.core
  (:require [clojure.string :as string]
            [clojure.core.async :refer [chan close!]]
            [discljord.messaging :as discord-rest]
            [discljord.connections :as discord-ws]
            [discljord.formatting :refer [mention-user]]
            [discljord.events :refer [message-pump!]]
            [cljukebox.util :as util]
            [cljukebox.handlers :as handlers]))

(def !state (atom nil))
(def !bot-id (atom nil))
(def !config (atom (util/read-config)))

(defmulti handle-event (fn [type _data] type))

(defmethod handle-event :message-create
  [_ {:keys [guild-id content] :as data}]
  (let [prefix (util/get-prefix @!config guild-id)
        handler-fn (handlers/get-handler-fn content prefix)]
    (when handler-fn (handler-fn !config !state data))))

(defmethod handle-event :ready
  [_ _]
  (discord-ws/status-update! (:gateway @!state) :activity (discord-ws/create-activity :name "Type '^help' for a list of commands")))

(defmethod handle-event :default [_ _])

(defn start-bot! [token & intents]
  (let [event-channel (chan 100)
        gateway-connection (discord-ws/connect-bot! token event-channel :intents (set intents))
        rest-connection (discord-rest/start-connection! token)]
    {:events event-channel
     :gateway gateway-connection
     :rest rest-connection}))

(defn stop-bot! [{:keys [rest gateway events] :as _state}]
  (discord-rest/stop-connection! rest)
  (discord-ws/disconnect-bot! gateway)
  (close! events))

(defn -main [& [api-token]]
  (when api-token
    (util/merge-to-config !config {:token api-token}))
  (if-let [token (:token @!config)]
    (do
      (reset! !state (start-bot! token :guild-messages))
      (reset! !bot-id (:id @(discord-rest/get-current-user! (:rest @!state))))
      (try
        (message-pump! (:events @!state) handle-event)
        (finally (stop-bot! @!state))))
    (println "API token not configured - pass as an argument when starting the bot.")))
