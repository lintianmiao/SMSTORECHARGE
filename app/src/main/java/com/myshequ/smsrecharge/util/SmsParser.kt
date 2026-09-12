package com.myshequ.smsrecharge.util

import android.content.Context

/**
 * 短信解析结果
 */
data class ParseResult(
    val matched: Boolean,          // 是否命中短信来源和正文关键词
    val parsed: Boolean = false,   // 是否成功解析交易单号
    val orderNo: String = "",      // 解析出的交易单号（完整）
    val cardNo: String = "",       // 交易单号后6位，作为充值卡号
    val money: String = "",        // 解析出的交易金额（可带小数点），未解析到时为空
    val reason: String = ""        // 未命中/解析失败原因，便于排查
)

/**
 * 短信解析工具：
 * 1. 来源过滤：配置了短信来源时，发送号码必须包含任一来源片段。
 * 2. 关键词过滤：所有正文关键词必须同时出现在短信内容中（AND 逻辑，大小写不敏感）。
 * 3. 订单号提取：取指定关键字后面的指定长度字符串；若解析失败，仍会返回命中结果供后台保存消息。
 * 4. 充值卡号：取交易单号的最后 6 位。
 * 5. 交易金额提取：取指定关键字后面的数字（可带小数点），解析失败不影响其余流程。
 */
object SmsParser {

    private val separatorRegex = Regex("^[\\s:：=—_，,；;-]+")
    private val moneyRegex = Regex("^[0-9]+(\\.[0-9]+)?")

    fun parse(context: Context, smsBody: String, senderAddress: String = ""): ParseResult {
        if (smsBody.isBlank()) {
            return ParseResult(matched = false, reason = "短信内容为空")
        }

        if (!AppSettings.isSourceAllowed(context, senderAddress)) {
            return ParseResult(matched = false, reason = "短信来源不匹配：$senderAddress")
        }

        val keywords = AppSettings.getKeywords(context)
        if (keywords.isEmpty()) {
            return ParseResult(matched = false, reason = "未配置筛选关键词")
        }

        val lowerBody = smsBody.lowercase()
        val notMatched = keywords.filter { !lowerBody.contains(it.lowercase()) }
        if (notMatched.isNotEmpty()) {
            return ParseResult(matched = false, reason = "缺少关键词：${notMatched.joinToString("、")}")
        }

        val money = extractMoney(context, smsBody)

        val orderNo = extractOrderNo(context, smsBody)
        if (orderNo.isNullOrEmpty()) {
            return ParseResult(matched = true, parsed = false, money = money, reason = "命中来源和关键词，但未解析出交易单号")
        }

        val cardNo = if (orderNo.length >= 6) {
            orderNo.substring(orderNo.length - 6)
        } else {
            orderNo
        }

        return ParseResult(matched = true, parsed = true, orderNo = orderNo, cardNo = cardNo, money = money)
    }

    private fun extractOrderNo(context: Context, smsBody: String): String? {
        val keyword = AppSettings.getOrderKeyword(context)
        val length = AppSettings.getOrderLength(context)
        if (keyword.isBlank() || length <= 0) return null

        val keywordIndex = smsBody.lowercase().indexOf(keyword.lowercase())
        if (keywordIndex < 0) return null

        val rawAfterKeyword = smsBody.substring(keywordIndex + keyword.length)
        val cleanedAfterKeyword = rawAfterKeyword.replaceFirst(separatorRegex, "")
        if (cleanedAfterKeyword.length < length) return null

        return cleanedAfterKeyword.substring(0, length).trim().takeIf { it.isNotEmpty() }
    }

    private fun extractMoney(context: Context, smsBody: String): String {
        val keyword = AppSettings.getMoneyKeyword(context)
        if (keyword.isBlank()) return ""

        val keywordIndex = smsBody.lowercase().indexOf(keyword.lowercase())
        if (keywordIndex < 0) return ""

        val rawAfterKeyword = smsBody.substring(keywordIndex + keyword.length)
        val cleanedAfterKeyword = rawAfterKeyword.replaceFirst(separatorRegex, "")

        return moneyRegex.find(cleanedAfterKeyword)?.value ?: ""
    }
}
