[English](https://github.com/CaaMoe/MultiLogin/blob/v6/README.en.md)
<div align="center">

# MultiLogin

_✨ 正版与多种外置登录共存 ✨_

[![GitHub license](https://img.shields.io/github/license/CaaMoe/MultiLogin?style=flat-square)](https://github.com/CaaMoe/MultiLogin/blob/master/LICENSE)
[![QQ Group](https://img.shields.io/badge/QQ%20group-832210691-yellow?style=flat-square)](https://jq.qq.com/?_wv=1027&k=WrOTGIC7)
[![Join our Discord](https://img.shields.io/discord/1225725211727499347.svg?logo=discord&label=)](https://discord.gg/9vh4kZRFCj)
[![bStats](https://img.shields.io/bstats/servers/21890?color=brightgreen&label=bStats&logo=bs&style=flat-square)](https://bstats.org/plugin/velocity/MultiLogin/21890)

</div>

> [!IMPORTANT]
> 原作者已停止维护本项目。
>
> 当前版本由 [Miraitowa-zcx](https://github.com/Miraitowa-zcx) 同步到Velocity最新版本。其并非原作者授权的维护者，本非官方分支不代表原作者或原项目。

## 概述

MultiLogin 是一款主要为 Minecraft 代理端设计的插件，旨在实现对正版与多种外置登录共存的支持，用于连接两个或多个外置验证服务器下的玩家，使他们能够在同一个服务器上一起游戏。

## 特性

* 支持多达 128 个不同来源的 Yggdrasil 同时共存
* 鉴权代理、重试机制
* 游戏内档案管理系统
* 异步/同步皮肤修复机制
* 支持接管 Floodgate

## 安装

最低需要 `JDK 25`（运行环境也必须是 Java 25）。不需要安装 `authlib-injector`，没有任何前置插件，也不需要添加和更改 JVM 参数。

当前 Velocity 适配目标为官方 `4.1.0-SNAPSHOT`。构建默认跟随 PaperMC 官方最新的 4.1 构建；运行时会记录编译目标和实际代理版本。

~~把大象装进冰箱需要几步？~~

1. [下载](https://github.com/CaaMoe/MultiLogin/releases/latest) 插件
2. 丢进 plugins
3. 启动服务器

## 配置

详见 [Wiki](https://github.com/CaaMoe/MultiLogin/wiki)

## 构建

1. 克隆这个项目
2. 使用 JDK 25 执行 `./gradlew shadowJar` / `gradlew shadowJar`
3. 在 `*/build/libs` 下寻找你需要的

Velocity 构建默认解析官方最新构建。若要得到可复现产物，可固定构建号，例如：

```shell
./gradlew shadowJar -PvelocityBuild=8
```

解析成功后，官方 JAR 与元数据缓存在 `velocity/libraries`。离线构建只会使用已通过 SHA-256 与大小校验的缓存；没有有效缓存时会明确失败，不会悄悄改用其他版本。

## 数据库

`sql.backend` 支持 `H2`、`MYSQL` 和 `POSTGRESQL`。H2 仍是默认值；PostgreSQL 通常使用端口 `5432`，可使用默认连接模板或显式设置：

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

切换数据库后端不会自动搬迁已有数据。H2、MySQL 与 PostgreSQL 之间迁移时，请自行备份并导入数据；同一后端内已有 V2 表到 V3 表的升级仍由插件事务化完成。

或者你也可以

1. [Fork](https://github.com/CaaMoe/MultiLogin/fork) 此项目
2. 开启 Actions
3. 随便提交一个文件

## BUG 汇报

[Weekly Ver](https://github.com/CaaMoe/MultiLogin/releases/tag/weekly) 点击此处，也许你遇到的问题已修复

[832210691](https://jq.qq.com/?_wv=1027&k=WrOTGIC7) 点击此处，来加入QQ交流群

[new issue](https://github.com/CaaMoe/MultiLogin/issues/new) 点击此处，提交你的问题

[Discord](https://discord.gg/HJXHCZRS) 进来聊聊你的问题
## 贡献者

<a href="https://github.com/CaaMoe/MultiLogin/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=CaaMoe/MultiLogin"  alt="作者头像"/>
</a>

[我也想为贡献者之一？](https://github.com/CaaMoe/MultiLogin/pulls)
