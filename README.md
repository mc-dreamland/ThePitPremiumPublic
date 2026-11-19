# ThePitPremium · 天坑乱斗

> 本 README 由 AI 生成，仅供参考。

本项目基于 [ThePitCommunity/ThePitPremium](https://github.com/ThePitCommunity/ThePitPremium) 修改，适用于 **Paper 1.20.6 和 Java 21**。玩家从出生区进入竞技场，通过击杀获得金币和经验，逐步解锁天赋、精通与神话装备。

[从源码构建](#从源码构建) · [全新部署](#部署到服务器) · [启动排错](#启动排错) · [配置文件](#配置) · [地图与 pitsetup 教程](#地图配置与-pitsetup-教程) · [占位符](#placeholderapi-占位符) · [命令说明](#命令说明)

## 相对 ThePitCommunity/ThePitPremium 的主要改动

对比基准为上游 `master` 的公开源码快照。[上游提交 `ebcfe2e5`](https://github.com/ThePitCommunity/ThePitPremium/tree/ebcfe2e5f2109c90b7c0416d0b112e6891e13f66)

- **服务端适配与代码重构。** 从 Spigot 1.8.8 迁至 Paper 1.20.6 和 Java 21，将 Java、Kotlin 混合的插件源码统一为 Java。数据包处理从 ProtocolLib 迁到 PacketEvents，移动与朝向监听改由插件内部统一分发。
- **新增天赋、附魔、道具与活动。** 加入「神秘学说」天赋、「杰作」超连杀和「蚱蜢」「石像鬼」附魔。玩家可在神秘造物台合成和开启神秘之盒，也能体验附魔爆发与世界 BOSS 等新活动。
- **训练区与野外陪练。** 接入 MythicMobs，两类陪练可分别配置刷新点、数量和奖励，并按玩家数量分批补充。奖励按伤害贡献分配。头顶名字也可按区域和精通等级隐藏，并为连杀、赏金和活动保留显示条件。
- **性能优化。** 实测可在E5 2630 V4 服务器上承载150人,TPS保持在19+。
- **交易与物品管理。** 服主可以关闭玩家交易和报价，或设置神话装备再次交易的冷却。神话装备记录创建者、现持有者和最近成交时间。`Items.yml` 提供 **127 个物品内部名**的默认条目，可逐项调整显示名和 UUID 开关。
- **存档排查与保护。** 管理员可用 `/scan` 按玩家或物品 UUID 查找重复装备，沿历史备份查看持有人和附魔变化，并从报告界面将已确认的重复 UUID 加入回收名单。增加本地和 Redis 保存屏障，降低玩家重连或跨服切换时过早读档的风险。

## 从源码构建

准备 JDK 21，在包含 `build.gradle.kts` 和 `settings.gradle.kts` 的项目根目录构建。源码包需要包含 `gradle/libs.versions.toml`、`src/main/` 中的代码和插件资源，以及 `src/test/` 中的测试。先按下表准备依赖，再运行构建命令。

| 依赖 | 如何准备 |
| --- | --- |
| Paper 1.20.6 开发包 | Gradle 通过 paperweight 解析 `1.20.6-R0.1-SNAPSHOT`。首次构建需要访问构建脚本中声明的 Maven 仓库。 |
| Citizens | 将兼容 Paper 1.20.6 的 Citizens API 或插件 JAR 放入 `libs/`。本项目使用的版本为 `2.0.38-b3786`。 |
| MythicMobs | 自行取得 **5.8.2**，放在 `libs/MythicMobs-5.8.2.jar`。构建脚本按这个文件名引用，训练 Bot 也会检查服务端版本。 |
| 点券插件 | **请自行修改 HeyPoints 对接，替换为自己的点券插件。** 将替代插件的 API 加为 `compileOnly` 依赖，或放入 `libs/`。改动位置见下节。 |
| AVipSystem | **请自行适配会员系统，或移除下文列出的 VIP 功能。** 原实现编译时使用 `com.andan:AVipSystem:1.0-SNAPSHOT`，运行时也会直接调用该插件。沿用原实现时需要自行取得对应 API 和运行版。 |
| 其余依赖 | PacketEvents、NBTAPI、DecentHolograms、PlaceholderAPI、LuckPerms、FAWE、RedLib、MongoDB 驱动、Jedis 等按 [构建脚本](build.gradle.kts) 和 [版本目录](gradle/libs.versions.toml) 解析。`compileOnly` 依赖不会随插件打包。 |

项目根目录下的 `libs/` 用于编译。沿用 AVipSystem 且手里只有 API JAR 时，可将它放入 `libs/`，并删除构建脚本中的 `compileOnly("com.andan:AVipSystem:1.0-SNAPSHOT")`。脚本会自动读取这个目录中的非 MythicMobs JAR。保留原坐标时，需要让 Gradle 能从所配的 Maven 仓库或本机 Maven 仓库解析它。

**选择构建方式。** 项目使用 Gradle 8.13。包含 `gradle/wrapper/gradle-wrapper.jar` 的源码包可直接使用下面的 Wrapper 命令。纯源码包省略了这个 JAR 时，用本机 Gradle 8.13 在同一目录运行 `gradle build`。可先用 `java -version` 核对当前 Java 环境。

**Windows PowerShell**

```powershell
.\gradlew.bat build
```

**Linux / macOS**

```sh
sh ./gradlew build
```

`build` 会编译、运行测试并打包。当前版本部署到 Paper 1.20.6 时，使用带有运行库的 **`build/libs/ThePitPremium-3.93.jar`**。音乐附魔按资源名从 JAR 内读取 `.nbs` 乐谱；源码发行版如省略乐谱，应在构建前配上有权使用的同名资源，或调整相应附魔。

### 替换 HeyPoints

1. 在 [PlayerPointsAPIImpl.java](src/main/java/cn/charlotte/pit/impl/PlayerPointsAPIImpl.java) 中替换 HeyPoints 的导入、插件查找和 API 调用。保留该类和 `INSTANCE` 可以沿用现有调用方，包括启动入口 `PitHook.loadAPI()` 和末影箱扩容菜单。
2. 按 [PointsAPI.java](src/main/java/cn/charlotte/pit/api/PointsAPI.java) 对接三个方法：`getPoints(player)` 读取余额，`hasPoints(player, points)` 判断余额是否足够，`costPoints(player, points, reason)` 扣款。沿用玩家 UUID 对应的账户，并把 `reason` 传给支持交易记录的接口。
3. 在 [plugin.yml](src/main/resources/plugin.yml) 的 `depend` 中将 `HeyPoints` 换为自家点券插件在其 `plugin.yml` 中声明的名称。将对应的运行版插件安装到服务端 `plugins/`。
4. **如果开放增值包购买**，还要修改 [SupporterEntranceButton.java](src/main/java/cn/charlotte/pit/menu/supporter/button/SupporterEntranceButton.java)。当前购买按钮通过控制台执行 `deliveryconfirm <玩家> pit_rank_pack`。请将它接到自己的购买、扣款和发货流程，确保一笔购买只扣一次点券。末影箱扩容则直接通过 `PointsAPI` 扣款。接入后分别测试余额不足和成功购买两种情况。

### 替换或移除 AVipSystem

保留现有会员权益只需要两项能力：**查询在线玩家的整数 VIP 等级**（普通玩家返回 `0`），以及可选的**会员显示前缀**。等级可以来自自己的会员插件，也可以由权限组映射。点券余额、扣款和增值包发货沿用上一节的接入方式。

| 功能 | 沿用现有规则时如何平替 | 不需要该功能时可以移除什么 |
| --- | --- | --- |
| 击杀与助攻奖励 | 在 [CombatListener.java](src/main/java/cn/charlotte/pit/listener/CombatListener.java) 替换两处等级查询。击杀奖励每级增加 5% 经验和金币，Bot 奖励也复用这段计算；玩家助攻从 VIP 3 起按完整等级计算，例如 VIP 3 加 15%。 | 移除两处添加“VIP加成”的经验和金币倍率，保留基础奖励、天赋及独立的额外经验和金币加成。专门覆盖这两处等级的调试命令 `/pitdebug activateVipBonus` 也可一并移除。 |
| VIP 5 自动获得增值包 | [PlayerProfile.isSupporter()](src/main/java/cn/charlotte/pit/data/PlayerProfile.java) 先检查档案中的 `betterPlayer` 标记，在线玩家达到 VIP 5 后会将该标记设为 `true`。替换等级来源即可沿用自动授予规则。 | 只移除 VIP 5 自动授予分支，保留已有档案标记和 `/pitpack` 发放路径。这样已购或已获赠的玩家仍可使用增值包权益，包括末影箱空间和展示命令。 |
| 菜单与称号显示 | [SupporterEntranceButton.java](src/main/java/cn/charlotte/pit/menu/supporter/button/SupporterEntranceButton.java) 用等级显示加成比例，用 `%avip_chat%` 显示会员称号；[LuckPermsUtil.java](src/main/java/cn/charlotte/pit/util/rank/LuckPermsUtil.java) 也用该占位符拼接前缀。换成自己提供的等级和称号即可。 | 可去掉会员称号、加成比例及 VIP 5 赠送提示，并同步修改[末影箱菜单](src/main/java/cn/charlotte/pit/menu/enderchests/EnderChestsMenu.java)的 VIP 5 文案。其他称号、点券余额和增值包购买入口均可单独保留。 |

完成替换或移除后，清理三个直接调用类中的旧 `import`，从 [build.gradle.kts](build.gradle.kts) 删除 AVipSystem 的 `compileOnly` 声明。若新实现直接调用其他会员插件的 Java API，添加该 API 的编译依赖，并在 [plugin.yml](src/main/resources/plugin.yml) 的 `depend` 中声明实际插件名。仅用 Bukkit 权限映射等级时，无需另加会员 API。

验证时可用普通玩家、VIP 2 和 VIP 3 分别检查击杀与助攻：VIP 2 的击杀加 10%、助攻无 VIP 加成，VIP 3 的两者都加 15%。验证 VIP 5 自动发包时使用尚未获得增值包的档案，避免已有标记掩盖测试结果。

## 部署到服务器

1. 用 Java 21 准备一台 Paper 1.20.6 服务器，在停服状态下放入天坑地图，并确认该世界会随服务器加载。插件启动时会调整已加载世界的游戏规则，包括保留物品栏和关闭昼夜循环。
2. 在服务端 `plugins/` 安装硬依赖：**PacketEvents、NBTAPI、Citizens、适配后的点券插件、DecentHolograms、PlaceholderAPI、MythicMobs 5.8.2**。项目编译时使用 PacketEvents 2.11.2、NBTAPI 2.15.0、DecentHolograms 2.9.6 和 PlaceholderAPI 2.11.6。
3. 按源码中的实际对接安装会员插件：沿用原实现时安装 AVipSystem，改接其他插件时安装对应运行版，已移除会员 API 调用时可跳过此项。权限组和前缀逻辑使用 LuckPerms。带建筑粘贴或回滚的事件使用 **FastAsyncWorldEdit（FAWE）** 的 API。插件描述将 WorldEdit、FAWE、LuckPerms 列为软依赖。
4. 为新服准备独立的空 MongoDB 数据库，并授予插件账号读写和建索引权限。创建 `plugins/ThePitPremium/`，将配置模板复制为该目录下的 `config.yml`。填写数据库连接、交易规则、本服地图的出生点和保护区。账号的认证库应与 `service.mongodb.database` 一致。单服可保持 Redis 关闭，多个子服共享档案时应连接同一数据库和同一 Redis。[配置模板](src/main/resources/config.yml) · [出生区示例](#首次布置出生区)
5. 将构建出的插件 JAR 放入 `plugins/`。按玩法准备事件建筑：尖塔夺魁读取 `plugins/ThePitPremium/schem/spire.schem` 和 `pit_door.schem`，疯狂天坑读取同目录的 `pit_door.schem`，天坑外卖读取 `hamburger.schem`。这些是运行时读取的地图文件。
6. 完整启动服务器。首次启动会在 `plugins/ThePitPremium/lib/` 缓存运行库，需要能访问依赖仓库。进服后用 `/pitsetup locs tp loc.spawn 0` 核对第一个出生点。启用训练 Bot 时用 `/pitbot status` 检查状态。
7. **验证新玩家档案。** 用测试账号首次进服。档案加载后，管理员执行 `/papi parse <测试玩家名> %pit_coins%` 记录余额，再执行 `/modify coin add <测试玩家名> 1`。测试玩家正常退出并完成保存后，重启服务器。重新登录后再查余额，应比首次查询多 1 金币。
8. 如果配置了 `trade.enabled: false`，让两名测试玩家同时在线，由其中一人执行 `/trade <对方名字>`，确认收到“当前服务器已关闭交易功能。”

### 启动排错

从服务端 `logs/latest.log` 找到与 ThePitPremium 有关的第一条错误，按出现的阶段检查。

| 现象 | 检查方法 |
| --- | --- |
| Paper 提示缺少依赖插件 | 核对报错中的插件名与 `plugin.yml` 的 `depend`，再往前查该依赖自身的启动日志。改接点券插件后仍提示缺少 HeyPoints，检查是否部署了修改描述后重新构建的 JAR。 |
| 出现 `Exception whilst downloading dependency` | 记下同一行的库名，查看紧接着的异常。检查服务端到对应仓库的网络，以及运行账号对 `plugins/ThePitPremium/lib/` 的写入权限，修复后重启。 |
| 出现 `Connected!`，但玩家档案无法读写 | 索引检查在后台进行。继续查找 `Unable to create MongoDB index` 和登录、退出时的数据库异常。连接超时先查地址、端口和网络。认证失败则核对账号所在库与 `service.mongodb.database`，并检查该账号的读写和建索引权限。 |
| 插件已启用，训练 Bot 未出现 | 在游戏内执行 `/pitbot status`。若日志提示版本不符，检查服务端实际加载的 MythicMobs 是否为 5.8.2。若提示缺少怪物类型，核对 `plugins/MythicMobs/mobs/PitTrainingBots.yml`，依次执行 `/mm reload` 和 `/pitbot reload`。 |

出生点、NPC 或事件场地出错时，见[地图验收与排错](#开服验收与排错)。

## 配置

首次开服按以下顺序操作。服主在服务端修改 `plugins/ThePitPremium/` 下的文件，搭图管理员在游戏内设置位置。

1. 首次启动前，在从模板复制的 `config.yml` 中填写 MongoDB 连接信息，并按本服玩法设置 `trade.enabled` 和神话装备交易冷却。
2. 先填至少一个可安全落脚的出生点，以及出生区的两个对角。启用蛋糕事件时，一并填好下文的蛋糕区域。这样即使地图与模板的 `world` 完全不同，也能从有效位置开始搭图。
3. 启动后用有 `pit.admin` 权限的玩家进服，依次完成[出生区](#首次布置出生区)、[NPC](#npc-设点)、[全息文字](#全息文字与工作台)、[事件](#事件与其他地图点位)。手工编辑 YAML 时先停服，避免后续游戏内设点把磁盘改动覆盖。
4. 完整重启，用普通玩家检查出生、保护边界、商店和实际开放的事件。训练 Bot 和头顶名字各有独立配置与重载命令。

### 主配置 config.yml

以下为单服配置片段，合并到默认文件对应节点即可；保留其余地图坐标。将示例账号与密码替换为实际值。

```yaml
service:
  mongodb:
    ip: 127.0.0.1
    port: 27017
    database: thePit
    user: pit_app
    password: "替换为数据库密码"
  redis:
    enable: false
  webhooks:
    customName: ""
trade:
  enabled: true
  mythic-equipment-cooldown: "12h"
```

| 配置项 | 用途 |
| --- | --- |
| `service.mongodb` | 保存档案、背包、邮件和交易记录等。认证库就是 `database` 指定的库，示例中的用户应在 `thePit` 库创建并具备该库的读写权限。无认证的本地开发实例可移除 `user` 和 `password` 两项。 |
| `service.redis` | 默认关闭。两个子服共享档案时，让它们连接同一个 MongoDB 数据库和 Redis 实例；在每端的 `redis` 节点填写 `enable: true`、`ip`、`port` 和对应的 `password`。 |
| `service.webhooks.customName` | 自定义名称审核的 HTTP 回调，发送申请 UUID、玩家名和待审核名称。开放改名纸审核时填写自己的接收地址。 |
| `trade.enabled` | 默认 `true`。设为 `false` 后，玩家发起或接受交易、报价时会看到“当前服务器已关闭交易功能。” |
| `trade.mythic-equipment-cooldown` | 神话装备再次交易的间隔。默认 `"-1"` 表示每件仅能交易一次；`"30m"`、`"12h"`、`"7d"` 分别按分钟、小时、天计算，`"0m"` 允许立即再次交易。交易窗口修改内容后的确认等待固定为 5 秒。 |
| `loc.spawn`、`loc.region.pitA`、`loc.region.pitB` | 出生点列表和出生安全区的两个对角。至少保留一个有效出生点，坐标格式为 `世界名\|X\|Y\|Z\|Yaw\|Pitch`。 |
| `loc.npc`、`loc.hologram`、`loc.events` | 设置 NPC、全息文字和地图事件的位置。换地图后应替换默认坐标。 |
| `server-name`、`server-qun` | 分别填写服务器显示名和计分板上的交流群文案。公开服请填自己的信息。 |
| `enchants.disable`、`arenaHighestY` | 禁用列表填写附魔实现返回的名称；空列表写 `[]`。普通玩家放置方块的 Y **达到**高度上限就会被拦截。 |
| `curfew.enable`、`curfew.start`、`curfew.end` | 按服务器本地时间的整点小时判断宵禁，起止小时均包含。例如 `0` 至 `7` 覆盖 00:00 至 07:59；当前比较方式要求开始小时小于或等于结束小时。 |
| `debug.debugServer`、`debug.public` | 正式服将前者设为 `false`。开启调试服后，后者为 `false` 时仅 OP 或有 `thepit.admin` 权限的玩家可以进入；设为 `true` 仍会按玩家名称颜色筛选。 |
| `genesis-start-date` | 阵营活动的起始时间，填写 Unix 时间戳，单位为毫秒。 |
| `booster` | 战斗提示会读取该倍率。当前击杀结算中的对应乘法已注释，调整后请以档案实际到账金额和经验为准。 |

### 其他配置文件

| 文件 | 内容 | 修改后生效方式 |
| --- | --- | --- |
| `plugins/ThePitPremium/Items.yml` | 按物品 InternalName 配置唯一 UUID 和基础显示名。 | 重启服务器。 |
| `plugins/ThePitPremium/custom.yml` | 增值包价格与货币显示名（`vip-price`、`price-name`）、聊天格式、部分事件最低在线人数（`event-online-required`）。 | 重启服务器。 |
| `plugins/ThePitPremium/bots.yml` | 训练区、野外刷新点、数量与奖励。也可以在游戏内标记位置。 | `/pitbot reload`，已有 Bot 会清理并按新配置补充。 |
| `plugins/MythicMobs/mobs/PitTrainingBots.yml` | Bot 的实体、属性、装备和伪装。首次启动时从插件资源复制。 | 先 `/mm reload`，再 `/pitbot reload`。 |
| `plugins/ThePitPremium/name-hiding.yml` | 头顶玩家名的隐藏区域、精通区间及显示条件。 | `/namehide reload`，用 `/namehide status` 核对。 |

**物品 UUID 与名称。** 首次启动会生成 [Items.yml](src/main/resources/Items.yml)。按物品的内部名找到对应条目，修改后重启。例如，给小麦启用 UUID 并改名，同时关闭新生成神话之剑的 UUID：

```yaml
wheat:
  uuid: true
  displayName: '&a丰收小麦'
mythic_sword:
  uuid: false
  displayName: '神话之剑'
```

`uuid: true` 为每件新发放的物品写入独立的 `extra.uuid`。例如用 `/pititems give <玩家> WHEAT 3` 发放已启用 UUID 的小麦时，会收到 3 件数量各为 1、UUID 各不相同的小麦。背包装不下的命令发放物品会寄到玩家邮箱。`uuid: false` 时，新物品按正常规则堆叠。历史物品与备份保留原有编号，改名券的审核单号单独保存。

`displayName` 是基础显示名，支持 `&` 颜色代码；未填、留空或新增物品尚无条目时使用代码里的原名。共用一个 InternalName 的变体共用设置，例如 `fishing_fish_irony` 下的几种鱼留空时各自保留原名。神话装备重建时仍会应用等阶、前缀和玩家改名。修改显示名后可新领取一件物品验证，存量物品的已保存名称在重建前保留。

在源码中新增物品时，定义稳定的 `getInternalName()` 和原名 `getDefaultItemDisplayName()`，通过 `getItemDisplayName()` 获取最终名称。注册到 `PitItems` 的物品可以覆写 `isUuidEnabledByDefault()` 指定默认开关；`Items.yml` 中明确填写的 `uuid` 优先。

**调整玩法。** 首启后打开 `custom.yml`，在已有节点下修改。例如测试占山为王时，可以降低 `koth` 的人数门槛；新手保护则用小数表示伤害倍率。以下示例保留其他事件和规则的原值，合并时只修改对应子项。

```yaml
event-online-required:
  koth: 2
noob-protect:
  enable: true
  level: 120
  damage_boost: 1.1
  damage_reduce: 0.9
```

- 新手保护作用于尚未精通且等级低于 `level` 的玩家。示例对应造成伤害加 10%、承受伤害减 10%。占山为王、龙蛋、蛋糕及空投分别读取 `koth`、`dragon_egg`、`cake`、`care_package` 的人数。尖塔、外卖和疯狂天坑等大型事件在实现中按分区返回 20 或 50 人，修改文件中的同名项不会改变这些门槛。
- 增值包菜单的价格与货币名分别填入 `vip-price` 和 `price-name`，实际扣款与发货要与前文点券对接一致。自定义聊天时开启 `custom-chat-format.enable`，在 `custom-chat-format.format` 保留玩家显示名和消息的两个 `%s`。`dateFormat` 使用如 `MM/dd HH:mm` 的时间格式。
- 配置 MythicMobs 普通怪物的击杀奖励时，在 `mm-listener` 下用怪物内部 ID 建一项，设置 `xp-provide` 和 `coins-provide`，例如都填 `"10-20"`。在该项的 `kill-message` 设置给击杀者的提示，可使用 `{mobName}`、`{coin}`、`{exp}`。实现会读取 `kill-broadcast.enable` 和 `kill-broadcast.message`，但发送全服广播的代码已注释。训练 Bot 奖励在 `bots.yml` 单独配置。

| `custom.yml` 中的其他设置 | 填写方式与适用范围 |
| --- | --- |
| `scoreboard-showtime`、`water-mark`、`bot-name` | 分别控制计分板时间显示、神话物品说明中的水印、部分称号中的 Bot 名称。训练陪练本身的显示名填在 `bots.yml`。 |
| `festival-boosts` | 节日名称作为子键，`default` 用于未单独配置的节日。例如在该节放入 `default: 0.35`，表示系统识别为节日时，经验和金币各增加 35%。 |
| `luck-gem`、`rate`、`mythicDropChance` | `luck-gem` 填小数概率，作用于带有 `lucky` 状态的玩家。`rate` 的 `dark`、`normal`、`rage`、`dark_green` 分组和 `mythicDropChance` 的权限匹配可用各组的 `test` 指定。当前实现使用代码中的概率常量；调整模板里的 `value` 不会改变这些概率。 |
| `pitSupportPermission`、`removeSupportWhenNoPermission` | 插件向外部接口提供的增值包权限名和撤权设置。会员自动发包条件与购买入口见上文的会员适配说明。 |
| `kingsQuestsMarker` | 首次启动时由插件生成并保存国王任务 UUID，使用默认值即可。 |
| `lobby-command`、`*-npc-skin`、`highest-prestige`、`New.Worldenble`、`New.WorldTime` | 这些键出现在生成的默认文件中。当前仓库没有通过它们加载大厅命令、覆盖 NPC 皮肤、修改精通上限或切换世界计时的有效调用。改皮肤时检查具体 NPC 的 `getNpcSkin()` 实现。 |

各键的默认值与实际读取规则见[玩法配置实现](src/main/java/cn/charlotte/pit/config/NewConfiguration.java)。

**布置训练 Bot。** 在训练区下角执行 `/pitbot pos1 training`，到对侧上角执行 `/pitbot pos2 training`，让区域覆盖 Bot 的两格身高。在每个野外刷新点执行 `/pitbot add`，用 `/pitbot list` 查看编号。设错位置时，用 `/pitbot remove <编号>` 删除该点。在 `bots.yml` 的 `training` 和 `wild` 下分别调整 `enabled`、`min-bots`、`max-bots`、`bots-per-player`、`respawn-seconds`、`experience` 和 `coins`。手改后执行 `/pitbot reload`、`/pitbot status`、`/pitbot count`。奖励衰减、连杀和特殊 Bot 倍率见[带注释的 Bot 配置](src/main/resources/bots.yml)。

**配置头顶名隐藏。** 在 `name-hiding.yml` 设置 `enabled`、精通范围 `prestige.min/max`，并分别选择 `areas.spawn`、`areas.middle`、`areas.wild`。区域按被观察玩家的位置判断；出生区使用下文的安全区，中央区使用疯狂天坑的中心和尺寸。配置 `show-above-streak` 和 `show-above-bounty` 可让达到阈值的玩家显示名字，填 `-1` 关闭对应例外。执行 `/namehide reload`、`/namehide status`，用两名玩家跨区观察。其余选项见[名字隐藏配置](src/main/resources/name-hiding.yml)。

### PlaceholderAPI 占位符

ThePitPremium 启动后会注册以下变量，可填入支持 PlaceholderAPI 的计分板、菜单或全息文字。先用已加载档案的玩家在游戏内执行 `/papi parse me %pit_coins%`，返回值应与该玩家的游戏金币余额一致。[PlaceholderAPI 命令说明](https://wiki.placeholderapi.com/users/commands/#papi-parse)

| 变量 | 返回内容 |
| --- | --- |
| `%pit_level%`、`%pit_prestige%`、`%pit_exp%` | 玩家当前等级、精通次数、经验值。 |
| `%pit_level_tag%`、`%pit_level_tag_roman%` | 带颜色的等级标签，后者使用罗马数字显示精通。 |
| `%pit_coins%`、`%pit_bounty%` | 游戏金币余额、当前赏金。 |
| `%pit_has_bonus_exp%`、`%pit_has_bonus_gold%`、`%pit_is_supporter%` | 是否有额外经验加成、额外金币加成、增值包资格，返回 `true` 或 `false`。 |
| `%pit_genesis_tag%` | 阵营活动期间返回天使或恶魔标记，活动未开启或玩家未选阵营时为空。 |
| `%pit_forceprestige_require%` | 按当前精通次数计算的数值。精通低于 10 时为 `1000 + 精通 × 500`，从 10 起为 `1000 + 精通 × 1000`。 |
| `%pitnpcholo_shop_0%` | 商店 NPC 面向当前玩家显示的第 1 行文字。同类变量使用 NPC 内部名和从 `0` 开始的行号。 |

## 地图配置与 pitsetup 教程

在游戏内使用 `/pitsetup`，别名为 `/thepitsetup` 和 `/setup`，权限为 `pit.admin`。以下命令中的字段对应 `config.yml` 的嵌套路径，**区分大小写**。例如 `loc.npc.leaderBoard` 中的 `B` 必须大写。大多数坐标写作 `世界名|X|Y|Z|Yaw|Pitch`，后两项表示水平和俯仰朝向。单点用一行字符串，多个候选点用 YAML 列表。

### 首次布置出生区

1. **先让玩家安全进服。** 首次启动前，在 `config.yml` 的 `loc` 节点中填写下列字段。将 `pit_world` 换成本服实际加载的世界名，坐标填本服地图的实测值。`spawn` 会随机选点，列表中的每一项都应是本服可安全落脚的位置。

   ```yaml
   loc:
     spawn:
       - "pit_world|0.5|86|0.5|180|0"
     region:
       pitA: "pit_world|-12|82|-12|0|0"
       pitB: "pit_world|12|94|12|0|0"
   ```

2. **圈出安全区。** 启服后站到保护范围一侧的最低角，执行 `/pitsetup locs loc.region.pitA`；到对侧的最高角执行 `/pitsetup locs loc.region.pitB`。两个点决定长方体的宽、长和高度。确保出生平台和玩家站立高度都在里面，跳下天坑后能离开保护范围。
3. **校准出生点与朝向。** 站到出生位置，面向玩家应看到的方向，执行 `/pitsetup quickreplace loc.spawn 0`。需要多个随机出生点时，到每个新位置执行 `/pitsetup quickadd loc.spawn`。用 `/pitsetup locs tp loc.spawn 0`、`/pitsetup locs tp loc.spawn 1` 逐个核对。只保留一个点时，第一个索引就是 `0`。
4. **验收。** 用普通玩家执行 `/spawn`，确认脚下有落脚方块且头顶留有空间。让两名玩家分别在保护区内、离开保护区后的竞技场测试伤害。保护边界偏高或偏低时，重新设置对应角点。

### 通用设点与修正

| 命令 | 在哪里执行、会改变什么 |
| --- | --- |
| `/pitsetup locs <字段>` | 站到目标处执行，保留脚下的精确坐标和朝向。单点覆盖旧值，坐标列表追加一项。适合 NPC、半砖平台和需要小数高度的全息文字。 |
| `/pitsetup quickadd <字段>` | 单点覆盖、列表追加。将 X、Z 对齐到所在方块中心，Y 向下取整，保留朝向。适合整格地面的出生点、旗帜或事件候选点。 |
| `/pitsetup quickreplace <字段> [索引]` | 使用同样的居中坐标。改列表前先查清索引，第一项为 `0`；省略、负数或越界都会**追加**。例如 `/pitsetup quickreplace loc.spawn 1` 只有在已有两项时才会替换第二项。 |
| `/pitsetup locs tp <字段> [索引]` | 传送回保存的位置。单点省略索引，如 `/pitsetup locs tp loc.npc.shop`；列表必须指定索引，如 `/pitsetup locs tp loc.spawn 0`。 |
| `/pitsetup locs remove <字段> [索引]` | 清单点时省略索引，删列表时填写有效索引，如 `/pitsetup locs remove loc.spawn 2`。删除后后面的项会前移。批量删除可从最大索引向前操作，出生点始终保留至少一个有效位置。 |
| `/pitsetup locs list [all/single/list]` | 三个参数任选其一，省略等于 `all`。`single` 可查看已设置的普通单点。列表枚举只扫描旧的 `list.` 前缀，`list` 或 `all` 会漏掉实际的出生点和事件点；请用 YAML 和带索引的 `locs tp` 核对。 |
| `/pitsetup getLocation` | 显示当前位置，点击聊天中的坐标可将文本带入输入框，再按需复制。 |
| `/pitsetup edit` | 切换自己的地图编辑状态。需要破坏或布置方块时开启，完工后再执行一次关闭。单纯记录坐标可直接设点。 |
| `/pitsetup reloadnpc` | 按当前内存坐标调用 Citizens 生成 NPC，也可以在控制台执行。若已生成的 NPC 仍在旧位置，完整重启；手改 YAML 后也要重启。 |
| `/pitsetup punchY <数值>` | 例如 `/pitsetup punchY 4.0` 会把数值保存至 `custom.yml` 的 `punch_y`。当前读取该值设置击飞高度的代码已注释。 |

**批量设点用标记箭。** 执行 `/pitsetup LocationsSetup loc.events.koth.locs`，主手拿到标记箭后，到每个候选点站稳，正常站立点击一次，间隔至少一秒。记录的是玩家脚下的方块中心。结束时再执行 `/pitsetup LocationsSetup` 退出并收回箭；切换字段时先退出再指定新字段。进入时省略字段会进入 `All` 状态，此时点击不会写入点位。潜行点击可清除单点；纠正列表请用 `locs remove <字段> <索引>`，旧的潜行删除逻辑按 `list.` 前缀识别列表。

**查看临时标记。** 例如 `/pitsetup LocationsSetup hologram show loc.npc.shop` 显示商店坐标，`/pitsetup LocationsSetup hologram show single` 显示普通单点。还可传 `all` 或 `list`，省略等于 `all`。列表标记同样受旧前缀限制。退出箭头模式会清掉所有搭图管理员共用的临时标记。

**清空或初始化列表。** 维护时停服，把对应 YAML 列表设为 `[]` 再启动。例如在 `loc → events → koth` 下写 `locs: []`，然后进服追加山头。未初始化的列表可能显示设点成功却没有保存；聊天面板里的 `/pitsetup locs clear` 也没有对应的注册命令。正式开放事件前至少填好一个可用点。

### NPC 设点

站在 NPC 应出现的位置，面向玩家接近的方向。例如执行 `/pitsetup npc shop`，离开原地，再用 `/pitsetup locs tp loc.npc.shop` 核对坐标和朝向。下表“输入”均接在 `/pitsetup` 后；使用完整字段的行可直接照抄。

| NPC | 输入 | 保存字段 |
| --- | --- | --- |
| 商店、任务 | `npc shop`、`npc quest` | `loc.npc.shop`、`loc.npc.quest` |
| 天赋、精通、统计 | `npc perk`、`npc prestige`、`npc status` | `loc.npc.perk`、`loc.npc.prestige`、`loc.npc.status` |
| 守门人、邮件 | `npc keeper`、`npc mail` | `loc.npc.keeper`、`loc.npc.mail` |
| 合约、交易 | `npc contract`、`npc trade` | `loc.npc.contract`、`loc.npc.tradeNPC` |
| 创世阵营恶魔、天使 | `npc genesisdemon`、`npc genesisangel` | `loc.npc.genesis_demon`、`loc.npc.genesis_angel` |
| 烘焙师、鸡、蠹虫 | `npc cakebaker`、`npc chicken`、`npc silverfish` | `loc.npc.cake_bake`、`loc.npc.chicken`、`loc.npc.silver_fish` |
| 国王 | `locs loc.npc.king` | `loc.npc.king`，旧 `npc kinger` 写错字段 |
| 排行榜 NPC | `locs loc.npc.leaderBoard` | `loc.npc.leaderBoard`，旧 `npc leader` 的大小写不匹配 |
| 帮助 NPC | `locs loc.npc.helpNPC` | `loc.npc.helpNPC`，旧 `npc help` 写错字段 |
| 活动 NPC、活动 Boss | `locs loc.npc.activity`、`locs loc.npc.boss` | `loc.npc.activity`、`loc.npc.boss`，按对应活动设置 |

### 全息文字与工作台

先选好文字的显示位置和高度，再执行 `/pitsetup locs <右栏字段>`。例如设置排行榜 NPC 后，飞到文字应显示的位置执行 `/pitsetup locs loc.hologram.leaderBoard`。NPC 和对应文字可以分别移动。以下旧 `holo` 快捷命令仍有注册，字段名已与当前配置脱节。

| 用途 | 旧快捷命令（接在 `/pitsetup` 后） | 当前字段 |
| --- | --- | --- |
| 出生提示 | `holo spawn` | `loc.hologram.spawn` |
| 神话附魔台提示、附魔说明 | `holo enchant`、`holo enchantTip` | `loc.hologram.mythic`、`loc.hologram.enchantTip` |
| 垃圾桶、末影箱 | `holo trashbin`、`holo enderchest` | `loc.hologram.trashBin`、`loc.hologram.chest` |
| 排行榜、帮助文字 | `holo leader`、`holo helper` | `loc.hologram.leaderBoard`、`loc.hologram.helper` |
| 工作台、火炉 | `holo workTable`、`holo fireTable` | `loc.hologram.workTable`、`loc.hologram.fireTable` |
| 附魔锻造台 | `holo enchantForgeTable` | `loc.table.enchantForgeTable` |
| 神秘造物台 | 使用完整字段 | `loc.hologram.mystery` |

**附魔台方块坐标**另存于 `loc.region.enchant`，用于附魔效果定位。看准方块的 X、Y、Z，停服后填入 YAML，或让自己的脚位于该方块坐标再执行 `/pitsetup locs loc.region.enchant`。直接站在方块顶面会产生高度差。旧 `loc enchantTable`、`loc pit loc1`、`loc pit loc2` 都引用了旧字段；安全区请使用上文的 `loc.region.pitA` 和 `loc.region.pitB`。

### 事件与其他地图点位

先确认事件需要的是一个中心、多个候选位置，还是完整区域。下表中的单点可用 `/pitsetup locs <字段>` 覆盖；列表可在每个候选位置执行一次 `/pitsetup quickadd <字段>`。旧 `listloc` 命令访问的是 `list.*`，会无响应或报告成功却没有改到游戏使用的字段。

| 地图内容 | 怎么设、怎么用 | 对应旧快捷命令（接在 `/pitsetup` 后） |
| --- | --- | --- |
| 随机出生 | `loc.spawn` 是列表，每个点都要适合玩家落脚。 | `listloc add spawn` |
| 龙蛋 | 单点 `loc.events.egg.loc` 决定首次生成位置。站在预留给龙蛋的格子执行 `/pitsetup quickadd loc.events.egg.loc`。 | `loc egg` |
| 占山为王 | 在每个可放置占领圈的平台中心追加到 `loc.events.koth.locs`。事件会随机选一个。 | `listloc add koth`、`listloc setup koth`、`listloc reset koth` |
| 高梯之王 | 在允许生成梯塔的地面追加 `loc.leader`。当前版本未注册这个事件；如自行启用，事件从列表随机选一个点建塔。 | `listloc add leader` |
| 空投、下水道宝箱 | 空投箱的候选格追加到 `loc.packages`；下水道箱的候选格追加到 `loc.sewers.chests`。两者是独立列表。 | `listloc setup packages`、`listloc reset packages`、`listloc setup sewer` |
| 旗帜争夺战、方块划地战 | 旗帜所在的格子追加到 `loc.squads`，该事件当前未注册。方块划地战已注册，可拾取增益的点追加到 `loc.blockHead`。 | `listloc add squads`、`listloc add blockHead` |
| 幸运轮盘 | 每个候选点追加到 `loc.Event.LuckyWheel`。保留这里的大小写。 | `listloc add luckyWheel` |
| 天坑外卖 | 用 `loc.events.hamburger.villager.a-offer` 设置店主及建筑中心，在各送餐地点追加 `loc.events.hamburger.villager.a`。模板另有单点 `loc.events.hamburger.shop`；事件粘贴建筑时读取 `a-offer`。 | `listloc add hamburger a`、`listloc setup hamburger`、`listloc reset hamburger`、`listloc clear hamburger a` |
| 尖塔夺魁 | 基点用 `loc.events.spire.spireLoc`，各层中心按从低到高的顺序填入 `loc.events.spire.spireFloorLocations`；门和入口的设置见下文。 | `loc spireSpawn`、`listloc add spireFloor` |
| 创世阵营出生 | 恶魔阵营候选点追加到 `loc.Genesis.demon`。天使阵营的 `angelSpawns` 在当前源码缺少配置路径，只能先修复该字段的加载与保存后再布置。天使 NPC 本身可照常设点。 | `listloc add demonSpawns`、`listloc add angelSpawns` |
| 红包大战 | 将可生成红包方块的位置追加到 `loc.event.randomred`。另一个已声明的列表 `loc.events.RandomRedEnvelopesEvent` 没有被这个事件读取。 | 使用完整字段 |
| 音乐点、调试点 | `music.loc` 是预留的坐标列表。调试单点 `debug.infinityNpcLoc`、`debug.ienchantNpcLoc` 可直接用 `locs` 设置，手改时保留模板中的 Bukkit Location 对象格式。 | 使用完整字段 |

**例一：占山为王。** 在 `config.yml` 的 `loc → events → koth` 下添加名为 `locs` 的列表，注意末尾的 `s`。每项填写本服地图上的一个平台中心。下面的示例配置了一个候选点。

```yaml
loc:
  events:
    koth:
      locs:
        - "pit_world|30.5|65|10.5|0|0"
```

1. 执行 `/pitsetup locs tp loc.events.koth.locs 0`，确认平台位置。移动到第二个平台中心，执行 `/pitsetup quickadd loc.events.koth.locs`，再用索引 `1` 复查。
2. 在测试服执行 `/startevent 占山为王 true -1`，到选中的平台检查占领圈。结束测试可用 `/startevent cancel`，再检查地形。要清除旧候选点，按索引从后向前删除，或停服后替换 YAML 中的整个列表。

**例二：疯狂天坑与尖塔。** 两个事件共用天坑门，先进入地图并在门结构粘贴原点执行 `/pitsetup locs loc.region.doorCenter`。大型事件的结构文件放在 `plugins/ThePitPremium/schem/`，结构的粘贴原点要与地图上选定的位置对应。

| 步骤 | 操作 | 验收 |
| --- | --- | --- |
| 疯狂天坑围墙 | 在圆形场地底部中心执行 `/pitsetup quickadd loc.events.rage.middle`。停服编辑 `loc.events.rage.radius` 和 `loc.events.rage.height`，分别填正整数半径和高度，单位为格。准备 `pit_door.schem`。 | 测试事件期间检查玻璃围墙的底部、边缘和门，结束后检查复原。 |
| 尖塔建筑与传送门 | 准备 `spire.schem` 和 `pit_door.schem`。在塔结构原点执行 `/pitsetup locs loc.events.spire.spireLoc`。在传送门范围的两个对角分别执行 `/pitsetup locs loc.portal.posA` 和 `/pitsetup locs loc.portal.posB`。两角必须在同一世界，选区内空气会被填为传送门。 | 让入口覆盖预定的传送方格，检查是否误选到上下楼层。 |
| 尖塔层中心 | 玩家进入时从索引 `0` 开始，最高可达索引 `9`，需要 **10 个有效点**。在测试地图按层测量，用 `/pitsetup quickreplace loc.events.spire.spireFloorLocations 0` 等命令逐项替换旧值，再用 `quickadd` 补足缺项。随附模板只有 9 项。 | 首先传送检查索引 `0` 和 `9`，再逐层核对。玩家实际会分散传送到该层中心周围，落脚区域也要留在塔内。 |

**例三：天坑外卖。** 准备 `hamburger.schem`。在店主处执行 `/pitsetup locs loc.events.hamburger.villager.a-offer`；在每个可步行到达的收餐位置执行 `/pitsetup quickadd loc.events.hamburger.villager.a`。保留至少一个有效的出生点和一个收餐位置。测试时核对餐厅粘贴位置、顾客是否出现，以及玩家能否从店主处跑到顾客处完成一单。

**例四：蛋糕争夺战。** 停服编辑 `loc.events.cake.regions`。每个列表元素是一对方块对角，格式为 `世界名|X|Y|Z`，用来框出一整块可被蛋糕填充的空间。事件从列表随机选择一个区域，并在服务器**第一个加载的世界**放置方块。请在该世界测量坐标，至少配置一组，所选空间留给事件使用。

```yaml
loc:
  events:
    cake:
      regions:
        - posA: "pit_world|30|65|30"
          posB: "pit_world|34|68|34"
        - posA: "pit_world|-24|65|12"
          posB: "pit_world|-20|68|16"
```

每个区域的 `posA` 和 `posB` 要写在同一个列表项中。重启后在测试服执行 `/startevent 蛋糕争夺战 true -1`，核对蛋糕生成的世界和区域边界。

### 开服验收与排错

| 现象 | 怎么查 |
| --- | --- |
| 提示设点成功，重启后还在原处 | 先核对完整字段的大小写，在游戏中用 `locs tp` 查内存位置，再检查服务端 `plugins/ThePitPremium/config.yml` 中的值。旧快捷命令可能报告成功却没有写入。手改文件后完整重启。 |
| 显示“位置不存在”或列表没有条目 | 单点省略索引，列表从 `0` 开始。核对 YAML 的缩进、列表前的 `-`、世界名和世界加载状态。若新增第一项一直无效，停服将字段初始化为 `[]` 后再设点。 |
| 随机出生或事件偶尔出现在错误位置 | 检查候选列表中每一项的世界名和坐标。`quickadd` 会在列表末尾追加位置。移除模板中的示例点后，逐个按索引传送复查。 |
| NPC 坐标正确但实体未移动，或文字还在原处 | 核对 NPC 与全息文字各自的字段。游戏内改动会保存，已生成的实体和文字可能要重启才会按新位置创建。 |
| 高梯之王、旗帜争夺战已设点，仍无法开启 | 当前版本的事件注册表注释掉了这两个实现。有意开放时，需要在源码中恢复对应注册、重新构建部署，再测试活动。见[事件注册表](src/main/java/cn/charlotte/pit/PitHook.java)。 |
| 大型事件准备失败 | 查看控制台对应事件的准备错误，核对世界、出生点、门、候选点以及 `.schem` 的目录和文件名。一次只在测试地图开启一个事件，等上一个事件结束并恢复地图后再测下一个。 |

字段和命令跟随当前源码版本。[地图配置字段](src/main/java/cn/charlotte/pit/config/PitConfig.java) · [设点命令实现](src/main/java/cn/charlotte/pit/commands/admin/Setup.java) · [标记箭与临时全息实现](src/main/java/cn/charlotte/pit/setup/LocationsSetup.java)

## 命令说明

下表中 `<参数>` 为必填项，`[参数]` 为可选项。

### 玩家命令

| 命令 | 用途与条件 |
| --- | --- |
| `/spawn` | 返回随机出生点并清除当前连杀和赏金；战斗中无法使用。别名为 `/respawn`、`/home`、`/back`。 |
| `/option`、`/recap` | 分别打开个人设置、查看最近的战斗回顾。 |
| `/view <玩家>` | 查看玩家档案。需要 70 级或至少一次精通；离线目标仅 OP 可查。 |
| `/trade <玩家>` | 向对方发起交易；若对方已发来请求，同一命令用于接受。一般需达到 60 级、脱离战斗并位于允许交易的区域。 |
| `/trade tradeGui`、`/trade limits` | 打开玩家交易界面、查看今天的交易次数和金额限额。 |
| `/trade offer <玩家> <价格>`、`/trade viewOffer <卖家>` | 为手持物品向指定玩家报价；买家查看报价并确认购买。交易关闭后，卖家可用 `/trade offer` 取回未结算物品。 |
| `/trade gui` | 系统拍卖事件期间打开竞拍界面。玩家之间的交易界面使用 `/trade tradeGui`。 |
| `/events`、`/show`、`/showgold` | 分别预览事件、展示手持物品、展示金币。仅增值包玩家或工作人员可用。 |
| `/cool` | 解锁对应天赋后列出在线精通玩家。 |
| `/kingquest`、`/bakemaster` | 打开国王任务、烘焙界面；需要权限 `pit.kingquest.ui`。 |

### 管理员常用命令

以下命令需要 `pit.admin`。地图设点和菜单命令由游戏内玩家执行。

| 命令 | 用途 |
| --- | --- |
| `/pitsetup locs list single`、`/pitsetup quickadd <配置路径>` | 查看已设普通单点；将脚下的居中坐标写入单点或追加到列表。常用字段支持 Tab 补全。 |
| `/pitsetup quickreplace <配置路径> [索引]`、`/pitsetup reloadnpc` | 替换坐标或按内存配置尝试生成 NPC。列表索引从 0 开始，省略或越界会追加。 |
| `/startevent <事件名> <true或false> <秒数>` | 普通事件立即替换正在运行的普通事件。测试大型事件可用 `/startevent 尖塔夺魁 true 30` 设 30 秒预告，填 `-1` 使用默认 300 秒。开启个人伤害调试信息的管理员传 `false` 会跳过预告。场地准备完成后才开赛。 |
| `/startevent cancel` | 取消大型事件并结束当前普通事件。若大型事件日志提示恢复失败，修复日志指出的问题后再次执行，可重试清理和地图恢复。 |
| `/pititems itemlist`、`/pititems give <在线玩家> <物品类型> <数量>` | 浏览插件物品、发放指定物品。别名为 `/pi`。 |
| `/modify coin <set或add或take> <在线玩家> <金额>` | 设置、增加或扣除游戏金币。外部点券余额由点券插件管理。 |
| `/openmenu <菜单>`、`/trashbin open <在线玩家>` | 为自己打开指定菜单（如 `shop`、`quest`、`mail`），或为目标玩家打开垃圾桶。 |
| `/scan`、`/scan uuid`、`/scan uuid status`、`/scan uuid view [玩家名或物品UUID]` | 查看排查命令帮助、启动重复物品扫描、查询进度、查看结果。 |
| `/pitbot pos1 training`、`/pitbot pos2 training`、`/pitbot add` | 在脚下设置训练区两个对角，或添加野外刷新点。`/pitbot list` 列出刷新点，`/pitbot count` 查看数量。 |
| `/pitbot status`、`/namehide status` | 分别检查训练 Bot 和头顶名字隐藏功能是否生效。 |
| `/pitpack <在线玩家>`、`/forceprestige <在线玩家>` | 发放天坑增值包，或为达到 120 级的目标玩家执行精通。 |

完整注册列表见 [PitCommands.java](src/main/java/cn/charlotte/pit/commands/PitCommands.java)，各子命令的参数和权限见对应的命令类。
