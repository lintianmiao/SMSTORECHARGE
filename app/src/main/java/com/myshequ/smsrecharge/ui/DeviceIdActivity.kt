package com.myshequ.smsrecharge.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.myshequ.smsrecharge.R
import com.myshequ.smsrecharge.databinding.ActivityDeviceIdBinding
import com.myshequ.smsrecharge.network.RechargeResultParser
import com.myshequ.smsrecharge.network.RetrofitClient
import com.myshequ.smsrecharge.util.AppSettings
import kotlinx.coroutines.launch

/**
 * 设备号校验界面：
 * 1. APP 首次打开使用时会跳转到该页面，要求输入设备号。
 * 2. 调用后台接口 checkDeviceIdValid.do 进行校验，返回 return_code 为 "SUCCESS" 时保存设备号，
 *    并保存返回中的卖家ID与交易模式信息（seller_id + list），供短信/APP通知解析使用。
 * 3. 校验失败时提示错误消息，保持在当前输入界面，不会保存设备号，也不会启动短信/APP通知拦截功能。
 */
class DeviceIdActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceIdBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceIdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etDeviceId.setText(AppSettings.getDeviceId(this))
        binding.btnCheckDeviceId.setOnClickListener {
            hideKeyboard()
            checkAndSaveDeviceId()
        }

        // 点击空白区域隐藏键盘
        binding.layoutContent.setOnClickListener { hideKeyboard() }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun checkAndSaveDeviceId() {
        val deviceId = binding.etDeviceId.text.toString().trim()
        if (deviceId.isEmpty()) {
            binding.tvDeviceIdError.text = getString(R.string.device_id_empty_tip)
            return
        }

        binding.tvDeviceIdError.text = ""
        binding.btnCheckDeviceId.isEnabled = false
        binding.pbChecking.visibility = View.VISIBLE

        lifecycleScope.launch {
            val checkUrl = AppSettings.getCheckDeviceIdUrl()
            val result = try {
                val response = RetrofitClient.apiService.checkDeviceId(
                    url = checkUrl,
                    deviceId = deviceId
                )
                RechargeResultParser.parse(response)
            } catch (e: Exception) {
                RechargeResultParser.ofException(e)
            }

            binding.btnCheckDeviceId.isEnabled = true
            binding.pbChecking.visibility = View.GONE

            if (result.success) {
                AppSettings.setDeviceId(this@DeviceIdActivity, deviceId)
                // 校验成功时保存后台下发的卖家ID与交易模式（解析规则）；
                // 若返回中未包含 list 字段，则保留本地已保存的交易模式
                val payload = RechargeResultParser.parseTradeModes(result.rawResponse)
                if (payload != null) {
                    if (payload.sellerId.isNotBlank()) {
                        AppSettings.setSellerId(this@DeviceIdActivity, payload.sellerId)
                    }
                    payload.modes?.let { AppSettings.setTradeModes(this@DeviceIdActivity, it) }
                }
                Toast.makeText(this@DeviceIdActivity, R.string.device_id_check_success, Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@DeviceIdActivity, MainActivity::class.java))
                finish()
            } else {
                binding.tvDeviceIdError.text = result.message.ifBlank {
                    getString(R.string.device_id_check_failed_default)
                }
            }
        }
    }
}
