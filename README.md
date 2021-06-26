| Branch | Build | Lint | Dependencies |
|-|-|-|-|
| [**main**](https://github.com/pmonks/for-science/tree/main) | [![Build](https://github.com/pmonks/for-science/workflows/build/badge.svg?branch=main)](https://github.com/pmonks/for-science/actions?query=workflow%3Abuild) | [![Lint](https://github.com/pmonks/for-science/workflows/lint/badge.svg?branch=main)](https://github.com/pmonks/for-science/actions?query=workflow%3Alint) | [![Dependencies](https://github.com/pmonks/for-science/workflows/dependencies/badge.svg?branch=main)](https://github.com/pmonks/for-science/actions?query=workflow%3Adependencies) |
| [**dev**](https://github.com/pmonks/for-science/tree/dev)  | [![Build](https://github.com/pmonks/for-science/workflows/build/badge.svg?branch=dev)](https://github.com/pmonks/for-science/actions?query=workflow%3Abuild) | [![Lint](https://github.com/pmonks/for-science/workflows/lint/badge.svg?branch=dev)](https://github.com/pmonks/for-science/actions?query=workflow%3Alint) | [![Dependencies](https://github.com/pmonks/for-science/workflows/dependencies/badge.svg?branch=dev)](https://github.com/pmonks/for-science/actions?query=workflow%3Adependencies) |

[![Open Issues](https://img.shields.io/github/issues/pmonks/for-science.svg)](https://github.com/pmonks/for-science/issues)
[![License](https://img.shields.io/github/license/pmonks/for-science.svg)](https://github.com/pmonks/for-science/blob/main/LICENSE)

# for-science

A small [Discord](https://discord.com/) bot that you can send Clojure code to, to experiment with the language, demonstrate core language principles, or just mess about.  Note: only supports the subset of Clojure's core provided by the [Small Clojure Interpreter](https://github.com/borkdude/sci).

Please review the [privacy policy](https://github.com/pmonks/for-science/blob/main/PRIVACY.md) before interacting with the bot.

## Adding the Bot to Your Discord Server

If you're an administrator of a server, [click here](https://discord.com/oauth2/authorize?client_id=854190844084355082&scope=bot&permissions=2148006976) and follow the instructions.

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
