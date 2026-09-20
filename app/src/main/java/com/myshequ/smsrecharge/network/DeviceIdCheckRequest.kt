package com.myshequ.smsrecharge.network

/**
 * 设备号校验接口（checkDeviceIdValid.do）请求参数。
 * 后台按 return_code 是否为 "SUCCESS" 判断该设备号是否合法。
 */
data class DeviceIdCheckRequest(
    val deviceId: String
)
