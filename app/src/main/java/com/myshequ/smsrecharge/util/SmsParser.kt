package com.myshequ.smsrecharge.util

import android.content.Context
import com.myshequ.smsrecharge.R
import com.myshequ.smsrecharge.data.TradeMode

/**
 * 消息解析结果
 */
data class ParseResult(
    val matched: Boolean,          // 是否命中某个交易模式（来源通过且交易信息判定通过）
    val parsed: Boolean = false,   // 是否成功提取出交易号与充值卡号
    val modeName: String = "",     // 命中的交易模式名称
    val orderNo: String = "",      // 提取出的交易号
    val cardNo: String = "",       // 提取出的充值卡号
    val money: String = "",        // 提取出的交易金额，未配置规则或未提取到时为空字符串（不影响 parsed）
    val reason: String = ""        // 未命中/解析失败原因，便于排查
)

/**
 * 消息解析工具：移除原有固定规则逻辑，统一按后台下发（设备号校验接口返回）的交易模式规则解析。
 *
 * 解析流程（对指定 notify_type 的交易模式依次尝试，直到某个模式命中为止，或试遍所有该类型模式）：
 * 1. 来源判断：trade_info_source 非空时，来源（短信发送号码或APP通知来源）必须包含其中任一片段
 *    （英文逗号分隔，不区分大小写）；留空表示不限制来源。
 * 2. 交易信息判定：trade_judge_rule 非空时，内容必须命中该正则表达式，命中即视为该模式命中；留空表示不判定。
 * 3. 命中后提取交易号：按 trade_no_parse_rule1 提取；trade_no_parse_rule2 非空时，
 *    对 rule1 的提取结果再按 rule2 二次提取（二级串联），最终取 rule2 的结果。
 * 4. 充值卡号：在提取出的交易号基础上解析——先去掉交易号中的空格（含全角空格），
 *    再按 card_no_parse_rule1 / card_no_parse_rule2 以同样方式在去空格后的交易号上提取；
 *    交易号未提取出时卡号不再解析。
 * 5. 交易金额：按 mony_parse_rule1 / mony_parse_rule2 以同样方式提取；未配置规则或未提取到时为空字符串，
 *    不影响交易号与充值卡号的判定及后续流程。
 * 6. 提取正则包含捕获组时优先取第一个非空捕获组，否则取整个匹配内容。
 */
object SmsParser {

    fun parse(context: Context, content: String, source: String, notifyType: Int): ParseResult {
        if (content.isBlank()) {
            return ParseResult(matched = false, reason = context.getString(R.string.parse_reason_empty_content))
        }

        val modes = AppSettings.getTradeModes(context).filter { it.notifyType == notifyType }
        if (modes.isEmpty()) {
            return ParseResult(matched = false, reason = context.getString(R.string.parse_reason_no_modes, notifyType))
        }

        val skipReasons = mutableListOf<String>()
        for (mode in modes) {
            val label = mode.name.ifBlank { context.getString(R.string.parse_mode_unnamed) }

            if (!isSourceMatch(mode.tradeInfoSource, source)) {
                skipReasons += context.getString(R.string.parse_skip_source, label)
                continue
            }

            val judgeFailReason = checkTradeJudge(context, mode.tradeJudgeRule, content)
            if (judgeFailReason != null) {
                skipReasons += "[$label]$judgeFailReason"
                continue
            }

            // 该模式已命中：先提取交易号与交易金额；卡号在去除空格后的交易号上提取，金额可为空不影响解析判定
            val orderNo = extractByRules(content, mode.tradeNoParseRule1, mode.tradeNoParseRule2)
            val money = extractByRules(content, mode.monyParseRule1, mode.monyParseRule2).orEmpty()
            if (orderNo == null) {
                // 交易号未提取出，卡号以其为输入，无法继续解析
                return ParseResult(
                    matched = true,
                    parsed = false,
                    modeName = label,
                    money = money,
                    reason = context.getString(R.string.parse_matched_no_order, label)
                )
            }

            // 卡号提取输入：去掉交易号中的空格（含全角空格）
            val cardNoSource = orderNo.replace(" ", "").replace("　", "")
            val cardNo = extractByRules(cardNoSource, mode.cardNoParseRule1, mode.cardNoParseRule2)
            if (cardNo != null) {
                return ParseResult(
                    matched = true,
                    parsed = true,
                    modeName = label,
                    orderNo = orderNo,
                    cardNo = cardNo,
                    money = money
                )
            }

            return ParseResult(
                matched = true,
                parsed = false,
                modeName = label,
                money = money,
                reason = context.getString(R.string.parse_matched_no_card, label)
            )
        }

        return ParseResult(
            matched = false,
            reason = context.getString(
                R.string.parse_all_unmatched,
                skipReasons.joinToString("; ")
            )
        )
    }

    /**
     * 来源判断：trade_info_source 为英文逗号分隔的片段列表，来源包含任一片段即通过；留空表示不限制来源。
     */
    private fun isSourceMatch(tradeInfoSource: String, source: String): Boolean {
        val fragments = tradeInfoSource.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (fragments.isEmpty()) return true
        val lowerSource = source.lowercase()
        return fragments.any { lowerSource.contains(it.lowercase()) }
    }

    /**
     * 交易信息判定：返回 null 表示通过，否则返回未通过的原因。
     */
    private fun checkTradeJudge(context: Context, rule: String, content: String): String? {
        if (rule.isBlank()) return null
        return try {
            if (Regex(rule).containsMatchIn(content)) null
            else context.getString(R.string.parse_judge_not_matched)
        } catch (_: Exception) {
            context.getString(R.string.parse_judge_invalid)
        }
    }

    /**
     * 按规则提取：rule1 作用于原文；rule2 非空时作用于 rule1 的提取结果（二级串联）。
     */
    private fun extractByRules(text: String, rule1: String, rule2: String): String? {
        if (rule1.isBlank()) return null
        val firstValue = extractByRule(text, rule1) ?: return null
        if (rule2.isBlank()) return firstValue
        return extractByRule(firstValue, rule2)
    }

    /**
     * 单条正则提取：优先取第一个非空捕获组，无捕获组时取整个匹配内容。
     */
    private fun extractByRule(text: String, rule: String): String? {
        return try {
            val match = Regex(rule).find(text) ?: return null
            val group = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() }
            (group ?: match.value).trim().takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }
}
