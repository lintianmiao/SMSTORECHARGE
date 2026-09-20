package com.myshequ.smsrecharge.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.myshequ.smsrecharge.R
import com.myshequ.smsrecharge.data.TradeMode
import com.myshequ.smsrecharge.databinding.ActivitySettingsBinding
import com.myshequ.smsrecharge.util.AppSettings
import com.myshequ.smsrecharge.util.SmsParser

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        if (granted) {
            Toast.makeText(this, R.string.settings_saved_tip, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnOpenNotificationListener.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        binding.btnGrantSmsPermission.setOnClickListener {
            requestSmsPermission()
        }
        binding.btnTestParse.setOnClickListener {
            hideKeyboard()
            testParse()
        }
        binding.btnRebindDeviceId.setOnClickListener {
            startActivity(Intent(this, DeviceIdActivity::class.java))
        }

        // 点击空白区域隐藏键盘
        binding.layoutContent.setOnClickListener { hideKeyboard() }
    }

    override fun onResume() {
        super.onResume()
        // 每次回到本页面时刷新设备号与交易模式展示（重新绑定设备号后模式可能已更新）
        loadCurrentSettings()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun loadCurrentSettings() {
        val deviceId = AppSettings.getDeviceId(this)
        binding.tvDeviceId.text = getString(
            R.string.settings_device_id_label,
            deviceId.ifBlank { getString(R.string.settings_device_id_unbound) }
        )
        renderTradeModes(AppSettings.getTradeModes(this))
    }

    /**
     * 以按钮形式展示所有交易模式的名称：点击某个模式按钮后，在下方界面展示该模式的规则信息。
     */
    private fun renderTradeModes(modes: List<TradeMode>) {
        binding.layoutModeButtons.removeAllViews()
        if (modes.isEmpty()) {
            binding.tvTradeModesEmpty.visibility = View.VISIBLE
            binding.tvModeInfoTitle.visibility = View.GONE
            binding.layoutModeInfo.visibility = View.GONE
            return
        }

        binding.tvTradeModesEmpty.visibility = View.GONE
        binding.tvModeInfoTitle.visibility = View.VISIBLE
        binding.layoutModeInfo.visibility = View.VISIBLE

        val marginPx = (8 * resources.displayMetrics.density).toInt()
        modes.forEachIndexed { index, mode ->
            val button = MaterialButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { if (index > 0) topMargin = marginPx }
                isAllCaps = false
                text = mode.name.ifBlank { getString(R.string.trade_mode_unnamed) }
                setOnClickListener { showModeInfo(mode) }
            }
            binding.layoutModeButtons.addView(button)
        }

        // 默认展示第一个模式的信息
        showModeInfo(modes.first())
    }

    /**
     * 在下方界面展示指定交易模式的规则信息（取代原有解析规则字段）。
     */
    private fun showModeInfo(mode: TradeMode) {
        val notSet = getString(R.string.trade_mode_rule_empty)
        val notifyTypeText = if (mode.notifyType == TradeMode.NOTIFY_TYPE_APP) {
            getString(R.string.source_app)
        } else {
            getString(R.string.source_sms)
        }
        val modeName = mode.name.ifBlank { getString(R.string.trade_mode_unnamed) }
        binding.tvModeName.text = getString(R.string.trade_mode_field_name, modeName)
        binding.tvModeNotifyType.text = getString(R.string.trade_mode_field_notify_type, notifyTypeText)
        binding.tvModeSource.text = getString(R.string.trade_mode_field_source, mode.tradeInfoSource.ifBlank { notSet })
        binding.tvModeJudge.text = getString(R.string.trade_mode_field_judge, mode.tradeJudgeRule.ifBlank { notSet })
        binding.tvModeTradeNo1.text = getString(R.string.trade_mode_field_trade_no1, mode.tradeNoParseRule1.ifBlank { notSet })
        binding.tvModeTradeNo2.text = getString(R.string.trade_mode_field_trade_no2, mode.tradeNoParseRule2.ifBlank { notSet })
        binding.tvModeCardNo1.text = getString(R.string.trade_mode_field_card_no1, mode.cardNoParseRule1.ifBlank { notSet })
        binding.tvModeCardNo2.text = getString(R.string.trade_mode_field_card_no2, mode.cardNoParseRule2.ifBlank { notSet })
        binding.tvModeMoney1.text = getString(R.string.trade_mode_field_money1, mode.monyParseRule1.ifBlank { notSet })
        binding.tvModeMoney2.text = getString(R.string.trade_mode_field_money2, mode.monyParseRule2.ifBlank { notSet })
    }

    private fun testParse() {
        // 按所选消息类型（短信/APP通知），使用后台下发的交易模式规则进行解析测试
        val testSource = binding.etTestSource.text.toString().trim()
        val testContent = binding.etTestSms.text.toString()
        val notifyType = if (binding.rbTestApp.isChecked) {
            TradeMode.NOTIFY_TYPE_APP
        } else {
            TradeMode.NOTIFY_TYPE_SMS
        }

        val result = SmsParser.parse(this, testContent, testSource, notifyType)
        binding.tvTestResult.text = if (result.matched) {
            val modeName = result.modeName.ifBlank { getString(R.string.trade_mode_unnamed) }
            if (result.parsed) {
                val amountText = result.money.ifBlank { getString(R.string.test_result_amount_absent) }
                getString(R.string.test_result_matched_parsed, modeName, result.orderNo, result.cardNo, amountText)
            } else {
                getString(R.string.test_result_matched_unparsed, modeName, result.reason)
            }
        } else {
            getString(R.string.test_result_unmatched, result.reason)
        }
    }

    private fun requestSmsPermission() {
        val receiveGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val readGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (receiveGranted && readGranted) {
            Toast.makeText(this, R.string.settings_saved_tip, Toast.LENGTH_SHORT).show()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
            )
        }
    }
}
