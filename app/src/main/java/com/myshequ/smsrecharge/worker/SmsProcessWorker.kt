package com.myshequ.smsrecharge.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.myshequ.smsrecharge.data.AppDatabase
import com.myshequ.smsrecharge.data.SmsRecord
import com.myshequ.smsrecharge.network.RechargeResultParser
import com.myshequ.smsrecharge.network.RetrofitClient
import com.myshequ.smsrecharge.util.AppSettings

/**
 * 后台处理一条已识别的短信或APP通知：
 * 1. 判断最近1小时是否为重复消息或重复交易号
 * 2. 先写入本地数据库并标记消息类型
 * 3. 所有命中的消息均调用后台接口，携带 messageType 供后台保存和区分处理
 * 使用 WorkManager 而不是在 BroadcastReceiver/NotificationListenerService 中直接处理网络请求，
 * 可以保证任务不会因为接收器执行时间过短而被系统中断。
 */
class SmsProcessWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_MESSAGE_SOURCE = "message_source"
        const val KEY_ADDRESS = "address"
        const val KEY_SMS_BODY = "sms_body"
        const val KEY_RECEIVED_TIME = "received_time"
        const val KEY_ORDER_NO = "order_no"
        const val KEY_CARD_NO = "card_no"
        const val KEY_MONEY = "money"
        const val KEY_PARSE_MESSAGE = "parse_message"
        const val KEY_SHOULD_PROCESS = "should_process"

        private const val DUPLICATE_WINDOW_MS = 60 * 60 * 1000L
    }

    override suspend fun doWork(): Result {
        val messageSource = inputData.getString(KEY_MESSAGE_SOURCE) ?: SmsRecord.SOURCE_SMS
        val address = inputData.getString(KEY_ADDRESS) ?: ""
        val smsBody = inputData.getString(KEY_SMS_BODY) ?: ""
        val receivedTime = inputData.getLong(KEY_RECEIVED_TIME, System.currentTimeMillis())
        val orderNo = inputData.getString(KEY_ORDER_NO) ?: ""
        val cardNo = inputData.getString(KEY_CARD_NO) ?: ""
        val money = inputData.getString(KEY_MONEY) ?: ""
        val parseMessage = inputData.getString(KEY_PARSE_MESSAGE) ?: ""
        val parsed = inputData.getBoolean(KEY_SHOULD_PROCESS, orderNo.isNotEmpty() && cardNo.isNotEmpty())

        val dao = AppDatabase.getInstance(applicationContext).smsRecordDao()
        val startTime = receivedTime - DUPLICATE_WINDOW_MS
        val sameMessageCount = dao.countSameMessageInRange(smsBody, startTime, receivedTime)
        val messageType = when {
            sameMessageCount > 0 -> SmsRecord.MESSAGE_TYPE_DUPLICATE_MESSAGE
            parsed && dao.countSameOrderInRange(orderNo, startTime, receivedTime) > 0 -> SmsRecord.MESSAGE_TYPE_DUPLICATE_ORDER
            else -> SmsRecord.MESSAGE_TYPE_NEW
        }

        val record = SmsRecord(
            messageSource = messageSource,
            senderAddress = address,
            smsBody = smsBody,
            receivedTime = receivedTime,
            orderNo = orderNo,
            cardNo = cardNo,
            money = money,
            status = if (parsed) SmsRecord.STATUS_PENDING else SmsRecord.STATUS_UNPARSED,
            messageType = messageType,
            apiMessage = if (parsed) "" else parseMessage
        )
        val id = dao.insert(record)

        val apiUrl = AppSettings.getApiUrl(applicationContext)
        val deviceId = AppSettings.getDeviceId(applicationContext)
        val result = try {
            val response = RetrofitClient.apiService.smsToRechargeCard(
                url = apiUrl,
                cardNo = cardNo,
                orderNo = orderNo,
                phone = address,
                smsContent = smsBody,
                smsTime = receivedTime,
                messageType = messageType,
                money = money,
                deviceId = deviceId
            )
            RechargeResultParser.parse(response)
        } catch (e: Exception) {
            RechargeResultParser.ofException(e)
        }

        val saved = dao.getById(id)
        if (saved != null) {
            saved.status = if (parsed) {
                if (result.success) SmsRecord.STATUS_SUCCESS else SmsRecord.STATUS_FAIL
            } else {
                SmsRecord.STATUS_UNPARSED
            }
            saved.apiMessage = listOf(parseMessage, result.message)
                .filter { it.isNotBlank() }
                .joinToString(separator = "\n")
            saved.apiRawResponse = result.rawResponse
            saved.requestTime = System.currentTimeMillis()
            dao.update(saved)
        }

        return if (result.success) {
            Result.success()
        } else {
            // 消息已经入库，避免 WorkManager 重试时重复写入本地记录。
            Result.failure()
        }
    }
}
