#!/usr/bin/env bash

# Use a low memory ceiling to emulate Heroku dyno environment
clojure -J-XX:NativeMemoryTracking=summary -J-Xmx300m -J-Dclojure.server.repl="{:port 5555 :accept clojure.core.server/repl}" -Srepro -M:run "$@"
