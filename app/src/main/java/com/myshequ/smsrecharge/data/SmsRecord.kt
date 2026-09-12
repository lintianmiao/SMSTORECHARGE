package com.myshequ.smsrecharge.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 本地记录：一条被识别到的交易短信及其处理结果
 */
@Entity(tableName = "sms_record")
data class SmsRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 消息来源：SMS-短信，APP-APP通知
    @ColumnInfo(defaultValue = "SMS")
    val messageSource: String = SOURCE_SMS,

    // 短信发送号码或APP通知来源
    val senderAddress: String,

    // 短信或通知原始内容
    val smsBody: String,

    // 短信接收时间（毫秒时间戳）
    val receivedTime: Long,

    // 解析出的完整交易单号
    val orderNo: String,

    // 交易单号后6位，作为充值卡号
    val cardNo: String,

    // 解析出的交易金额（可带小数点），未解析到时为空
    @ColumnInfo(defaultValue = "")
    val money: String = "",

    // 处理状态：PENDING-处理中，SUCCESS-成功，FAIL-失败，UNPARSED-已保存未处理
    var status: String = STATUS_PENDING,

    // 消息类型：0-新消息，1-重复消息，2-重复交易号
    @ColumnInfo(defaultValue = "0")
    var messageType: Int = MESSAGE_TYPE_NEW,

    // 接口返回的原始内容（用于排查问题）
    var apiRawResponse: String = "",

    // 接口返回的提示信息
    var apiMessage: String = "",

    // 请求发生的时间
    var requestTime: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SUCCESS = "SUCCESS"
        const val STATUS_FAIL = "FAIL"
        const val STATUS_UNPARSED = "UNPARSED"

        const val SOURCE_SMS = "SMS"
        const val SOURCE_APP = "APP"

        const val MESSAGE_TYPE_NEW = 0
        const val MESSAGE_TYPE_DUPLICATE_MESSAGE = 1
        const val MESSAGE_TYPE_DUPLICATE_ORDER = 2
    }
}
