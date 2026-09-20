package com.myshequ.smsrecharge.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 交易模式：由后台接口 checkDeviceIdValid.do 随设备号校验结果下发，保存在本地并用于消息解析。
 *
 * 字段说明：
 * - notifyType（notify_type）：通知类型，0-短信，1-APP通知。
 * - tradeInfoSource（trade_info_source）：信息来源判断，英文逗号分隔多个片段，
 *   来源（短信发送号码或APP通知来源）包含任一片段即通过；留空表示不限制来源。
 * - tradeJudgeRule（trade_judge_rule）：是否交易信息的正则表达式，命中才继续提取交易号与卡号；留空表示不判定。
 * - tradeNoParseRule1 / tradeNoParseRule2：交易号提取正则；rule2 非空时先按 rule1 提取，再对提取结果按 rule2 二次提取。
 * - cardNoParseRule1 / cardNoParseRule2：充值卡号提取正则；以去除空格后的交易号为输入（rule2 规则同上）。
 * - monyParseRule1 / monyParseRule2：交易金额提取正则（后台字段名为 mony_parse_rule1 / mony_parse_rule2）；
 *   rule2 非空时先按 rule1 提取，再对提取结果按 rule2 二次提取；未配置规则或未提取到时金额为空字符串。
 */
data class TradeMode(
    val name: String = "",
    val notifyType: Int = NOTIFY_TYPE_SMS,
    val tradeInfoSource: String = "",
    val tradeJudgeRule: String = "",
    val tradeNoParseRule1: String = "",
    val tradeNoParseRule2: String = "",
    val cardNoParseRule1: String = "",
    val cardNoParseRule2: String = "",
    val monyParseRule1: String = "",
    val monyParseRule2: String = ""
) {

    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("notify_type", notifyType)
        put("trade_info_source", tradeInfoSource)
        put("trade_judge_rule", tradeJudgeRule)
        put("trade_no_parse_rule1", tradeNoParseRule1)
        put("trade_no_parse_rule2", tradeNoParseRule2)
        put("card_no_parse_rule1", cardNoParseRule1)
        put("card_no_parse_rule2", cardNoParseRule2)
        put("mony_parse_rule1", monyParseRule1)
        put("mony_parse_rule2", monyParseRule2)
    }

    companion object {
        const val NOTIFY_TYPE_SMS = 0
        const val NOTIFY_TYPE_APP = 1

        fun fromJson(json: JSONObject): TradeMode = TradeMode(
            name = json.optString("name"),
            notifyType = json.optInt("notify_type", NOTIFY_TYPE_SMS),
            tradeInfoSource = json.optString("trade_info_source"),
            tradeJudgeRule = json.optString("trade_judge_rule"),
            tradeNoParseRule1 = json.optString("trade_no_parse_rule1"),
            tradeNoParseRule2 = json.optString("trade_no_parse_rule2"),
            cardNoParseRule1 = json.optString("card_no_parse_rule1"),
            cardNoParseRule2 = json.optString("card_no_parse_rule2"),
            monyParseRule1 = json.optString("mony_parse_rule1"),
            monyParseRule2 = json.optString("mony_parse_rule2")
        )

        fun fromArray(array: JSONArray): List<TradeMode> =
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let { fromJson(it) }
            }

        fun listToJson(modes: List<TradeMode>): String {
            val array = JSONArray()
            modes.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun listFromJson(raw: String?): List<TradeMode> {
            if (raw.isNullOrBlank()) return emptyList()
            return try {
                fromArray(JSONArray(raw))
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
