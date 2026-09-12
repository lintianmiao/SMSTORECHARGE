package com.myshequ.smsrecharge.util

import android.content.Context
import android.content.SharedPreferences

/**
 * 应用可配置项：短信来源、筛选关键词、订单号提取关键字、订单号长度、后台接口地址、设备号相关配置。
 * 均可在“设置”页面修改，无需重新编译即可适配真实短信格式。
 */
object AppSettings {

    private const val PREF_NAME = "sms_recharge_settings"
    private const val KEY_KEYWORDS = "keywords"
    private const val KEY_SMS_SOURCES = "sms_sources"
    private const val KEY_ORDER_KEYWORD = "order_keyword"
    private const val KEY_ORDER_LENGTH = "order_length"
    private const val KEY_MONEY_KEYWORD = "money_keyword"
    private const val KEY_API_URL = "api_url"
    private const val KEY_CHECK_DEVICE_ID_URL = "check_device_id_url"
    private const val KEY_DEVICE_ID = "device_id"

    // 默认来源：兼容短信来源 WECHAT 和微信APP通知来源。可填写发送号码、应用名、包名或其片段；留空表示不限制来源。
    const val DEFAULT_SMS_SOURCES = "18926073526"

    // 默认关键词：短信正文必须同时包含指定关键词
    const val DEFAULT_KEYWORDS = "Vous avez recu,Orabank"

    // 默认订单号解析规则：取指定关键字后指定长度的字符串。
    const val DEFAULT_ORDER_KEYWORD = "Orabank"
    const val DEFAULT_ORDER_LENGTH = 11

    // 默认交易金额解析规则：取关键字后面的数字（可带小数点）。
    const val DEFAULT_MONEY_KEYWORD = "Vous avez recu"

    const val DEFAULT_API_URL = "https://test.myshequ.cn:8445/otherService/smsToRechargeCard.do"

    // 默认设备号校验接口地址：APP首次使用需先通过该接口校验输入的设备号。
    const val DEFAULT_CHECK_DEVICE_ID_URL = "https://test.myshequ.cn:8445/otherService/checkDeviceIdValid.do"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getKeywords(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_KEYWORDS, DEFAULT_KEYWORDS) ?: DEFAULT_KEYWORDS
        return raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun getKeywordsRaw(context: Context): String =
        prefs(context).getString(KEY_KEYWORDS, DEFAULT_KEYWORDS) ?: DEFAULT_KEYWORDS

    fun setKeywords(context: Context, raw: String) {
        prefs(context).edit().putString(KEY_KEYWORDS, raw).apply()
    }

    fun getSmsSources(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_SMS_SOURCES, DEFAULT_SMS_SOURCES) ?: DEFAULT_SMS_SOURCES
        return raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun getSmsSourcesRaw(context: Context): String =
        prefs(context).getString(KEY_SMS_SOURCES, DEFAULT_SMS_SOURCES) ?: DEFAULT_SMS_SOURCES

    fun setSmsSources(context: Context, raw: String) {
        prefs(context).edit().putString(KEY_SMS_SOURCES, raw).apply()
    }

    fun isSourceAllowed(context: Context, senderAddress: String): Boolean {
        val sources = getSmsSources(context)
        if (sources.isEmpty()) return true
        val lowerAddress = senderAddress.lowercase()
        return sources.any { lowerAddress.contains(it.lowercase()) }
    }

    fun getOrderKeyword(context: Context): String =
        prefs(context).getString(KEY_ORDER_KEYWORD, DEFAULT_ORDER_KEYWORD) ?: DEFAULT_ORDER_KEYWORD

    fun setOrderKeyword(context: Context, keyword: String) {
        prefs(context).edit().putString(KEY_ORDER_KEYWORD, keyword).apply()
    }

    fun getOrderLength(context: Context): Int =
        prefs(context).getInt(KEY_ORDER_LENGTH, DEFAULT_ORDER_LENGTH)

    fun setOrderLength(context: Context, length: Int) {
        prefs(context).edit().putInt(KEY_ORDER_LENGTH, length).apply()
    }

    fun getMoneyKeyword(context: Context): String =
        prefs(context).getString(KEY_MONEY_KEYWORD, DEFAULT_MONEY_KEYWORD) ?: DEFAULT_MONEY_KEYWORD

    fun setMoneyKeyword(context: Context, keyword: String) {
        prefs(context).edit().putString(KEY_MONEY_KEYWORD, keyword).apply()
    }

    fun getApiUrl(context: Context): String =
        prefs(context).getString(KEY_API_URL, DEFAULT_API_URL) ?: DEFAULT_API_URL

    fun setApiUrl(context: Context, url: String) {
        prefs(context).edit().putString(KEY_API_URL, url).apply()
    }

    fun getCheckDeviceIdUrl(context: Context): String =
        prefs(context).getString(KEY_CHECK_DEVICE_ID_URL, DEFAULT_CHECK_DEVICE_ID_URL) ?: DEFAULT_CHECK_DEVICE_ID_URL

    fun setCheckDeviceIdUrl(context: Context, url: String) {
        prefs(context).edit().putString(KEY_CHECK_DEVICE_ID_URL, url).apply()
    }

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

    fun resetToDefault(context: Context) {
        prefs(context).edit()
            .putString(KEY_KEYWORDS, DEFAULT_KEYWORDS)
            .putString(KEY_SMS_SOURCES, DEFAULT_SMS_SOURCES)
            .putString(KEY_ORDER_KEYWORD, DEFAULT_ORDER_KEYWORD)
            .putInt(KEY_ORDER_LENGTH, DEFAULT_ORDER_LENGTH)
            .putString(KEY_MONEY_KEYWORD, DEFAULT_MONEY_KEYWORD)
            .putString(KEY_API_URL, DEFAULT_API_URL)
            .putString(KEY_CHECK_DEVICE_ID_URL, DEFAULT_CHECK_DEVICE_ID_URL)
            .apply()
        // 注意：设备号（device_id）不在“恢复默认”范围内，避免误操作导致已绑定的设备号被清空。
    }
}
