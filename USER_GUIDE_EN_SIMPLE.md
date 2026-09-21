# SMS Recharge Card Assistant User Guide (English Simple Version)

Note: The app name is fixed to “SMS Recharge Card Assistant” in all languages. The UI is shown in English by default; you can switch the UI language on the Device ID screen or from the main screen menu. Interface names in this guide match the English UI.

## 1. First Launch

1. Install and open the app.
2. On first launch, the app opens the Device ID screen automatically; enter the device ID.
3. Tap “Verify and Save”.
4. If verification succeeds, the app enters the main screen and automatically fetches the parsing rules (trade modes) from the backend; if verification fails, an error message appears on the page — check the device ID and try again.
5. Before the device ID is verified, the app will not intercept SMS messages or APP notifications.

## 2. Switch UI Language (Optional)

The UI is shown in English by default. You can switch it in two places:

- The “Language” button at the top right of the Device ID screen.
- The “Language” item in the main screen menu.

Available languages: System Default, 中文, English, Français, Español, Português. The change takes effect immediately.

## 3. Enable SMS Permissions

1. After entering the main screen, if a permission banner appears at the top, tap “Grant SMS”.
2. Allow the “Receive SMS” and “Read SMS” permissions as prompted by the system.
3. After authorization, the banner disappears and the app can automatically detect matching SMS messages.

You can also open the “Settings” screen and tap “Grant SMS Permission”.

## 4. Enable APP Notification Listener

1. After entering the main screen, if a notification permission banner appears at the top, tap “Grant Notification”; or open the “Settings” screen and tap “Open Notification Listener Permission”.
2. On the system notification access page, find this app and enable notification access.
3. Return to the app — the banner disappears and the app can detect supported APP notifications (such as WeChat).

Note: Notification listener permission must be enabled manually in the system settings; the system does not show a permission dialog for it.

## 5. View Parsing Rules (Trade Modes)

Parsing rules are configured on the backend and are delivered to and saved in the app automatically after the device ID is verified successfully (this screen is read-only).

On the “Settings” screen, you can:

- View the mode name buttons in the “Trade Modes” section; the details of the first mode are shown by default.
- Tap a mode button to view its details under “Mode Details”: Name, Notify Type (SMS / APP Notification), Trade Info Source, Trade Judge Rule, Trade No. Parse Rule 1/2, Card No. Parse Rule 1/2, and Amount Parse Rule 1/2; fields that are not configured show “(not set)”.
- If “No trade modes yet, please rebind the device ID to fetch them” appears, the rules have not been fetched yet.
- The rules are read-only. To change them, contact the backend administrator, then rebind the device ID to sync.

Note: The Card No. is parsed from the Order No.; if the Order No. is not parsed, the Card No. cannot be parsed either.

## 6. Test Parsing

1. On the “Settings” screen, select the message type: SMS or APP Notification.
2. Enter the sender number or APP name/package name in the “Source” box (optional).
3. Paste a real SMS or APP notification content.
4. Tap “Test Parse”.
5. Check the “Parse Result”:
   - Matched and parsed: shows the matched mode, Order No., Card No., and Amount (shown as “not extracted” when empty); the action reads “save record and call backend API”.
   - Matched but Order No. or Card No. not extracted: the failure reason is shown.
   - Not matched: the reason is shown.
6. If parsing fails, check whether the selected message type is correct and whether the source and content match the backend mode rules; if the rules do not fit, contact the backend administrator.

## 7. View Message Records

1. The main screen displays detected SMS and APP notification records, with “record(s) in total” shown at the top; each record shows: time, source (SMS / APP Notification), sender, Order No., Card No., Amount, status, and message type.
2. Statuses: Processing, Success, Failed, Saved Not Processed. Message types: New Message, Duplicate Message (same content within the last hour), Duplicate Order No. (same order number within the last hour).
3. Date filter: tap “Start Date” and “End Date” to pick the dates, then tap “Query” to filter records; if a date is missing, the app shows “Select Date”.
4. Tap “View All” to show all records again.
5. Tap a record to view its details: time, source, type, sender, Order No., Card No., Amount, status, message content, and the backend API response (including the message and the raw response).

## 8. Clear History Records

1. Tap “Clear Messages” in the main screen toolbar.
2. Enter how many days ago the messages should be cleared from (for example, entering 7 clears records older than 7 days).
3. Tap “Clear”.
4. The app deletes matching local records and shows the actual number of cleared records.

Note: Only numbers greater than 0 are accepted; only local records are deleted and they cannot be restored.

## 9. Rebind Device ID

1. Open the “Settings” screen to view the current device ID.
2. Tap “Rebind Device ID”.
3. Enter the new device ID.
4. Tap “Verify and Save”. After successful verification, the device is rebound and the latest trade modes are synced.

Tip: After the backend rules are changed, use this method to fetch the latest rules again.

## 10. FAQ

1. Why does the app stay on the Device ID screen?
   The device ID has not been verified. Check whether the device ID is correct, or contact the backend administrator to confirm that the device ID is activated.

2. Why are there no SMS or notification records?
   Check in order: whether SMS permission or notification listener permission is granted; whether the device ID is verified; whether trade modes exist on the “Settings” screen; whether the message source and content match the mode rules.

3. Why does a record show “Duplicate Message” / “Duplicate Order No.”?
   The same content or the same order number was received within the last hour; the app marks it as a duplicate so the backend can process it accordingly.

4. Why is the Amount shown as “Not parsed”?
   The matched mode has no amount rule or the amount could not be extracted; this does not affect the parsing and submission of the Order No. and Card No.

5. Why does a record show “Failed”?
   The backend API may have returned a failure or a network error occurred. Open the record details to view the API response.
