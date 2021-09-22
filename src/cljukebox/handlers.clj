(ns cljukebox.handlers
  (:require [clojure.string :as string]
            [cljukebox.handlers.prefix :as prefix]
            [cljukebox.util :as util]))

(def base-handlers
  {"prefix" prefix/handler-data})

(defn help-handler [!config !state {:keys [channel-id guild-id content] :as data}]
  (let [prefix (util/get-prefix !config guild-id)
        [command] (rest (string/split content #" "))]
    (if command
      (if-let [{:keys [doc usage-str]} (get base-handlers command)]
        (util/send-embed !state channel-id {:title command
                                            :description doc
                                            :fields [{:name "Usage Example"
                                                      :value (format "`%s`" usage-str)}]})
        (util/send-message !state channel-id (format "*%s* is not an existing command" command)))
      (util/send-embed !state channel-id {:title "Help Menu"
                                          :description "For usage examples of specific commands, use `help <command>`"
                                          :fields (mapv (fn [[k v]]
                                                          {:name k
                                                           :value (:doc v)})
                                                        base-handlers)}))))

(def handlers
  (assoc base-handlers "help" {:handler-fn help-handler}))

(defn get-handler-fn [content prefix]
  (some (fn [[k v]]
          (when (string/starts-with? content (str prefix k))
            (:handler-fn v))) handlers))
