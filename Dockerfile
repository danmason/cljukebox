FROM clojure:openjdk-11-tools-deps

WORKDIR /usr/lib/cljukebox
VOLUME /var/lib/cljukebox
ENTRYPOINT ["java", "-jar", "cljukebox.jar"]

ADD ./target/cljukebox.jar cljukebox.jar
