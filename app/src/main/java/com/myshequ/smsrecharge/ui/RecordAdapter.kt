package com.myshequ.smsrecharge.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.myshequ.smsrecharge.R
import com.myshequ.smsrecharge.data.SmsRecord
import com.myshequ.smsrecharge.databinding.ItemSmsRecordBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordAdapter(private val onItemClick: (SmsRecord) -> Unit) :
    ListAdapter<SmsRecord, RecordAdapter.RecordViewHolder>(DIFF_CALLBACK) {

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): RecordViewHolder {
        val binding = ItemSmsRecordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecordViewHolder(private val binding: ItemSmsRecordBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(record: SmsRecord) {
            val context = binding.root.context
            val sourceText = if (record.messageSource == SmsRecord.SOURCE_APP) {
                context.getString(R.string.source_app)
            } else {
                context.getString(R.string.source_sms)
            }
            val messageTypeText = when (record.messageType) {
                SmsRecord.MESSAGE_TYPE_DUPLICATE_MESSAGE -> context.getString(R.string.message_type_duplicate_message)
                SmsRecord.MESSAGE_TYPE_DUPLICATE_ORDER -> context.getString(R.string.message_type_duplicate_order)
                else -> context.getString(R.string.message_type_new)
            }
            binding.tvTime.text = context.getString(R.string.label_time, timeFormat.format(Date(record.receivedTime)))
            binding.tvMessageSource.text = context.getString(R.string.label_message_source, sourceText)
            binding.tvPhone.text = context.getString(R.string.label_phone, record.senderAddress)
            binding.tvOrderNo.text = context.getString(R.string.label_order_no, record.orderNo.ifBlank { "未解析" })
            binding.tvCardNo.text = context.getString(R.string.label_card_no, record.cardNo.ifBlank { "未生成" })
            binding.tvMoney.text = context.getString(R.string.label_money, record.money.ifBlank { "未解析" })

            when (record.status) {
                SmsRecord.STATUS_SUCCESS -> {
                    binding.tvStatus.text = "${context.getString(R.string.label_status_success)} · $messageTypeText"
                    binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.colorSuccess))
                }
                SmsRecord.STATUS_FAIL -> {
                    binding.tvStatus.text = "${context.getString(R.string.label_status_fail)} · $messageTypeText"
                    binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.colorFail))
                }
                SmsRecord.STATUS_UNPARSED -> {
                    binding.tvStatus.text = "${context.getString(R.string.label_status_unparsed)} · $messageTypeText"
                    binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.colorTextSecondary))
                }
                else -> {
                    binding.tvStatus.text = "${context.getString(R.string.label_status_pending)} · $messageTypeText"
                    binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.colorTextSecondary))
                }
            }

            binding.root.setOnClickListener { onItemClick(record) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SmsRecord>() {
            override fun areItemsTheSame(oldItem: SmsRecord, newItem: SmsRecord) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: SmsRecord, newItem: SmsRecord) = oldItem == newItem
        }
    }
}
