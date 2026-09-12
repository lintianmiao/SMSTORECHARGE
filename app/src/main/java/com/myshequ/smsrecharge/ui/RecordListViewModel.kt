package com.myshequ.smsrecharge.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.myshequ.smsrecharge.data.SmsRecord
import com.myshequ.smsrecharge.data.SmsRecordRepository
import kotlinx.coroutines.launch

class RecordListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SmsRecordRepository(application)

    private val result = MediatorLiveData<List<SmsRecord>>()
    private var currentSource: LiveData<List<SmsRecord>>? = null

    val records: LiveData<List<SmsRecord>> = result

    init {
        showAll()
    }

    fun showAll() {
        switchSource(repository.getAll())
    }

    fun filterByDateRange(startTime: Long, endTime: Long) {
        switchSource(repository.getByDateRange(startTime, endTime))
    }

    private fun switchSource(newSource: LiveData<List<SmsRecord>>) {
        currentSource?.let { result.removeSource(it) }
        currentSource = newSource
        result.addSource(newSource) { result.value = it }
    }

    /**
     * 清除指定天数之前的消息。days 为多少天前（以当前时间为基准，接收时间早于该时间点的记录均会被删除）。
     * 删除完成后通过 onComplete 回传实际删除的条数，Room 的 LiveData 会自动刷新列表，无需手动重新查询。
     */
    fun clearMessagesBefore(days: Int, onComplete: (Int) -> Unit) {
        val beforeTime = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        viewModelScope.launch {
            val deletedCount = repository.deleteBefore(beforeTime)
            onComplete(deletedCount)
        }
    }
}
