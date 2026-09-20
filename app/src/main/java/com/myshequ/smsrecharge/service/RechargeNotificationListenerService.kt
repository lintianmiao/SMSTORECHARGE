package com.myshequ.smsrecharge.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.myshequ.smsrecharge.data.SmsRecord
import com.myshequ.smsrecharge.data.TradeMode
import com.myshequ.smsrecharge.util.AppSettings
import com.myshequ.smsrecharge.util.SmsParser
import com.myshequ.smsrecharge.worker.SmsProcessWorker

/**
 * APP通知监听服务：
 * 1. 需要用户在系统“通知使用权/通知访问权限”页面手动授权。
 * 2. 读取通知标题、正文、长文本等内容，按后台下发的交易模式（notify_type=1）规则解析。
 * 3. 命中某个交易模式后交给 SmsProcessWorker 入库，并由后台接口保存消息。
 * 4. 若本机尚未绑定设备号（未完成首次使用校验），则不启动拦截消息功能，直接忽略通知。
 */
class RechargeNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName == packageName) return

        // 尚未绑定设备号时，不启动任何拦截消息功能
        if (!AppSettings.hasDeviceId(this)) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val bigText = extras.getCharSequence("android.bigText")?.toString().orEmpty()
        val subText = extras.getCharSequence("android.subText")?.toString().orEmpty()
        val tickerText = sbn.notification.tickerText?.toString().orEmpty()

        val content = listOf(title, text, bigText, subText, tickerText)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(separator = "\n")
        if (content.isBlank()) return

        val appLabel = getAppLabel(sbn.packageName)
        val notificationSource = "$appLabel(${sbn.packageName})"
        val result = SmsParser.parse(this, content, notificationSource, TradeMode.NOTIFY_TYPE_APP)
        if (!result.matched) return

        val inputData = Data.Builder()
            .putString(SmsProcessWorker.KEY_MESSAGE_SOURCE, SmsRecord.SOURCE_APP)
            .putString(SmsProcessWorker.KEY_ADDRESS, notificationSource)
            .putString(SmsProcessWorker.KEY_SMS_BODY, content)
            .putLong(SmsProcessWorker.KEY_RECEIVED_TIME, sbn.postTime)
            .putString(SmsProcessWorker.KEY_ORDER_NO, result.orderNo)
            .putString(SmsProcessWorker.KEY_CARD_NO, result.cardNo)
            .putString(SmsProcessWorker.KEY_MONEY, result.money)
            .putString(SmsProcessWorker.KEY_PARSE_MESSAGE, result.reason)
            .putBoolean(SmsProcessWorker.KEY_SHOULD_PROCESS, result.parsed)
            .build()

        val request = OneTimeWorkRequestBuilder<SmsProcessWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(request)
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }
    }
}
