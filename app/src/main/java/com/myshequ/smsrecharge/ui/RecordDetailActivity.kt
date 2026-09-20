package com.myshequ.smsrecharge.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.myshequ.smsrecharge.R
import com.myshequ.smsrecharge.data.AppDatabase
import com.myshequ.smsrecharge.data.SmsRecord
import com.myshequ.smsrecharge.databinding.ActivityRecordDetailBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordDetailBinding
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
        if (recordId < 0) {
            finish()
            return
        }

        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(applicationContext).smsRecordDao()
            val record = dao.getById(recordId)
            if (record != null) {
                bind(record)
            } else {
                finish()
            }
        }
    }

    private fun bind(record: SmsRecord) {
        val sourceText = if (record.messageSource == SmsRecord.SOURCE_APP) {
            getString(R.string.source_app)
        } else {
            getString(R.string.source_sms)
        }
        val messageTypeText = when (record.messageType) {
            SmsRecord.MESSAGE_TYPE_DUPLICATE_MESSAGE -> getString(R.string.message_type_duplicate_message)
            SmsRecord.MESSAGE_TYPE_DUPLICATE_ORDER -> getString(R.string.message_type_duplicate_order)
            else -> getString(R.string.message_type_new)
        }
        binding.tvTime.text = getString(R.string.label_time, timeFormat.format(Date(record.receivedTime)))
        binding.tvMessageSource.text = getString(R.string.label_message_source, sourceText)
        binding.tvMessageType.text = getString(R.string.label_message_type, messageTypeText)
        binding.tvPhone.text = getString(R.string.label_phone, record.senderAddress)
        binding.tvOrderNo.text = getString(R.string.label_order_no, record.orderNo.ifBlank { getString(R.string.value_not_parsed) })
        binding.tvCardNo.text = getString(R.string.label_card_no, record.cardNo.ifBlank { getString(R.string.value_not_generated) })
        binding.tvMoney.text = getString(R.string.label_money, record.money.ifBlank { getString(R.string.value_not_parsed) })
        binding.tvStatus.text = when (record.status) {
            SmsRecord.STATUS_SUCCESS -> getString(R.string.label_status_success)
            SmsRecord.STATUS_FAIL -> getString(R.string.label_status_fail)
            SmsRecord.STATUS_UNPARSED -> getString(R.string.label_status_unparsed)
            else -> getString(R.string.label_status_pending)
        }
        binding.tvSmsContent.text = record.smsBody
        binding.tvApiMessage.text = record.apiMessage
        binding.tvApiRaw.text = record.apiRawResponse
    }

    companion object {
        const val EXTRA_RECORD_ID = "extra_record_id"
    }
}
