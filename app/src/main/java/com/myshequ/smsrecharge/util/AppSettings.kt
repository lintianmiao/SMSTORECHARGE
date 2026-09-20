package com.myshequ.smsrecharge.util

import android.content.Context
import android.content.SharedPreferences
import com.myshequ.smsrecharge.data.TradeMode

/**
 * 应用可配置项：设备号、卖家ID与后台下发的交易模式（解析规则）。
 * 交易模式由设备号校验接口 checkDeviceIdValid.do 随校验结果下发，校验成功后保存在本地，
 * 短信/APP通知的解析统一按交易模式中的规则执行；“设置”页面仅展示模式信息，不提供修改入口。
 * 后台接口地址（充值接口、设备号校验接口）固定使用本文件中的默认值，不在“设置”页面提供修改入口。
 */
object AppSettings {

    private const val PREF_NAME = "sms_recharge_settings"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_SELLER_ID = "seller_id"
    private const val KEY_TRADE_MODES = "trade_modes"

    const val DEFAULT_API_URL = "https://test.myshequ.cn:8445/otherService/smsToRechargeCard.do"

    // 默认设备号校验接口地址：APP首次使用需先通过该接口校验输入的设备号。
    const val DEFAULT_CHECK_DEVICE_ID_URL = "https://test.myshequ.cn:8445/otherService/checkDeviceIdValid.do"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // 后台接口地址（充值接口、设备号校验接口）：固定使用默认地址，不再提供“设置”页面修改入口。
    fun getApiUrl(): String = DEFAULT_API_URL

    fun getCheckDeviceIdUrl(): String = DEFAULT_CHECK_DEVICE_ID_URL

    // 设备号：首次使用时经后台校验通过后保存，之后随每次充值接口请求一起提交。
    fun getDeviceId(context: Context): String =
        prefs(context).getString(KEY_DEVICE_ID, "") ?: ""

    fun setDeviceId(context: Context, deviceId: String) {
        prefs(context).edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }

    fun hasDeviceId(context: Context): Boolean = getDeviceId(context).isNotBlank()

    fun clearDeviceId(context: Context) {
        prefs(context).edit().remove(KEY_DEVICE_ID).apply()
    }

    // 卖家ID：与设备号校验结果一同由后台下发并保存。
    fun getSellerId(context: Context): String =
        prefs(context).getString(KEY_SELLER_ID, "") ?: ""

    fun setSellerId(context: Context, sellerId: String) {
        prefs(context).edit().putString(KEY_SELLER_ID, sellerId).apply()
    }

    // 交易模式：设备号校验成功后由后台下发；短信/APP通知解析统一按模式中的规则执行。
    fun getTradeModes(context: Context): List<TradeMode> =
        TradeMode.listFromJson(prefs(context).getString(KEY_TRADE_MODES, "") ?: "")

    fun setTradeModes(context: Context, modes: List<TradeMode>) {
        prefs(context).edit().putString(KEY_TRADE_MODES, TradeMode.listToJson(modes)).apply()
    }
}
