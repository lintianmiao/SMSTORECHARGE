package com.myshequ.smsrecharge.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * 后台接口地址在“设置”页面可配置，因此这里使用 @Url 传入完整地址，
 * Retrofit 在使用绝对 URL 时会忽略 baseUrl。
 * 由于接口返回结构未知，直接拿原始 ResponseBody，在上层做通用解析。
 * 注意：后台接口（.do）通常期望表单参数（application/x-www-form-urlencoded），
 * 而非 JSON 格式，因此改用 @FormUrlEncoded 和 @Field。
 */
interface ApiService {

    @FormUrlEncoded
    @POST
    suspend fun smsToRechargeCard(
        @Url url: String,
        @Field("cardNo") cardNo: String,
        @Field("orderNo") orderNo: String,
        @Field("phone") phone: String,
        @Field("smsContent") smsContent: String,
        @Field("smsTime") smsTime: Long,
        @Field("messageType") messageType: Int,
        @Field("money") money: String,
        @Field("deviceId") deviceId: String
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST
    suspend fun checkDeviceId(
        @Url url: String,
        @Field("deviceId") deviceId: String
    ): Response<ResponseBody>
}
