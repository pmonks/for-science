#!/usr/bin/env bash

source ./bin/clean.sh
source ./bin/build

# Use a low memory ceiling to emulate Heroku dyno environment
java -XX:NativeMemoryTracking=summary -Xmx300m -Dclojure.server.repl="{:port 5555 :accept clojure.core.server/repl}" -jar ./target/for-science-standalone.jar "$@"
