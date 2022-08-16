package com.yusa.phototextextractor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel (appObj: Application) : AndroidViewModel(appObj) {
    private val extractedRepository : ExtractedRepository = ExtractedRepository(appObj)
    fun getAllExtracted(): LiveData<List<ExtractedImage>> {
        return extractedRepository.readAllData
    }
    fun addExtracted(extractedImage: ExtractedImage){
        viewModelScope.launch {
            extractedRepository.addExtracted(extractedImage)
        }
    }
    fun deleteExtracted(extractedImage: ExtractedImage){
        viewModelScope.launch {
            extractedRepository.deleteExtracted(extractedImage)
        }
    }
    fun deleteAllExtracted(){
        viewModelScope.launch {
            extractedRepository.deleteAllExtracted()
        }
    }


}