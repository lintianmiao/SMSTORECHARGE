package com.myshequ.smsrecharge.network

/**
 * 请求接口的参数。messageType：0-新消息，1-重复消息，2-重复交易号。
 * cardNo/orderNo 解析失败时可为空，后台可据此仅保存消息。
 * money：按模式 mony_parse_rule1/mony_parse_rule2 正则解析出的交易金额，未解析到时为空字符串。
 * deviceId：本机已通过 checkDeviceIdValid.do 校验并保存的设备号。
 */
data class RechargeRequest(
    val cardNo: String,
    val orderNo: String,
    val phone: String,
    val smsContent: String,
    val smsTime: Long,
    val messageType: Int,
    val money: String,
    val deviceId: String
)
