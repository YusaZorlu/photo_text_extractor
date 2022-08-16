package com.yusa.phototextextractor

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class ExtractedRepository(application: Application) {
    private val extractedDao: ExtractedDao
    init {
        val database = ExtractedDatabase.getDatabase(application)
        extractedDao = database.extractedDao()
    }
    val readAllData: LiveData<List<ExtractedImage>> = extractedDao.getAll()
    suspend fun addExtracted(extractedImage : ExtractedImage) {
        extractedDao.insert(extractedImage)
    }
    suspend fun updateExtracted(extractedImage: ExtractedImage) {
        extractedDao.update(extractedImage)
    }
    suspend fun deleteExtracted(extractedImage: ExtractedImage) {
        extractedDao.delete(extractedImage)
    }
    suspend fun deleteAllExtracted() {
        extractedDao.deleteAllExtractedImages()
    }

}