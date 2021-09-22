(ns cljukebox.core
  (:require [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.core.async :refer [chan close!]]
            [discljord.messaging :as discord-rest]
            [discljord.connections :as discord-ws]
            [discljord.formatting :refer [mention-user]]
            [discljord.events :refer [message-pump!]]
            [clojure.java.io :as io]))

(def !state (atom nil))

(def !bot-id (atom nil))

(def default-config )

(defn read-config []
  (let [config-file (io/file "config.edn")]
    (if (.exists config-file)
      (edn/read-string (slurp config-file))
      {:token nil
       :default-prefix "^"})))

(def !config (atom (read-config)))

(defn merge-to-config [m]
  (let [current-config @!config
        updated-config (merge current-config m)]
    (reset! !config updated-config)
    (spit "config.edn" updated-config)))

(defmulti handle-event (fn [type _data] type))

(defmethod handle-event :message-create
  [_ {:keys [channel-id content] :as data}]
  (let [{:keys [default-prefix] :as config-map} @!config
        prefix (or (get-in config-map [channel-id :prefix]) default-prefix)
        has-command-str? (fn [command-str] (string/includes? content (str prefix command-str)))]
    (cond
      (has-command-str? "test") (discord-rest/create-message! (:rest @!state) channel-id :content "Testing!"))))

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
    (merge-to-config {:token api-token}))
  (if-let [token (:token @!config)]
    (do
      (reset! !state (start-bot! token :guild-messages))
      (reset! !bot-id (:id @(discord-rest/get-current-user! (:rest @!state))))
      (try
        (message-pump! (:events @!state) handle-event)
        (finally (stop-bot! @!state))))
    (println "API token not configured - pass as an argument when starting the bot.")))
