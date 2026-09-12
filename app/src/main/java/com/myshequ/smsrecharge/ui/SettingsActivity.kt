package com.myshequ.smsrecharge.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.myshequ.smsrecharge.R
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

        loadCurrentSettings()

        binding.btnSave.setOnClickListener { saveSettings() }
        binding.btnResetDefault.setOnClickListener { resetDefault() }
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

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun loadCurrentSettings() {
        binding.etSmsSources.setText(AppSettings.getSmsSourcesRaw(this))
        binding.etKeywords.setText(AppSettings.getKeywordsRaw(this))
        binding.etOrderKeyword.setText(AppSettings.getOrderKeyword(this))
        binding.etOrderLength.setText(AppSettings.getOrderLength(this).toString())
        binding.etMoneyKeyword.setText(AppSettings.getMoneyKeyword(this))
        binding.etApiUrl.setText(AppSettings.getApiUrl(this))
        binding.etCheckDeviceIdUrl.setText(AppSettings.getCheckDeviceIdUrl(this))
        val deviceId = AppSettings.getDeviceId(this)
        binding.tvDeviceId.text = getString(
            R.string.settings_device_id_label,
            deviceId.ifBlank { getString(R.string.settings_device_id_unbound) }
        )
    }

    private fun saveSettings() {
        val smsSources = binding.etSmsSources.text.toString().trim()
        val keywords = binding.etKeywords.text.toString().trim()
        val orderKeyword = binding.etOrderKeyword.text.toString().trim()
        val orderLength = binding.etOrderLength.text.toString().trim().toIntOrNull()
        val moneyKeyword = binding.etMoneyKeyword.text.toString().trim()
        val apiUrl = binding.etApiUrl.text.toString().trim()
        val checkDeviceIdUrl = binding.etCheckDeviceIdUrl.text.toString().trim()

        if (orderKeyword.isEmpty()) {
            Toast.makeText(this, R.string.settings_order_keyword_empty_tip, Toast.LENGTH_SHORT).show()
            return
        }
        if (orderLength == null || orderLength <= 0) {
            Toast.makeText(this, R.string.settings_order_length_invalid_tip, Toast.LENGTH_SHORT).show()
            return
        }

        AppSettings.setSmsSources(this, smsSources)
        AppSettings.setKeywords(this, keywords)
        AppSettings.setOrderKeyword(this, orderKeyword)
        AppSettings.setOrderLength(this, orderLength)
        AppSettings.setMoneyKeyword(this, moneyKeyword)
        AppSettings.setApiUrl(this, apiUrl)
        AppSettings.setCheckDeviceIdUrl(this, checkDeviceIdUrl)

        Toast.makeText(this, R.string.settings_saved_tip, Toast.LENGTH_SHORT).show()
    }

    private fun resetDefault() {
        AppSettings.resetToDefault(this)
        loadCurrentSettings()
        Toast.makeText(this, R.string.settings_saved_tip, Toast.LENGTH_SHORT).show()
    }

    private fun testParse() {
        // 使用当前输入框中的规则（先临时保存再解析），保证测试的是即将生效的规则
        val smsSources = binding.etSmsSources.text.toString().trim()
        val keywords = binding.etKeywords.text.toString().trim()
        val orderKeyword = binding.etOrderKeyword.text.toString().trim()
        val orderLength = binding.etOrderLength.text.toString().trim().toIntOrNull()
        val moneyKeyword = binding.etMoneyKeyword.text.toString().trim()
        if (orderKeyword.isEmpty() || orderLength == null || orderLength <= 0) {
            binding.tvTestResult.text = getString(R.string.settings_test_fill_required_tip)
            return
        }
        AppSettings.setSmsSources(this, smsSources)
        AppSettings.setKeywords(this, keywords)
        AppSettings.setOrderKeyword(this, orderKeyword)
        AppSettings.setOrderLength(this, orderLength)
        AppSettings.setMoneyKeyword(this, moneyKeyword)

        val testSms = binding.etTestSms.text.toString()
        val testSender = AppSettings.getSmsSources(this).firstOrNull().orEmpty()
        val result = SmsParser.parse(this, testSms, testSender)
        val notParsedText = getString(R.string.test_result_not_parsed)
        binding.tvTestResult.text = if (result.matched) {
            if (result.parsed) {
                getString(
                    R.string.test_result_matched_parsed,
                    result.orderNo,
                    result.cardNo,
                    result.money.ifBlank { notParsedText }
                )
            } else {
                getString(
                    R.string.test_result_matched_unparsed,
                    result.money.ifBlank { notParsedText },
                    result.reason
                )
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
