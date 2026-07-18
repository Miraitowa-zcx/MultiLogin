[中文](https://github.com/CaaMoe/MultiLogin/blob/v6/README.md)
<div align="center">

# MultiLogin

_✨ Coexisting Minecraft Authentication and Multiple BlessingSkin Authentication ✨_

[![GitHub release](https://img.shields.io/github/release/CaaMoe/MultiLogin.svg)](https://github.com/CaaMoe/MultiLogin/releases/)
[![GitHub license](https://img.shields.io/github/license/CaaMoe/MultiLogin?style=flat-square)](https://github.com/CaaMoe/MultiLogin/blob/master/LICENSE)
[![QQ Group](https://img.shields.io/badge/QQ%20group-832210691-yellow?style=flat-square)](https://jq.qq.com/?_wv=1027&k=WrOTGIC7)
[![Join our Discord](https://img.shields.io/discord/1225725211727499347.svg?logo=discord&label=)](https://discord.gg/HJXHCZRS)

</div>

> [!IMPORTANT]
> The original author has discontinued maintenance of this project.
>
> This version is independently developed and contributed to by [Miraitowa-zcx](https://github.com/Miraitowa-zcx). They are not an authorized maintainer, and this unofficial fork does not represent the original author or original project.

## Summary

MultiLogin is a plugin designed primarily for Minecraft proxy,
aimed at supporting the coexistence of Minecraft authentication and multiple BlessingSkin authentication.
It is used to connect players under two or more external authentication servers,
allowing them to play together on the same server.

## Features

* Supports up to 128 Yggdrasils from different sources coexisting simultaneously
* Authentication proxy and retry mechanism
* In-game profile management system
* Asynchronous/synchronous skin repair mechanism
* Support takeover of Floodgate

## Deploy

The minimum requirement is `JDK 25` (the runtime must also use Java 25).
No `authlib-injector`, prerequisite plugin, or extra JVM argument is required.

The current Velocity target is the official `4.1.0-SNAPSHOT`. Builds follow the
latest official 4.1 build by default, and runtime diagnostics report both the
compile target and the running proxy version.

1. [Download](https://github.com/CaaMoe/MultiLogin/releases/latest) plugin
2. throw into plugins
3. launch the server

## Config

See details in [Wiki](https://github.com/CaaMoe/MultiLogin/wiki)

## Secure chat compatibility

This fork supports Minecraft `1.21.x` through `26.x`. When premium and offline identities coexist, every downstream Paper server must set `enforce-secure-profile=false`. This only makes a Mojang-signed profile key optional: premium players whose key matches the final profile UUID retain native signed chat, while offline players or identities switched to a different UUID automatically fall back to unsigned chat.

Keep Velocity `modern forwarding` enabled between the proxy and downstream servers, and isolate downstream server ports with a firewall or private network so players cannot bypass the proxy.

## Build

1. Clone this project
2. Use JDK 25 and execute `./gradlew shadowJar`
3. Find the required artifact under `*/build/libs`

The build resolves the latest official Velocity build by default. Pin a build
for reproducible output when needed:

```shell
./gradlew shadowJar -PvelocityBuild=8
```

The verified official JAR and metadata are cached in `velocity/libraries`.
Offline builds use the cache only after its SHA-256 and size pass verification;
without a valid cache, the build fails instead of silently selecting another version.

## Database

`sql.backend` supports `H2`, `MYSQL`, and `POSTGRESQL`. H2 remains the default.
PostgreSQL normally uses port `5432` and can use either the default URL template
or an explicit URL:

```yaml
sql:
  backend: 'POSTGRESQL'
  ip: '127.0.0.1'
  port: 5432
  database: 'multilogin'
  username: 'multilogin'
  password: 'change-me'
  connectUrl: 'jdbc:postgresql://{0}:{1}/{2}'
```

Changing the backend does not automatically migrate data between H2, MySQL, and
PostgreSQL. Back up and import that data yourself. Existing V2-to-V3 upgrades
within the selected backend remain transactional.

## BUG report

[Weekly Ver](https://github.com/CaaMoe/MultiLogin/releases/tag/weekly) Click here, perhaps the issue you encountered has been fixed

[832210691](https://jq.qq.com/?_wv=1027&k=WrOTGIC7) Click here to join the QQ communication group

[new issue](https://github.com/CaaMoe/MultiLogin/issues/new) Click here to submit your question

[Discord](https://discord.gg/HJXHCZRS) Join and chat with us

## Contributors

<a href="https://github.com/CaaMoe/MultiLogin/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=CaaMoe/MultiLogin"  alt="Contributor's head"/>
</a>

[Want to be Contributor?](https://github.com/CaaMoe/MultiLogin/pulls)
