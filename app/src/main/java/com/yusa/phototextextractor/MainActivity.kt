package com.yusa.phototextextractor

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.WindowManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yusa.phototextextractor.ui.theme.PhotoTextExtractorTheme
import java.io.File
import java.io.IOException
import java.time.LocalDateTime


class MainActivity : AppCompatActivity() {
    private val lastUri = mutableStateOf(Uri.EMPTY)
    private var toBeAdded = mutableStateOf(ExtractedImage(0,"","",""))
    private val isSaved = mutableStateOf(false)
    private val isPermissionGranted = mutableStateOf(false)
    private val isGallery = mutableStateOf(false)
    private val searchText = mutableStateOf("")
    private val searchedList = mutableListOf<ExtractedImage>()
    val visionOutText = mutableStateOf("load something")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        var extractedImages: List<ExtractedImage> = listOf()
        val observer = Observer<List<ExtractedImage>>{extractedImageList->
            extractedImages = extractedImageList
        }
        mainViewModel.getAllExtracted().observe(this,observer)
        setContent {

            //CaptureImageFromCamera(isPermissionGranted,isSaved,lastUri, mainViewModel,toBeAdded,visionOutText,searchText,isGallery)
            MainUi(
                isPermissionGranted,mainViewModel, searchText, isGallery, searchedList)
        }

    }
}
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun MainUi(
    isCameraAccessGranted: MutableState<Boolean>,
    mainViewModel: MainViewModel,
    searchText: MutableState<String>,
    isGallery: MutableState<Boolean>,
    searchedList: MutableList<ExtractedImage>,){
    PhotoTextExtractorTheme(darkTheme = true){
        Scaffold() {
            val context = LocalContext.current
            val launcherWithUri = rememberLauncherForActivityResult(
                contract = TakePictureWithUriReturnContract()){
                if (it.first){
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    try {
                        val image: InputImage
                        image = InputImage.fromFilePath(context, it.second)
                        val result = recognizer.process(image)
                            .addOnSuccessListener { visionText ->

                                var combinedString = ""
                                val parts = visionText.text.split("\n")
                                for (part in parts){
                                    val words = part.split(" ")
                                    for (word in words){
                                        combinedString += "$word-*-"
                                    }
                                }
                                combinedString.dropLast(3)
                                val rnds = (0..100000).random()
                                val c = LocalDateTime.now()
                                val d:String
                                if (c.dayOfMonth<10){
                                    d = "0"+c.dayOfMonth.toString()}
                                else d = c.dayOfMonth.toString()

                                val m :String
                                if (c.monthValue<10){
                                    m = "0"+c.monthValue.toString()
                                }
                                else m = c.monthValue.toString()

                                val y = c.year.toString()


                                val imageInfo = ExtractedImage(rnds,
                                    it.second.toString(),combinedString,"$d.$m.$y")
                                mainViewModel.addExtracted(imageInfo)
                                // Task completed successfully
                                // ...
                            }
                            .addOnFailureListener { e ->
                                // Task failed with an exception
                                // ...
                            }

                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } }
            val launcherOfGallery = rememberLauncherForActivityResult(
                contract = GetPicturesContract()){
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                try {
                    context.contentResolver.takePersistableUriPermission(it!!,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val image: InputImage
                    image = InputImage.fromFilePath(context, it)
                    val result = recognizer.process(image)
                        .addOnSuccessListener { visionText ->

                            var combinedString = ""
                            val parts = visionText.text.split("\n")
                            for (part in parts){
                                val words = part.split(" ")
                                for (word in words){
                                    combinedString += "$word-*-"
                                }
                            }
                            combinedString.dropLast(3)
                            val rnds = (0..100000).random()
                            val c = LocalDateTime.now()
                            val d:String
                            if (c.dayOfMonth<10){
                                d = "0"+c.dayOfMonth.toString()}
                            else d = c.dayOfMonth.toString()

                            val m :String
                            if (c.monthValue<10){
                                m = "0"+c.monthValue.toString()
                            }
                            else m = c.monthValue.toString()

                            val y = c.year.toString()


                            val imageInfo  = ExtractedImage(rnds,
                                it.toString(),combinedString,"$d.$m.$y")
                            mainViewModel.addExtracted(imageInfo)
                            // Task completed successfully
                            // ...
                        }
                        .addOnFailureListener { e ->
                            // Task failed with an exception
                            // ...
                        }

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    val uri =  uriProvider(context)
                    launcherWithUri.launch(uri)
                    isGallery.value = false

                } else {
                    // Show dialog
                    println("zws") } }
            val permission = Manifest.permission.CAMERA

            Column(
                Modifier
                    .fillMaxSize(0.9f)
                    .padding(10.dp, 5.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    TextField(value = searchText.value, onValueChange = {
                        searchText.value = it

                    }, label = { Text(text = "Search")}, modifier = Modifier.width(180.dp))
                    Button(modifier = Modifier
                        .height(50.dp)
                        .width(100.dp), onClick = {
                        searchFromText(searchText,mainViewModel,searchedList)
                    }) {
                        Text(text = "Search with text", fontSize = 9.sp)
                    }
                    Button(modifier = Modifier
                        .height(50.dp)
                        .width(100.dp),onClick = {
                        /*TODO*/ //SEARCH FROM IMAGE
                    }) {
                        Text(text = "Search with image", fontSize = 9.sp)
                    }
                }
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.03f))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)) {
                    Column(modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.6f),horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Image Processing Options",modifier = Modifier
                                .fillMaxHeight(0.2f))
                        Button(modifier = Modifier
                            .height(60.dp)
                            .width(150.dp),onClick = {
                            getImageFromCamera(isCameraAccessGranted,context,launcherWithUri,isGallery,permission,launcher)
                            }) {
                            Text(text = "Process from camera", fontSize = 9.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(modifier = Modifier
                            .height(60.dp)
                            .width(150.dp),onClick = { getImageFromGallery(launcherOfGallery,isGallery) }) {
                            Text(text = "Process from device", fontSize = 9.sp)
                        }
                    }
                    Column(modifier = Modifier
                        .fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Database Options",modifier = Modifier
                            .fillMaxHeight(0.2f))
                        Button(modifier = Modifier
                            .height(60.dp)
                            .width(100.dp),onClick = { /*TODO*/ }) {
                            Text(text = "Load all db", fontSize = 9.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(modifier = Modifier
                            .height(60.dp)
                            .width(100.dp),onClick = { /*TODO*/ }) {
                            Text(text = "Kill all db", fontSize = 9.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.03f))
                    LazyColumn(modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceAround){
                        itemsIndexed(searchedList){
                            index, item ->
                            Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center){
                                AsyncImage(model = item.image, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.size(5.dp))
                        }
                    }

            }
        }
    }

}
fun searchFromText(
    searchText: MutableState<String>,
    mainViewModel: MainViewModel,
    searchedList: MutableList<ExtractedImage>
){
    searchedList.clear()
    val extractedImages : List<ExtractedImage> = mainViewModel.getAllExtracted().value!!
    val searchResult = mutableListOf<ExtractedImage>()
    for (image in extractedImages){
        if (image.text != null){
            if (image.text.contains(searchText.value, ignoreCase = true)){
                searchResult.add(image)
                searchedList.add(image)
            }}
    }
}
fun getImageFromCamera(
    isCameraAccessGranted: MutableState<Boolean>,
    context: Context,
    launcherWithUri: ManagedActivityResultLauncher<Uri, Pair<Boolean, Uri>>,
    isGallery: MutableState<Boolean>,
    permission: String,
    launcher: ManagedActivityResultLauncher<String, Boolean>
){
    if (isCameraAccessGranted.value){
        val uri = uriProvider(context)
        launcherWithUri.launch(uri)
        isGallery.value = false
    }
    else{checkAndRequestCameraPermission(context, permission, launcher)
        isCameraAccessGranted.value = true}
}
fun getImageFromGallery(
    launcherOfGallery: ManagedActivityResultLauncher<String, Uri?>,
    isGallery: MutableState<Boolean>
) {
    launcherOfGallery.launch("image/*")
    isGallery.value = true
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun CaptureImageFromCamera(
    isCameraAccessGranted: MutableState<Boolean>,
    isSaved: MutableState<Boolean>,
    lastUri: MutableState<Uri>,
    mainViewModel: MainViewModel,
    toBeAdded: MutableState<ExtractedImage>,
    visionOutText: MutableState<String>,
    searchText: MutableState<String>,
    isGallery: MutableState<Boolean>,
) {

    PhotoTextExtractorTheme(darkTheme = true) {
        Scaffold(content = {
            val context = LocalContext.current
            val x = createBitmap(1000,1000)

            val bitmap = remember {
                mutableStateOf(x)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally, content = {


//                    val launcher2 =
//                        rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) {
//                            if (it != null) {
//                                bitmap.value = it
//                            }
//                        }
//                    val launcher3 =
//                        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
//                            isSaved.value = it
//                        }
                    val launcherWithUri = rememberLauncherForActivityResult(
                        contract = TakePictureWithUriReturnContract()){
                        if (it.first){
                            lastUri.value = it.second
                        }
                    }
                    val launcherOfGallery = rememberLauncherForActivityResult(
                        contract = GetPicturesContract()
                    ){
                        lastUri.value = it!!
                    }

                    val permission = Manifest.permission.CAMERA
                    val launcher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            val uri =  uriProvider(context)
                            launcherWithUri.launch(uri)
                            isGallery.value = false

                        } else {
                            // Show dialog
                            println("zws")
                        }
                    }
                    Button(
                        onClick = {
                            if (isCameraAccessGranted.value){
                                val uri = uriProvider(context)
                                launcherWithUri.launch(uri)
                                isGallery.value = false
                            }
                            else{checkAndRequestCameraPermission(context, permission, launcher)
                                isCameraAccessGranted.value = true}

                        }

                    ) {
                        Text(text = "Open Camera")
                    }
                    Button(onClick = {

                        processImage(lastUri.value, context, visionOutText,toBeAdded)



                    }) {
                        Text(text = "Process the image")
                    }
                    Button(onClick = {
                        launcherOfGallery.launch("image/*")
                        isGallery.value = true

                    }) {
                        Text(text = "Load from Gallery")
                    }
                    Row() {
                        Button(onClick = {

                            if (isGallery.value){
                                context.contentResolver.takePersistableUriPermission(lastUri.value,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val imageInfo = toBeAdded.value
                            mainViewModel.addExtracted(imageInfo)
                        }) {
                            Text(text = "Add to db")
                        }
                        Button(onClick = {
                            var allText = ""
                            val extractedImages2 = mainViewModel.getAllExtracted()
                            for (item in extractedImages2.value!!){
                                allText += "${item.id}***"
                                allText+= item.image + "***"
                                allText+= item.date + "***"
                                allText += item.text + "******"
                            }
                            visionOutText.value = allText
                        }) {
                            Text(text = "Load the db")
                        }
                        Button(onClick = {
                            mainViewModel.deleteAllExtracted()
                        }) {
                            Text(text = "kill db")
                        }

                    }
                    Row() {
                        TextField(value = searchText.value, onValueChange = {
                            searchText.value = it

                        }, label = { Text(text = "Search")}, modifier = Modifier.fillMaxWidth(0.75f))
                        Button(onClick = {
                            val extractedImages : List<ExtractedImage> = mainViewModel.getAllExtracted().value!!
                            val searchResult = mutableListOf<ExtractedImage>()
                            for (image in extractedImages){
                                if (image.text != null){
                                if (image.text.contains(searchText.value, ignoreCase = true)){
                                    searchResult.add(image)
                                    lastUri.value = Uri.parse(image.image)
                                }}
                            }
                            var allText = ""
                            for (item in searchResult){
                                allText += "${item.id}***"
                                allText+= item.image + "***"
                                allText+= item.date + "***"
                                allText += item.text + "******"
                            }
                            visionOutText.value = allText

                        }) {
                            Text(text = "Search")
                        }
                    }
                    AsyncImage(model = lastUri.value, contentDescription = null, modifier = Modifier.size(300.dp,300.dp))

                    SelectionContainer() {
                        Text(text = visionOutText.value, modifier= Modifier.verticalScroll(
                            rememberScrollState()))
                    }
                }
            )


        })
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    // CaptureImageFromCamera()
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

fun uriProvider(context: Context): Uri {
    val c = LocalDateTime.now()
    val s = c.second.toString()
    val mi = c.minute.toString()
    val h = c.hour.toString()
    val d:String = if (c.dayOfMonth<10){
        "0"+c.dayOfMonth.toString()
    }
    else c.dayOfMonth.toString()
    val m :String = if (c.monthValue<10){
        "0"+c.monthValue.toString()
    }
    else c.monthValue.toString()
    val y = c.year.toString()
    val fileName = "$y$m$d$h$mi$s" +"textphoto.jpg"
    val path = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$fileName"
    val file = File(path);

    return FileProvider.getUriForFile(
        context, context.applicationContext.packageName + ".provider", file
    )
}

fun processImage(uri: Uri, context: Context, visionOutText: MutableState<String>, toBeAdded: MutableState<ExtractedImage>){
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    try {
        val image: InputImage
        image = InputImage.fromFilePath(context, uri)
        val result = recognizer.process(image)
            .addOnSuccessListener { visionText ->

                var combinedString = ""
                val parts = visionText.text.split("\n")
                for (part in parts){
                    val words = part.split(" ")
                    for (word in words){
                        combinedString += "$word-*-"
                    }
                }
                combinedString.dropLast(3)
                visionOutText.value = combinedString
                val rnds = (0..1000).random()
                val c = LocalDateTime.now()
                val d:String
                if (c.dayOfMonth<10){
                    d = "0"+c.dayOfMonth.toString()}
                else d = c.dayOfMonth.toString()

                val m :String
                if (c.monthValue<10){
                    m = "0"+c.monthValue.toString()
                }
                else m = c.monthValue.toString()

                val y = c.year.toString()


                toBeAdded.value= ExtractedImage(rnds,
                    uri.toString(),combinedString,"$d.$m.$y")
                // Task completed successfully
                // ...
            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                // ...
            }

    } catch (e: IOException) {
        e.printStackTrace()
    }
}

class TakePictureWithUriReturnContract : ActivityResultContract<Uri, Pair<Boolean, Uri>>() {

    private lateinit var imageUri: Uri

    @CallSuper
    override fun createIntent(context: Context, input: Uri): Intent {
        imageUri = input
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, input)
    }

    override fun getSynchronousResult(
        context: Context,
        input: Uri
    ): SynchronousResult<Pair<Boolean, Uri>>? = null

    @Suppress("AutoBoxing")
    override fun parseResult(resultCode: Int, intent: Intent?): Pair<Boolean, Uri> {
        return (resultCode == Activity.RESULT_OK) to imageUri
    }
}
class GetPicturesContract : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input)
    }

    override fun getSynchronousResult(
        context: Context,
        input: String
    ): SynchronousResult<Uri?>? {
        return null
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
    }

}
