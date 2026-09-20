# SMS Recharge Card Assistant User Guide (English Simple Version)

## 1. First Launch

1. Install and open the app.
2. On first launch, enter the device ID.
3. Tap “Verify and Save”.
4. If verification succeeds, the app enters the main screen.
5. If verification fails, check the device ID and try again.
6. Before the device ID is verified, the app will not intercept SMS messages or APP notifications.

## 2. Enable SMS Permissions

1. If a permission banner appears on the main screen, tap “Grant Now”.
2. Allow the SMS receiving and SMS reading permissions.
3. After authorization, the app can automatically detect matching SMS messages.

## 3. Enable APP Notification Listener

1. Open the “Settings” screen.
2. Tap “Open Notification Listener Permission”.
3. Enable notification access for this app in the system settings page.
4. After authorization, the app can detect supported APP notifications.

## 4. View Parsing Rules (Trade Modes)

Parsing rules are configured on the backend and are delivered to the app automatically after the device ID is verified successfully.

Open the “Settings” screen to:

- View the mode name buttons in the “Trade Modes” section.
- Tap a mode button to view its details below: Name, Notify Type, Trade Info Source, Trade Judge Rule, Trade No. Parse Rule 1/2, Card No. Parse Rule 1/2, and Amount Parse Rule 1/2.
- The rules are read-only. To change them, contact the backend administrator, then rebind the device ID to sync.

## 5. Test Parsing

1. On the “Settings” screen, select the message type (SMS / APP Notification).
2. Enter the sender number or APP name/package name in the “Source” box (optional).
3. Paste a real SMS or APP notification content.
4. Tap “Test Parse”.
5. Check whether a trade mode is matched and whether the Order No., Card No., and Amount are parsed correctly (the Amount shows “not extracted” when empty).
6. If parsing fails, check whether the selected message type is correct and whether the content matches the backend mode rules.

## 6. View Message Records

1. The main screen displays detected SMS and APP notification records.
2. Select a start date and end date, then tap “Query” to filter records.
3. Tap “View All” to show all records again.
4. Tap a record to view details and the backend API response.

## 7. Clear History Records

1. Tap “Clear Messages” on the main screen.
2. Enter how many days ago the messages should be cleared from.
3. Tap “Clear”.
4. The app deletes matching local records and shows the actual number of cleared records.

## 8. Rebind Device ID

1. Open the “Settings” screen.
2. Tap “Rebind Device ID”.
3. Enter the new device ID.
4. Tap “Verify and Save”.
5. After successful verification, the new device ID is saved locally.
