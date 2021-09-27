(defproject cljukebox "0.0.1"
  :description "A clojure based self-hosted music bot for Discord"
  :url "https://github.com/danmason/cljukebox"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.discord4j/discord4j-core "3.1.7"]
                 [com.sedmelluq/lavaplayer "1.3.77" ]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [medley "1.3.0"]]
  :repl-options {:init-ns cljukebox.core}
  :repositories [["m2-dv8tion" "https://m2.dv8tion.net/releases"]]
  :java-source-paths ["java-src"]
  :jvm-opts ["-Dlogback.configurationFile=resources/logback.xml"]
  :uberjar-name "cljukebox.jar"
  :main cljukebox.core)
