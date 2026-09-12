package com.myshequ.smsrecharge.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SmsRecordDao {

    @Insert
    suspend fun insert(record: SmsRecord): Long

    @Update
    suspend fun update(record: SmsRecord)

    @Query("SELECT * FROM sms_record ORDER BY receivedTime DESC")
    fun getAll(): LiveData<List<SmsRecord>>

    @Query(
        "SELECT * FROM sms_record WHERE receivedTime BETWEEN :startTime AND :endTime " +
            "ORDER BY receivedTime DESC"
    )
    fun getByDateRange(startTime: Long, endTime: Long): LiveData<List<SmsRecord>>

    @Query("SELECT * FROM sms_record WHERE id = :id")
    suspend fun getById(id: Long): SmsRecord?

    @Query("SELECT COUNT(*) FROM sms_record WHERE smsBody = :smsBody AND receivedTime BETWEEN :startTime AND :endTime")
    suspend fun countSameMessageInRange(smsBody: String, startTime: Long, endTime: Long): Int

    @Query("SELECT COUNT(*) FROM sms_record WHERE orderNo = :orderNo AND orderNo != '' AND receivedTime BETWEEN :startTime AND :endTime")
    suspend fun countSameOrderInRange(orderNo: String, startTime: Long, endTime: Long): Int

    @Query("SELECT COUNT(*) FROM sms_record")
    fun getCount(): LiveData<Int>

    @Query("DELETE FROM sms_record WHERE receivedTime < :beforeTime")
    suspend fun deleteBefore(beforeTime: Long): Int
}
