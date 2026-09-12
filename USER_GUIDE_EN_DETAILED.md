# SMS Recharge Card Assistant User Guide (English Detailed Version)

## 1. Overview

SMS Recharge Card Assistant automatically detects transaction information from SMS messages and APP notifications. It parses messages based on the source whitelist, message keywords, Order No. keyword, and Order No. length. After successful parsing, the last 6 digits of the Order No. are used as the recharge card number, and the message record is submitted to the backend API.

Main features:

- Device ID verification on first launch.
- Automatic SMS detection.
- APP notification detection after user authorization.
- Order No., Card No., and Amount parsing.
- New message, duplicate message, and duplicate Order No. marking.
- Local message record viewing.
- Date-range filtering.
- History cleanup by days.
- Device ID rebinding.

## 2. First Launch and Device ID Verification

1. Install and open the app.
2. If no verified device ID is saved on this phone, the app opens the Device ID verification screen.
3. Enter the assigned device ID.
4. Tap “Verify and Save”.
5. The app calls the backend `checkDeviceId.do` API.
6. If verification succeeds, the device ID is saved locally and the app enters the main screen.
7. If verification fails, the error message is displayed and the app stays on the verification screen.

Notes:

- Before the device ID is verified, SMS interception will not start.
- Before the device ID is verified, APP notification interception will not start.
- If verification fails, check whether the device ID is correct or contact the backend administrator.

## 3. Enable SMS Detection Permissions

1. On the main screen, tap “Grant Now” if a permission banner is shown.
2. Allow the following permissions when the system prompts:
   - Receive SMS.
   - Read SMS.
3. After authorization, the app can automatically receive and process matching SMS messages.

Notes:

- If SMS permissions are denied, the app cannot read SMS content automatically.
- Some Android vendors may require manual permission checks in system settings.

## 4. Enable APP Notification Detection

To detect notifications from apps such as WeChat or other supported applications, enable notification listener permission.

Steps:

1. Open the app “Settings” screen.
2. Tap “Open Notification Listener Permission”.
3. The system opens the notification access settings page.
4. Find this app and enable notification access.
5. Return to the app. Notification detection will then be available.

Notes:

- Notification listener permission must be enabled manually in Android system settings.
- Android does not allow this permission to be granted through a normal runtime permission dialog.
- If notifications are not recorded, check both the system permission and the parsing rules.

## 5. Configure Parsing Rules

Open the “Settings” screen to configure the following items.

### 5.1 Source whitelist

The source whitelist limits which SMS senders, APP names, or APP package names can be processed.

Rules:

- Separate multiple sources with English commas.
- An SMS passes if its sender number contains any configured source fragment.
- An APP notification passes if the APP name or package name contains any configured source fragment.
- Leave it blank to allow all sources.

Example:

```text
WECHAT,微信,com.tencent.mm
```

### 5.2 Message content filter keywords

Message content filter keywords decide whether a message should be processed.

Rules:

- Separate multiple keywords with English commas.
- All keywords must be present in the message content.
- Matching is case-insensitive.

### 5.3 Order No. Keyword

The app searches for this keyword in the message. After the keyword is found, the app extracts a fixed-length string after it as the full Order No.

Example:

```text
交易单号
```

### 5.4 Order No. Length

This value defines the length of the full Order No. extracted after the Order No. keyword.

Example:

```text
20
```

After successful parsing, the app uses the last 6 digits of the full Order No. as the Card No.

### 5.5 Amount keyword

The app extracts the number after this keyword as the transaction amount.

Notes:

- Integers are supported.
- Decimal numbers are supported.
- If the amount cannot be parsed, the amount value is left empty and the message can still be saved and submitted.

### 5.6 Backend API URL

This is the recharge card backend API URL. When a message is detected, the app submits the message content, Order No., Card No., amount, device ID, and message type to this API.

### 5.7 Device ID Verification API URL

This is the API used for first-time Device ID verification and Device ID rebinding.

## 6. Save Settings

1. Edit the settings on the “Settings” screen.
2. Tap “Save”.
3. After the success message is shown, the new settings take effect.

If the Order No. keyword is empty, or if the Order No. length is not a number greater than 0, the app shows an error and does not save invalid settings.

## 7. Test Parsing

Use “Test Parse” before relying on real message detection.

Steps:

1. Open the “Settings” screen.
2. Paste a real SMS or APP notification content into the test input box.
3. Tap “Test Parse”.
4. The app displays the parsing result.

The parsing result may include:

- Whether the source and keywords are matched.
- Whether the Order No. is parsed.
- The full Order No.
- The last 6 digits used as the Card No.
- The transaction amount.
- The next processing action.
- The failure reason if parsing fails.

If parsing fails, check:

1. Whether the source whitelist matches the sender number, APP name, or APP package name.
2. Whether all message keywords are present in the content.
3. Whether the Order No. keyword matches the actual message content.
4. Whether the Order No. length is correct.
5. Whether the Amount keyword matches the actual amount prefix.

## 8. Message Processing and Backend Submission

When the app receives an SMS message or APP notification, it processes it in this order:

1. Check whether the device ID is verified.
2. Check whether the message source matches the source whitelist.
3. Check whether the message content contains all configured keywords.
4. Try to parse the Order No.
5. Try to parse the Amount.
6. Check whether the same message content exists within the last 1 hour.
7. Check whether the same Order No. exists within the last 1 hour.
8. Save the local record.
9. Call the backend recharge card API.
10. Save the backend API response.

Message type values:

- `0`: New message.
- `1`: Duplicate message. The same message content exists within the last 1 hour.
- `2`: Duplicate Order No. The same Order No. exists within the last 1 hour.

## 9. View Message List

The main screen displays local message records in descending order by received time.

A record may show:

- Received time.
- Message source.
- Message type.
- Sender/source identifier.
- Order No.
- Card No.
- Amount.
- Processing status.

Processing status values:

- Processing.
- Success.
- Failed.
- Saved, Not Processed.

## 10. Filter Records by Date

1. Tap “Start Date” and select a start date.
2. Tap “End Date” and select an end date.
3. Tap “Query”.
4. The app shows records within the selected date range.
5. Tap “View All” to display all records again.

## 11. View Record Details

1. Tap any record on the main screen.
2. The detail screen shows the complete message content and backend API response.
3. If backend submission failed, the error details can be checked on this screen.

The detail screen may include:

- Original message content.
- Message source.
- Message type.
- Order No.
- Card No.
- Amount.
- API response.
- Processing status.

## 12. Clear History Records

The app supports clearing old local records by days.

Steps:

1. Tap “Clear Messages” on the main screen toolbar.
2. Enter the number of days in the dialog.
3. For example, enter `7` to clear messages older than 7 days.
4. Tap “Clear”.
5. The app deletes matching local records.
6. After deletion, the app shows the actual number of cleared records.

Notes:

- Only numbers greater than 0 are valid.
- Empty input or a value less than or equal to 0 will not trigger deletion.
- Only local records are deleted. Backend data is not deleted.
- The message list refreshes automatically after deletion.

## 13. Rebind Device ID

If the device ID needs to be changed, use the rebind function.

Steps:

1. Open the “Settings” screen.
2. Check the current device ID.
3. Tap “Rebind Device ID”.
4. Enter the new device ID.
5. Tap “Verify and Save”.
6. After successful verification, the new device ID is saved locally.

## 14. FAQ

### 14.1 Why does the app stay on the Device ID screen?

The device ID has not been verified successfully. Check whether the device ID is correct or contact the backend administrator.

### 14.2 Why are there no SMS records?

Check:

- Whether SMS permissions are granted.
- Whether the sender matches the source whitelist.
- Whether the SMS content contains all configured keywords.
- Whether the device ID has been verified.

### 14.3 Why are there no APP notification records?

Check:

- Whether notification listener permission is enabled.
- Whether the APP name or package name matches the source whitelist.
- Whether the notification content contains all configured keywords.
- Whether the device ID has been verified.

### 14.4 Why is the Order No. not parsed?

Check whether the Order No. keyword and Order No. length match the real message content.

### 14.5 Why is the Amount not parsed?

Check whether the Amount keyword matches the amount prefix in the real message content.

### 14.6 Why does a record show Failed?

The backend API may have returned an error, or a network error may have occurred. Open the record detail page to check the API response.

### 14.7 Can cleared messages be restored?

No. Clearing messages deletes records from the local database. Use this feature carefully.
