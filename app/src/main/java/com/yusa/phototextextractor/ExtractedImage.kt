package com.yusa.phototextextractor

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "extracted")
data class ExtractedImage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "baseimage")
    val image: String?,
    @ColumnInfo(name = "extractedtext")
    val text: String?,
    @ColumnInfo(name = "date")
    val date: String?
)
