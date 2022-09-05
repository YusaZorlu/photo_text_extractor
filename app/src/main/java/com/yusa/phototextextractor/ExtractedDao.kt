package com.yusa.phototextextractor

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ExtractedDao {
    @Query("SELECT * from extracted")
    fun getAll(): LiveData<List<ExtractedImage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item:ExtractedImage)

    @Update
    suspend fun update(item:ExtractedImage)

    @Delete
    suspend fun delete(item:ExtractedImage)

    @Query("DELETE FROM extracted")
    suspend fun deleteAllExtractedImages()
}