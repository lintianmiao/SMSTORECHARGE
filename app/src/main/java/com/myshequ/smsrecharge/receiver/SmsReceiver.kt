package com.myshequ.smsrecharge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.myshequ.smsrecharge.data.SmsRecord
import com.myshequ.smsrecharge.data.TradeMode
import com.myshequ.smsrecharge.util.AppSettings
import com.myshequ.smsrecharge.util.SmsParser
import com.myshequ.smsrecharge.worker.SmsProcessWorker

/**
 * 接收系统短信广播，按后台下发的交易模式（notify_type=0）规则快速完成来源判断与交易信息判定（纯内存计算，不涉及 IO），
 * 命中某个交易模式后交给 WorkManager 异步完成入库；成功提取交易号与充值卡号才调用后台接口。
 * 若本机尚未绑定设备号（未完成首次使用校验），则不启动拦截消息功能，直接忽略短信。
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        // 尚未绑定设备号时，不启动任何拦截消息功能
        if (!AppSettings.hasDeviceId(context)) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // 同一发送号码在极短时间内分片发送的多条 pdu 会被系统合并为同一批 messages，这里拼接为完整短信内容
        val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }
        val address = messages.firstOrNull()?.originatingAddress ?: ""
        val receivedTime = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

        val result = SmsParser.parse(context, fullBody, address, TradeMode.NOTIFY_TYPE_SMS)
        if (!result.matched) {
            return
        }

        val inputData = Data.Builder()
            .putString(SmsProcessWorker.KEY_MESSAGE_SOURCE, SmsRecord.SOURCE_SMS)
            .putString(SmsProcessWorker.KEY_ADDRESS, address)
            .putString(SmsProcessWorker.KEY_SMS_BODY, fullBody)
            .putLong(SmsProcessWorker.KEY_RECEIVED_TIME, receivedTime)
            .putString(SmsProcessWorker.KEY_ORDER_NO, result.orderNo)
            .putString(SmsProcessWorker.KEY_CARD_NO, result.cardNo)
            .putString(SmsProcessWorker.KEY_MONEY, result.money)
            .putString(SmsProcessWorker.KEY_PARSE_MESSAGE, result.reason)
            .putBoolean(SmsProcessWorker.KEY_SHOULD_PROCESS, result.parsed)
            .build()

        val request = OneTimeWorkRequestBuilder<SmsProcessWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueue(request)
    }
}
