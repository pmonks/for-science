| | | |
|---:|:---:|:---:|
| [**main**](https://github.com/pmonks/for-science/tree/main) | [![CI](https://github.com/pmonks/for-science/workflows/CI/badge.svg?branch=main)](https://github.com/pmonks/for-science/actions?query=workflow%3ACI+branch%3Amain) | [![Dependencies](https://github.com/pmonks/for-science/workflows/dependencies/badge.svg?branch=main)](https://github.com/pmonks/for-science/actions?query=workflow%3Adependencies+branch%3Amain) |
| [**dev**](https://github.com/pmonks/for-science/tree/dev) | [![CI](https://github.com/pmonks/for-science/workflows/CI/badge.svg?branch=dev)](https://github.com/pmonks/for-science/actions?query=workflow%3ACI+branch%3Adev) | [![Dependencies](https://github.com/pmonks/for-science/workflows/dependencies/badge.svg?branch=dev)](https://github.com/pmonks/for-science/actions?query=workflow%3Adependencies+branch%3Adev) |

[![Open Issues](https://img.shields.io/github/issues/pmonks/for-science.svg)](https://github.com/pmonks/for-science/issues)
[![License](https://img.shields.io/github/license/pmonks/for-science.svg)](https://github.com/pmonks/for-science/blob/main/LICENSE)

# for-science

A small [Discord](https://discord.com/) bot that you can send Clojure code to, to experiment with the language, demonstrate core language principles, or just mess about.  Note: only supports the subset of Clojure's core provided by the [Small Clojure Interpreter](https://github.com/borkdude/sci).

Please review the [privacy policy](https://github.com/pmonks/for-science/blob/main/PRIVACY.md) before interacting with the bot.

## Adding the Bot to Your Discord Server

If you're an administrator of a server, [click here](https://discord.com/oauth2/authorize?client_id=854190844084355082&scope=bot&permissions=2148006976) and follow the instructions.  You will also need to ensure that the bot has permissions to read messages from other people, and create messages in every channel you want to use it in.  The `!move` command also requires permission to delete other people's messages.

## Using the Bot

The bot provides these commands in any channel or a DM:
* `!clj ...forms...` - evaluate the following text as Clojure forms. If code fences are used, will only evaluate text within those code fences (thereby allowing for a "literate" style of message).  Note that each use of this command is run in a "fresh" instance of the interpreter - no state is maintained between invocations (to help avoid memory leaks).  If this is limiting for your use case, please [chime in here](https://github.com/pmonks/for-science/issues/7).
* `!move #channel` - logically moves the current conversation to #channel. This is done by posting cross-linked messages in both this channel and the other channel, and asking users to continue in the other channel. Note: it doesn't actually move any messages in a technical sense - it's more about logically moving a conversation from that point forward.

It also provides these commands in a DM only:
* `!help` - provides brief help.
* `!privacy` - provides a link to the bot's privacy policy.

## Running Your Own Copy of the Bot

### Obtaining API Keys

Configure a Discord bot using the [Discord developer portal](https://discord.com/developers), obtaining an API key.  Detailed instructions on this process are provided in the [`discljord` project](https://github.com/IGJoshua/discljord).

### Running the Bot

Currently the bot is only distributed in source form, so regardless of how you intend to deploy it, you'll need to clone this repository locally.

#### Direct Execution

1. Either set environment variables as described in the default [`config.edn` file](https://github.com/pmonks/for-science/blob/main/resources/config.edn), or copy that file somewhere else and hardcode the values in the file directly.
2. If you set the environment variables in the previous step run `clj -M:run`, otherwise run `clj -M:run -c /path/to/your/config.edn`

## Contributor Information

[Contributing Guidelines](https://github.com/pmonks/for-science/blob/main/.github/CONTRIBUTING.md)

[Bug Tracker](https://github.com/pmonks/for-science/issues)

[Code of Conduct](https://github.com/pmonks/for-science/blob/main/.github/CODE_OF_CONDUCT.md)

### Developer Workflow

The `for-science` source repository has two permanent branches: `main` and `dev`.  **All development must occur either in branch `dev`, or (preferably) in feature branches off of `dev`.**  All PRs must also be submitted against `dev`; the `main` branch is **only** updated from `dev` via PRs created by the core development team.  All other changes submitted to `main` will be rejected.

This model allows otherwise unrelated changes to be batched up in the `dev` branch, integration tested there, and then released en masse to the `main` branch.  The `main` branch is configured to auto-deploy to a production environment, and therefore that branch must only contain tested, functioning code.

## License

Copyright © 2021 Peter Monks

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)
