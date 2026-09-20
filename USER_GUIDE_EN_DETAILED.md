# SMS Recharge Card Assistant User Guide (English Detailed Version)

## 1. Overview

SMS Recharge Card Assistant automatically detects transaction information from SMS messages and APP notifications. Parsing is driven by backend-delivered “Trade Modes”: for SMS messages the app tries all SMS modes in order, and for APP notifications it tries all APP notification modes in order. Each mode runs “source check → trade judge regex → Order No. extraction” until a mode matches; the Order No. and the Amount are extracted from the message content, the Card No. is extracted from the Order No. with spaces removed, and the message record is submitted to the backend API.

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
5. The app calls the backend `checkDeviceIdValid.do` API.
6. If verification succeeds, the device ID, the seller ID, and the backend-delivered trade modes (parsing rules) are saved locally, and the app enters the main screen.
7. If verification fails, the error message is displayed and the app stays on the verification screen.

Notes:

- Before the device ID is verified, SMS interception will not start.
- Before the device ID is verified, APP notification interception will not start.
- If verification fails, check whether the device ID is correct or contact the backend administrator.
- Trade modes are delivered together with the device ID verification result; rebind the device ID to sync updates after the backend changes them.

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

## 5. View Parsing Rules (Trade Modes)

Parsing rules are configured on the backend. They are delivered to the app automatically after the device ID is verified successfully and are shown on the “Settings” screen as read-only.

The “Settings” screen shows:

### 5.1 Trade Mode buttons

- The “Trade Modes” section lists one button per backend-delivered trade mode.
- Tap a mode button to show its full rule details in the “Mode Details” section below.
- The first mode is selected by default.
- If no trade modes are available yet, the app shows “No trade modes yet, please rebind the device ID to fetch them”.

### 5.2 Mode detail fields

- Name: the mode name.
- Notify Type: SMS or APP Notification.
- Trade Info Source: source check; comma-separated fragments, the message source passes if it contains any fragment; blank means no source restriction.
- Trade Judge Rule: regex that decides whether the message is a trade message; the mode matches only when the content matches; blank means no judge.
- Trade No. Parse Rule 1 / 2: Order No. extraction regexes; when Rule 2 is set, Rule 1 is applied first and Rule 2 is then applied to the Rule 1 result.
- Card No. Parse Rule 1 / 2: Card No. extraction regexes applied to the Order No. with spaces removed; the same two-step behavior applies, and the Card No. is not parsed when the Order No. is missing.
- Amount Parse Rule 1 / 2: amount extraction regexes with the same two-step behavior; when no rule is set or nothing is extracted, the amount is empty and does not affect the Order No./Card No. parsing and submission.
- Unset fields are shown as “(not set)”.

### 5.3 Matching order (backend parsing logic)

1. SMS messages: the app tries every trade mode whose Notify Type is SMS, in order.
2. APP notifications: the app tries every trade mode whose Notify Type is APP Notification, in order.
3. Each mode runs the source check first, then the trade judge check; once the judge passes, the mode is considered matched and no further modes are tried.
4. After a match, the Order No. is extracted from the content, and the Card No. is extracted from the Order No. with spaces removed; if extraction fails, the reason is recorded and no other mode is tried.

### 5.4 How to change the rules

The rules are read-only in the app. To change them, contact the backend administrator to update the trade modes on the backend, then rebind the device ID in the app to sync.

## 6. Fetch the Latest Rules

1. After the backend updates the trade modes, open the “Settings” screen.
2. Tap “Rebind Device ID”.
3. Enter the device ID and tap “Verify and Save”.
4. After successful verification, the app syncs the latest trade modes from the backend.

Notes:

- The rules are delivered as a JSON list together with the device ID verification result; each item contains name, notify_type, trade_info_source, trade_judge_rule, trade_no_parse_rule1, trade_no_parse_rule2, card_no_parse_rule1, card_no_parse_rule2, mony_parse_rule1, and mony_parse_rule2.
- If the backend response does not contain the trade mode list, the app keeps the locally saved trade modes.

## 7. Test Parsing

Use “Test Parse” before relying on real message detection.

Steps:

1. Open the “Settings” screen.
2. Select the message type: SMS or APP Notification.
3. Enter the sender number or APP name/package name in the “Source” box (optional).
4. Paste a real SMS or APP notification content.
5. Tap “Test Parse”.
6. The app displays the parsing result.

The parsing result may include:

- Whether a trade mode is matched, and the matched mode name.
- The Order No., the Card No., and the Amount (shown as “not extracted” when empty).
- The failure reason when the mode is matched but the Order No. or Card No. is not extracted.
- The reason when no mode matches (including per-mode source/judge skip notes).

If parsing fails, check:

1. Whether the selected message type (SMS / APP Notification) matches the actual message source.
2. Whether the source in the content contains any fragment configured in the mode’s “Trade Info Source”.
3. Whether the content matches the mode’s “Trade Judge Rule” regex.
4. Whether the Order No. regex can extract a result from the content, and whether the Card No. regex can extract a result from the Order No. with spaces removed.
5. If the rules clearly do not fit, contact the backend administrator to adjust the trade mode configuration.

## 8. Message Processing and Backend Submission

When the app receives an SMS message or APP notification, it processes it in this order:

1. Check whether the device ID is verified.
2. Try the trade modes that match the message type (SMS / APP Notification) in order until one mode matches.
3. Extract the Order No. from the message content, extract the Card No. from the Order No. with spaces removed, and extract the Amount (may be empty) with the matched mode regexes.
4. Check whether the same message content exists within the last 1 hour.
5. Check whether the same Order No. exists within the last 1 hour.
6. Save the local record.
7. Call the backend recharge card API.
8. Save the backend API response.

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
- Whether the SMS sender passes the “Trade Info Source” check of any SMS mode.
- Whether the SMS content matches the “Trade Judge Rule” regex of any SMS mode.
- Whether the trade modes have been fetched from the backend (see the “Settings” screen).
- Whether the device ID has been verified.

### 14.3 Why are there no APP notification records?

Check:

- Whether notification listener permission is enabled.
- Whether the APP name or package name passes the “Trade Info Source” check of any APP Notification mode.
- Whether the notification content matches the “Trade Judge Rule” regex of any APP Notification mode.
- Whether the trade modes have been fetched from the backend (see the “Settings” screen).
- Whether the device ID has been verified.

### 14.4 Why is the Order No. not parsed?

Check whether the “Trade No. Parse Rule” regex of the matched mode can extract the Order No. from the content; if the rules clearly do not fit the message format, contact the backend administrator to adjust the trade mode configuration.

### 14.5 Why is the Amount not parsed?

Check whether the “Amount Parse Rule” regex of the matched mode can extract the amount from the content; an empty amount does not affect the parsing and submission of the Order No. and Card No. If the rules clearly do not fit the message format, contact the backend administrator to adjust the trade mode configuration.

### 14.6 Why does a record show Failed?

The backend API may have returned an error, or a network error may have occurred. Open the record detail page to check the API response.

### 14.7 Can cleared messages be restored?

No. Clearing messages deletes records from the local database. Use this feature carefully.
