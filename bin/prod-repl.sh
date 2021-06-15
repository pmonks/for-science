#!/usr/bin/env bash
echo "Once the heroku port forwarder has started, in a separate shell run one of these commands:"
echo ""
echo "        rlwrap nc 127.0.0.1 5555"
echo "        rlwrap socat - TCP:localhost:5555"
echo ""
heroku ps:forward 5555 --app=for-science --dyno=bot.1
