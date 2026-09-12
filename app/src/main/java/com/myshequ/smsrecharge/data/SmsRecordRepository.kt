package com.myshequ.smsrecharge.data

import android.app.Application
import androidx.lifecycle.LiveData

class SmsRecordRepository(application: Application) {
    private val dao = AppDatabase.getInstance(application).smsRecordDao()

    fun getAll(): LiveData<List<SmsRecord>> = dao.getAll()

    fun getByDateRange(startTime: Long, endTime: Long): LiveData<List<SmsRecord>> =
        dao.getByDateRange(startTime, endTime)

    suspend fun deleteBefore(beforeTime: Long): Int = dao.deleteBefore(beforeTime)
}
