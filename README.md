# 短信充值卡助手（SmsRechargeCard）

一个安卓 APP：自动接收手机短信，并可在用户授权后监听 APP 通知；两类消息都按后台随“设备号校验”结果下发的**交易模式**规则解析：短信消息依次尝试所有短信模式（notify_type=0），APP 通知依次尝试所有 APP 通知模式（notify_type=1），每个模式依次执行“信息来源判断 → 交易信息判定正则 → 交易号提取正则”，直至某个模式命中；交易号与交易金额由模式正则从消息正文提取，充值卡号则在去除空格后的交易号上按模式正则提取；每条命中的消息都会保存本地记录并调用后台接口，同时按最近 1 小时记录标记为新消息、重复消息或重复交易号。所有本地记录会注明消息源自短信还是 APP 通知，并支持在 APP 内查看、按日期筛选以及按天数批量清除历史消息。APP 首次使用需先通过设备号校验，校验通过前不会启动短信/APP通知拦截功能。APP 内所有页面文案均为英文（`res/values/strings.xml`），本说明文档使用中文撰写。

## 一、如何打开工程

1. 打开 Android Studio（建议 2023.1 Iguana 及以上版本）。
2. 选择 `Open`，选中本项目根目录 `SMSTORECHARGE` 文件夹。
3. 如果提示缺少 Gradle Wrapper jar，点击 Android Studio 弹出的提示自动生成即可（或点击右上角"Sync Project with Gradle Files" 🐘图标），Android Studio 会根据 `gradle/wrapper/gradle-wrapper.properties` 中配置的 Gradle 8.4 自动下载配置，首次同步需要联网。
4. 同步完成后，连接安卓真机（**需要真机测试短信功能和通知监听，模拟器一般无法接收真实短信**），点击运行即可安装。

工程基本信息：
- 开发语言：Kotlin
- minSdk 26（Android 8.0），targetSdk / compileSdk 34
- 依赖：AndroidX、Room（本地数据库）、WorkManager（后台任务与失败重试）、Retrofit + OkHttp（网络请求）、ViewBinding

## 二、核心功能与实现说明

### 0. 设备号校验（首次使用）
- `ui/DeviceIdActivity.kt` 为设备号验证界面：APP 首次打开时（本机尚未保存过校验通过的设备号）会强制跳转到该界面，要求用户输入设备号，同时保持在该界面，不会启动短信/APP通知拦截功能（`ui/MainActivity.kt`、`receiver/SmsReceiver.kt`、`service/RechargeNotificationListenerService.kt` 均会先检查设备号是否已绑定）。
- 点击“验证并保存”后调用后台接口 `checkDeviceIdValid.do`（地址内置在 APP 中，固定为 `https://test.myshequ.cn:8445/otherService/checkDeviceIdValid.do`），请求体为 `{"deviceId": "输入的设备号"}`。
- 返回结果的 `return_code` 为 `SUCCESS` 时，将设备号、卖家ID（`seller_id`）和交易模式列表（`list`）一并保存到本地（`util/AppSettings.kt`），并跳转到主界面；若返回中不含交易模式列表（`list` 字段），则保留本地已保存的交易模式。`return_code` 不为 `SUCCESS` 时在当前输入界面提示错误消息（取返回的 `return_message`，取不到则用默认提示文案），不保存设备号，也不会离开该界面。
- 交易模式为解析短信/APP通知的唯一规则来源：「设置」页面以按钮形式展示所有模式名称，点击后展示该模式详情（不提供修改入口）；后台规则更新后，重新绑定设备号即可从后台同步。
- 已绑定设备号后，可在「设置」页面查看当前设备号，并通过“重新绑定设备号”按钮重新进入该界面修改。

### 1. 短信/APP通知接收与交易模式匹配
- `receiver/SmsReceiver.kt` 监听系统广播 `android.provider.Telephony.SMS_RECEIVED`。
- `service/RechargeNotificationListenerService.kt` 使用 `NotificationListenerService` 监听 APP 通知，需要用户在系统“通知使用权/通知访问权限”中手动授权。
- 收到短信时，依次尝试所有短信类型（`notify_type=0`）的交易模式；收到 APP 通知时，依次尝试所有 APP 通知类型（`notify_type=1`）的交易模式。单个模式的判定顺序：
  1. **信息来源判断**（`trade_info_source`）：模式配置的来源片段用英文逗号分隔，消息来源（短信发送号码，或 APP 通知的 APP 名称/包名）包含任一片段即通过（不区分大小写）；配置留空表示不限制来源。
  2. **交易信息判定**（`trade_judge_rule`）：对消息正文执行该正则，匹配到即视为该模式命中，停止遍历后续模式；配置留空表示不判定、直接通过。
- 若某个模式命中了交易信息判定，但交易号或充值卡号未能提取，会记录“命中但未提取”的原因并按命中处理，不再继续尝试其他模式。
- 所有模式均未命中时，按未命中记录原因（原因中会包含各模式的来源/判定跳过说明）。

### 2. 交易号 / 充值卡号 / 交易金额提取
- `util/SmsParser.kt` 按命中的交易模式规则提取：
  1. 交易号：按 `trade_no_parse_rule1` 正则从消息正文提取；`trade_no_parse_rule2` 非空时，再对 rule1 的提取结果执行 rule2 二次提取，最终以 rule2 的结果为准。
  2. 充值卡号：在提取出的交易号基础上解析——先去掉交易号中的空格（含全角空格），再在去空格后的交易号上使用 `card_no_parse_rule1` / `card_no_parse_rule2` 以同样方式提取；交易号未提取出时卡号不再解析。
  3. 交易金额：同上，使用 `mony_parse_rule1` / `mony_parse_rule2`；未配置规则或未提取到时金额为空字符串，不影响解析判定与后续流程。
  4. 每条正则优先取第一个非空捕获组作为提取结果；无捕获组时取整段匹配内容；结果会去除首尾空白。
- 交易号与充值卡号均提取成功才算“解析成功”；两者任一为空则记录为“已保存未处理”，APP 仍会保存消息内容、失败原因，并调用后台接口用于消息保存。请在「设置」页面用真实短信或通知内容点击"测试解析"验证效果（可分别选择短信与 APP 通知类型进行测试）。

### 3. 调用后台接口
- 充值卡接口地址（内置在 APP 中，不可修改）：`https://test.myshequ.cn:8445/otherService/smsToRechargeCard.do`
- 请求方式：POST JSON，请求体见 `network/RechargeRequest.kt`：
  ```json
  {
    "cardNo": "充值卡号（由模式 card_no_parse_rule 在去空格后的交易号上提取）",
    "orderNo": "交易号（由模式 trade_no_parse_rule 正则提取）",
    "phone": "来源标识（短信发送号码或APP名称+包名）",
    "smsContent": "短信或APP通知原文",
    "smsTime": "消息接收时间戳(毫秒)",
    "messageType": "消息类型：0-新消息，1-重复消息，2-重复交易号",
    "money": "交易金额（由模式 mony_parse_rule 正则提取），未解析到时为空字符串",
    "deviceId": "本机已绑定的设备号"
  }
  ```
- 接口返回结构以 `return_code` 字段为准：`return_code` 为 `SUCCESS` 时表示本条消息处理成功，否则为失败，错误提示信息优先取 `return_message`。失败时会将错误消息保存到该条本地记录的处理结果字段（`apiMessage`）中，可在 APP「记录详情」页面查看。
- 解析逻辑统一实现在 `network/RechargeResultParser.kt`（`checkDeviceIdValid.do` 和 `smsToRechargeCard.do` 均复用）：优先识别 `return_code`/`return_message`，若接口未返回该字段则降级尝试 `success`/`code`/`status`/`message`/`msg` 等常见字段，再否则仅以 HTTP 状态码判断，原始返回内容始终保存供排查。交易模式列表的解析实现在同文件的 `parseTradeModes` 函数中（读取 `seller_id` 与 `list` 字段）。
- 如果实际接口的字段名、认证方式（如需要 token/签名）与以上假设不同，只需修改 `RechargeRequest.kt`、`DeviceIdCheckRequest.kt` 和 `RechargeResultParser.kt` 即可对接。

### 4. 后台任务、去重与接口调用
- `worker/SmsProcessWorker.kt` 使用 WorkManager 处理"去重判定 + 写入本地记录 + 调用接口 + 更新结果"，避免在广播接收器或通知监听服务中做耗时网络操作被系统中断。
- 收到短信或 APP 通知时，会先判断最近 1 小时内是否存在相同消息内容；如存在，标记为 `messageType=1`（重复消息）。
- 若不是重复消息且已解析出交易单号，会继续判断最近 1 小时内是否存在相同交易单号；如存在，标记为 `messageType=2`（重复交易号）。
- 以上均不命中时标记为 `messageType=0`（新消息）。
- 所有命中的消息都会调用后台接口，携带 `messageType` 供后台保存和区分处理。消息已经入库后，接口异常不会触发 WorkManager 重试，避免重试导致本地重复写入。

### 5. 本地记录与查询
- 使用 Room 数据库（`data/AppDatabase.kt`），每条记录包含：消息来源（短信/APP通知）、消息类型（新消息/重复消息/重复交易号）、来源标识（短信发送号码或APP名称+包名）、原始内容、接收时间、交易号、充值卡号、交易金额、处理状态（处理中/成功/失败/已保存未处理）、接口返回信息。
- 主界面（`ui/MainActivity.kt`）：
  - 列表展示全部本地记录（默认按时间倒序）；
  - 支持选择"开始日期"和"结束日期"后点击"查询"按日期范围筛选，点击"查看全部"恢复展示所有记录；
  - 点击某条记录可查看详情（短信原文、接口返回原文等）。

### 5.1 清除消息
- 主界面工具栏新增“清除消息”入口（`R.menu.menu_main` 中的 `action_clear_messages`）。
- 点击后弹出对话框，要求输入“多少天以前”的天数（正整数）；点击“清除”后按 `接收时间 < 当前时间 - 输入天数` 的条件批量删除本地记录（`data/SmsRecordDao.kt` 的 `deleteBefore` 方法），并 Toast 提示实际清除的条数；点击“取消”不做任何操作。
- 未输入或输入非正整数天数时会提示“请输入大于0的有效天数”，不会执行删除。
- 由于列表数据来源于 Room 的 `LiveData`，删除后界面会自动刷新，无需手动重新查询。

### 6. 权限
需要在设置中手动授权，或点击主界面顶部黄色提示条的"立即授权"按钮：
- `RECEIVE_SMS`：接收短信广播
- `READ_SMS`：读取短信内容
- 通知监听权限：在「设置」页面点击“打开通知监听权限”，跳转系统页面后手动开启本 APP 的通知访问权限。
- 设备号校验：APP 首次使用必须先完成设备号校验并保存，否则主界面与拦截消息功能均不可用。
- 短信权限是 Android 运行时敏感权限；通知监听权限无法通过普通运行时弹窗授予，必须由用户在系统设置页开启。

## 三、需要您重点确认/调整的地方

| 项目 | 当前默认值 | 修改方式 |
|---|---|---|
| 交易模式（解析规则） | 由后台随设备号校验结果下发（`name`/`notify_type`/`trade_info_source`/`trade_judge_rule`/`trade_no_parse_rule1`/`trade_no_parse_rule2`/`card_no_parse_rule1`/`card_no_parse_rule2`/`mony_parse_rule1`/`mony_parse_rule2`） | 后台配置；APP「设置」页面仅展示，重新绑定设备号即可同步更新 |
| 后台充值卡接口地址 | `https://test.myshequ.cn:8445/otherService/smsToRechargeCard.do` | 需要改代码：`util/AppSettings.kt` |
| 设备号校验接口地址 | `https://test.myshequ.cn:8445/otherService/checkDeviceIdValid.do` | 需要改代码：`util/AppSettings.kt` |
| 接口请求参数字段 | cardNo/orderNo/phone/smsContent/smsTime/messageType/money/deviceId | 需要改代码：`network/RechargeRequest.kt` |
| 接口返回结构判断字段 | 以 `return_code`==`SUCCESS` 为准，兼容 success/code/status/message/msg 等常见字段 | 需要改代码：`network/RechargeResultParser.kt` |

建议第一次真机安装后：
1. 完成设备号校验后，在「设置」页面确认后台下发的交易模式按钮与规则详情是否正确；
2. 如需识别 APP 通知，在「设置」页面点击“打开通知监听权限”，在系统页面手动开启本 APP 的通知访问权限；
3. 选择对应的消息类型（短信/APP通知），粘贴一条真实的交易短信或通知文本，点击"测试解析"，确认能正确解析出交易号与充值卡号；
4. 确认后台接口的真实参数与返回结构后，告知我以便同步调整 `RechargeRequest`、`DeviceIdCheckRequest` 和 `RechargeResultParser`，确保接口对接完全准确。

## 四、后续可选优化（如有需要可再提出）
- 部分国产手机（小米/华为/OPPO/vivo 等）存在应用后台管控，建议提示用户关闭该 APP 的电池优化/自启动限制，以保证 APP 被系统杀死后仍能正常接收短信广播（`RECEIVE_SMS` 广播由系统触发，一般不受限，但个别厂商 ROM 会额外限制未启动过的应用接收广播）。
- 已内置最近 1 小时去重：相同消息内容标记为重复消息，相同交易单号标记为重复交易号，并通过 `messageType` 传递给后台。
