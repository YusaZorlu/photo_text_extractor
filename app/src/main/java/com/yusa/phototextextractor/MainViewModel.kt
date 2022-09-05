package com.yusa.phototextextractor

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel (appObj: Application) : AndroidViewModel(appObj) {
    private val extractedRepository : ExtractedRepository = ExtractedRepository(appObj)
    val isCameraAccessGranted = mutableStateOf(false)
    val searchText = mutableStateOf("")
    val searchedList = mutableStateListOf<ExtractedImage>()
    val isGallery = mutableStateOf(false)

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
    fun searchFromText(
    ){
        val extractedImages : List<ExtractedImage> = this.getAllExtracted().value!!
        val searchResult = SnapshotStateList<ExtractedImage>()
        for (image in extractedImages){
            if (image.text != null){
                if (image.text.contains(this.searchText.value, ignoreCase = true)){
                    searchResult.add(image)
                }}
            this.searchedList.swapList(searchResult)
        }
    }
    fun searchFromImage(
        launcherSelectFromGallery: ManagedActivityResultLauncher<String, Uri?>,
    ) {
        launcherSelectFromGallery.launch("image/*")
    }
    fun getImageFromCamera(
        context: Context,
        launcherWithUri: ManagedActivityResultLauncher<Uri, Pair<Boolean, Uri>>,
        permission: String,
        launcher: ManagedActivityResultLauncher<String, Boolean>
    ){
        if (isCameraAccessGranted.value){
            val uri = uriProvider(context)
            launcherWithUri.launch(uri)
            this.isGallery.value = false
        }
        else{checkAndRequestCameraPermission(context, permission, launcher)
            isCameraAccessGranted.value = true}
    }
    fun getImageFromGallery(
        launcherOfGallery: ManagedActivityResultLauncher<String, Uri?>,
    ) {
        launcherOfGallery.launch("image/*")
        this.isGallery.value = true
    }
    fun loadAllDatabase() {
        val allImagesList = SnapshotStateList<ExtractedImage>()
        val extractedImages : List<ExtractedImage> = this.getAllExtracted().value!!
        for (image in extractedImages){
            allImagesList.add(image)
        }
        this.searchedList.swapList(allImagesList)
    }
    fun killAllDatabase(){
        this.deleteAllExtracted()
    }

    fun checkAndRequestCameraPermission(
        context: Context,
        permission: String,
        launcher: ManagedActivityResultLauncher<String, Boolean>
    ) {
        val permissionCheckResult = ContextCompat.checkSelfPermission(context, permission)
        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
            // Open camera because permission is already granted
        } else {
            // Request a permission
            launcher.launch(permission)
        }
    }
}