(ns cljukebox.util
  (:require [clojure.edn :as edn]
            [discljord.messaging :as discord-rest]
            [clojure.java.io :as io]
            [medley.core :as medley]))

(defn read-config []
  (let [config-file (io/file "config.edn")]
    (if (.exists config-file)
      (edn/read-string (slurp config-file))
      {:token nil
       :default-prefix "^"})))

(defn merge-to-config [!config m]
  (let [current-config @!config
        updated-config (medley/deep-merge current-config m)]
    (reset! !config updated-config)
    (spit "config.edn" updated-config)))

(defn get-prefix [!config guild-id]
  (let [{:keys [default-prefix] :as config-map} @!config]
    (or (get-in config-map [guild-id :prefix]) default-prefix)))

(defn send-message
  ([!state channel-id content]
   (discord-rest/create-message! (:rest @!state) channel-id :content content)))

(defn send-embed
  ([!state channel-id embed]
   (discord-rest/create-message! (:rest @!state) channel-id :embed embed)))
