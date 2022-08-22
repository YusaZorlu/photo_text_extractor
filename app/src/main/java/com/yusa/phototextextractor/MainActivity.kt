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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    private val searchText = mutableStateOf("")
    val visionOutText = mutableStateOf("load something")
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { imageUri ->
            lastUri.value = imageUri
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        var extractedImages: List<ExtractedImage> = listOf()
        val observer = Observer<List<ExtractedImage>>{extractedImageList->
            extractedImages = extractedImageList
        }
        mainViewModel.getAllExtracted().observe(this,observer)
        lastUri.value = lastUri.value
        setContent {

            CaptureImageFromCamera(isPermissionGranted,isSaved,lastUri,getContent,
                mainViewModel,toBeAdded,visionOutText,searchText)
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun CaptureImageFromCamera(
    isCameraAccessGranted: MutableState<Boolean>,
    isSaved: MutableState<Boolean>,
    lastUri: MutableState<Uri>,
    getContent: ActivityResultLauncher<String>,
    mainViewModel: MainViewModel,
    toBeAdded: MutableState<ExtractedImage>,
    visionOutText: MutableState<String>,
    searchText: MutableState<String>,
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

                    val permission = Manifest.permission.CAMERA
                    val launcher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            val uri =  uriProvider(context)
                            launcherWithUri.launch(uri)

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
                        getContent.launch("image/*")


                    }) {
                        Text(text = "Load from Gallery")
                    }
                    Row() {
                        Button(onClick = {

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
