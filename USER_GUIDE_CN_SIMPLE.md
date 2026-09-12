# 短信充值卡助手操作指南（中文简化版）

## 1. 首次打开 APP

1. 安装并打开 APP。
2. 首次使用时，APP 会要求输入设备号。
3. 输入设备号后点击“Verify and Save”。
4. 验证成功后进入主界面；验证失败时请检查设备号后重新输入。
5. 设备号未验证成功前，APP 不会拦截短信或 APP 通知。

## 2. 开启短信权限

1. 进入主界面后，如果顶部出现权限提示，请点击“Grant Now”。
2. 按系统提示允许“接收短信”和“读取短信”权限。
3. 授权后，APP 才能自动识别符合规则的短信。

## 3. 开启 APP 通知监听

1. 进入“Settings”页面。
2. 点击“Open Notification Listener Permission”。
3. 在系统通知访问权限页面中，开启本 APP 的通知访问权限。
4. 授权后，APP 可以识别微信等 APP 的通知内容。

## 4. 配置解析规则

进入“Settings”页面，可配置以下内容：

- Source whitelist：来源白名单。
- Message content filter keywords：消息正文关键词。
- Order No. Keyword：交易单号关键字。
- Order No. Length：交易单号长度。
- Amount keyword：交易金额关键字。
- Backend API URL：后台充值卡接口地址。
- Device ID Verification API URL：设备号校验接口地址。

配置完成后点击“Save”。

## 5. 测试解析

1. 在“Settings”页面粘贴一条真实短信或 APP 通知内容。
2. 点击“Test Parse”。
3. 查看是否能正确识别交易单号、充值卡号和金额。
4. 如果解析失败，请检查来源白名单、关键词、交易单号关键字和交易单号长度。

## 6. 查看消息记录

1. 主界面会显示已识别的短信或 APP 通知记录。
2. 可选择开始日期和结束日期后点击“Query”筛选记录。
3. 点击“View All”恢复显示全部记录。
4. 点击单条记录可查看详情和后台接口返回结果。

## 7. 清除历史消息

1. 在主界面点击“Clear Messages”。
2. 输入要清除多少天以前的消息。
3. 点击“Clear”。
4. APP 会删除对应的本地历史记录，并提示实际清除数量。

## 8. 重新绑定设备号

1. 进入“Settings”页面。
2. 点击“Rebind Device ID”。
3. 输入新的设备号。
4. 点击“Verify and Save”，验证成功后完成重新绑定。
