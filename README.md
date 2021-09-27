# cljukebox
A clojure based self-hosted music bot for Discord - built using [**Discord4J**](https://github.com/Discord4J/Discord4J) and [**lavaplayer**](https://github.com/sedmelluq/lavaplayer). 

## Running the Bot

To run the bot, you will require an API token for a bot user - see the [discord.py docs](https://discordpy.readthedocs.io/en/stable/discord.html) if you need help creating a bot user. Version artifacts are available on both [**Dockerhub**](https://hub.docker.com/repository/docker/danmason/cljukebox) and the [**Github releases**](https://github.com/danmason/cljukebox/releases) tab.


### Using the JAR (Java 11+)

With Java installed, running the bot should be fairly simple. With `cljukebox.jar` downloaded to a folder, you can run the following:
```bash
java -jar cljukebox.jar <api-token>
```

This will set up the bot with your API token (- so in future runs bot can be ran with:
```bash
java -jar cljukebox.jar
```

### Using Docker
To persist bot configuration between runs, you will need to make a [**docker volume**](https://docs.docker.com/storage/volumes/). To create a volume, run the following: 
```bash
docker volume create <volume-name>
```

Once you have the above set up and you have pulled the latest version of the bot, you can run the following command:
```bash
docker run -v <volume-name>:/usr/lib/cljukebox danmason/cljukebox:0.1.0 <api-token>
```

This will set up the bot with your API token - so in future runs bot can be ran with:
```bash
docker run -v <volume-name>:/usr/lib/cljukebox danmason/cljukebox:0.1.0
```

## Releasing
Push a tag to the repository with the new version number - CircleCI will build the Uberjar and upload it to the tag's release, and will push the docker image to Dockerhub.

## License

Copyright © 2021 Daniel Mason

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
