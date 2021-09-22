(ns cljukebox.handlers
  (:require [clojure.string :as string]
            [cljukebox.handlers.prefix :as prefix]))

(def handlers
  {"prefix" prefix/prefix-data})

(defn get-handler-fn [content prefix]
  (some (fn [[k v]]
          (when (string/starts-with? content (str prefix k))
            (:handler-fn v))) handlers))
