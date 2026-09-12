package com.myshequ.smsrecharge.network

import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response

/**
 * 后台返回结构以 return_code 字段为准：return_code 为 "SUCCESS" 时表示成功，否则为失败，
 * 错误提示信息优先取 return_message。为兼容未提前约定返回格式的接口，
 * 若未包含 return_code 字段，则降级尝试 success/code/status 等常见字段，再否则仅以 HTTP 状态码判断，
 * 原始内容始终保留供排查。checkDeviceId.do 和 smsToRechargeCard.do 均复用该解析逻辑。
 */
data class RechargeResult(
    val success: Boolean,
    val message: String,
    val rawResponse: String
)

object RechargeResultParser {

    fun parse(response: Response<ResponseBody>): RechargeResult {
        val httpOk = response.isSuccessful
        val rawBody = try {
            response.body()?.string() ?: response.errorBody()?.string() ?: ""
        } catch (e: Exception) {
            "读取响应内容失败：${e.message}"
        }

        if (rawBody.isBlank()) {
            return RechargeResult(success = httpOk, message = "HTTP ${response.code()}", rawResponse = rawBody)
        }

        return try {
            val json = JSONObject(rawBody)
            val success = when {
                json.has("return_code") -> json.optString("return_code").equals("SUCCESS", ignoreCase = true)
                json.has("success") -> json.optBoolean("success", httpOk)
                json.has("code") -> json.optString("code") in listOf("0", "200", "success", "SUCCESS")
                json.has("status") -> json.optString("status") in listOf("0", "200", "success", "SUCCESS", "ok", "OK")
                else -> httpOk
            }
            val message = json.optString("return_message", json.optString("message", json.optString("msg", rawBody)))
            RechargeResult(success = success, message = message, rawResponse = rawBody)
        } catch (e: Exception) {
            // 非 JSON 返回，直接以 HTTP 状态判断，原文作为提示信息
            RechargeResult(success = httpOk, message = rawBody, rawResponse = rawBody)
        }
    }

    fun ofException(e: Exception): RechargeResult =
        RechargeResult(success = false, message = "请求异常：${e.message}", rawResponse = "")
}
