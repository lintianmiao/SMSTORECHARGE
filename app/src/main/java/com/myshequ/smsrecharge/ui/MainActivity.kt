package com.myshequ.smsrecharge.ui

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.myshequ.smsrecharge.R
import com.myshequ.smsrecharge.databinding.ActivityMainBinding
import com.myshequ.smsrecharge.util.AppSettings
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: RecordListViewModel by viewModels()
    private lateinit var adapter: RecordAdapter

    private var startCalendar: Calendar? = null
    private var endCalendar: Calendar? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshPermissionTip()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 未绑定设备号时跳转到设备号验证页面，不展示主界面，不启动拦截消息功能
        if (!AppSettings.hasDeviceId(this)) {
            startActivity(Intent(this, DeviceIdActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupList()
        setupDateFilter()
        setupPermissionTip()

        viewModel.records.observe(this) { list ->
            adapter.submitList(list)
            binding.tvEmpty.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            binding.tvTotalCount.text = getString(R.string.total_records, list.size)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionTip()
    }

    private fun setupToolbar() {
        binding.toolbar.inflateMenu(R.menu.menu_main)
        binding.toolbar.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.action_clear_messages -> {
                    showClearMessagesDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun showClearMessagesDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.clear_messages_days_hint)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        val paddingPx = (16 * resources.displayMetrics.density).toInt()
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(paddingPx, paddingPx / 2, paddingPx, 0)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.clear_messages_dialog_title)
            .setMessage(R.string.clear_messages_dialog_message)
            .setView(container)
            .setPositiveButton(R.string.clear_messages_confirm) { _, _ ->
                val days = input.text.toString().trim().toIntOrNull()
                if (days == null || days <= 0) {
                    Toast.makeText(this, R.string.clear_messages_days_empty_tip, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.clearMessagesBefore(days) { deletedCount ->
                    Toast.makeText(this, getString(R.string.clear_messages_success, deletedCount), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.clear_messages_cancel, null)
            .show()
    }

    private fun setupList() {
        adapter = RecordAdapter { record ->
            val intent = Intent(this, RecordDetailActivity::class.java)
            intent.putExtra(RecordDetailActivity.EXTRA_RECORD_ID, record.id)
            startActivity(intent)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupDateFilter() {
        binding.tvStartDate.setOnClickListener {
            pickDate { calendar ->
                startCalendar = calendar
                binding.tvStartDate.text = formatDate(calendar)
            }
        }
        binding.tvEndDate.setOnClickListener {
            pickDate { calendar ->
                endCalendar = calendar
                binding.tvEndDate.text = formatDate(calendar)
            }
        }
        binding.btnQuery.setOnClickListener {
            val start = startCalendar
            val end = endCalendar
            if (start == null || end == null) {
                android.widget.Toast.makeText(this, R.string.select_date, android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val startMillis = (start.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val endMillis = (end.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            viewModel.filterByDateRange(startMillis, endMillis)
        }
        binding.btnQueryAll.setOnClickListener {
            startCalendar = null
            endCalendar = null
            binding.tvStartDate.text = getString(R.string.filter_start_date)
            binding.tvEndDate.text = getString(R.string.filter_end_date)
            viewModel.showAll()
        }
    }

    private fun pickDate(onPicked: (Calendar) -> Unit) {
        val now = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val calendar = Calendar.getInstance()
                calendar.set(year, month, dayOfMonth)
                onPicked(calendar)
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun formatDate(calendar: Calendar): String {
        return String.format(
            Locale.CHINA, "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun setupPermissionTip() {
        binding.btnGrantSmsPermission.setOnClickListener {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
            )
        }
        binding.btnGrantNotificationPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun refreshPermissionTip() {
        val smsGranted = hasSmsPermission()
        binding.layoutSmsPermissionTip.visibility = if (smsGranted) android.view.View.GONE else android.view.View.VISIBLE

        val notificationGranted = isNotificationServiceEnabled()
        binding.layoutNotificationPermissionTip.visibility = if (notificationGranted) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun hasSmsPermission(): Boolean {
        val receiveGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val readGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        return receiveGranted && readGranted
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (i in names.indices) {
                val cn = ComponentName.unflattenFromString(names[i])
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.packageName)) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
