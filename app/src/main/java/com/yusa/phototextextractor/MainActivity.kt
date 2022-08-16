package com.yusa.phototextextractor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.WindowManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yusa.phototextextractor.ui.theme.PhotoTextExtractorTheme
import java.io.File
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private var bigUri = Uri.EMPTY
    private val lastUri = mutableStateOf(bigUri)

    private val isSaved = mutableStateOf(false)
    private val isPermissionGranted = mutableStateOf(false)
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
        setContent {

            CaptureImageFromCamera(isPermissionGranted,isSaved,lastUri,getContent,mainViewModel,extractedImages)
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
    extractedImages: List<ExtractedImage>
) {

    PhotoTextExtractorTheme(darkTheme = true) {
        Scaffold(content = {

            val context = LocalContext.current
            val x = createBitmap(1000,1000)
            val visionOutText = remember {
                mutableStateOf("helo")
            }
            val bitmap = remember {
                mutableStateOf(x)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally, content = {


                    val launcher2 =
                        rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) {
                            if (it != null) {
                                bitmap.value = it
                            }
                        }
                    val launcher3 =
                        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
                            isSaved.value = it

                        }

                    val permission = Manifest.permission.CAMERA
                    val launcher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            val uri =  uriProvider(context)
                            launcher3.launch(uri)

                        } else {
                            // Show dialog
                            println("zws")
                        }
                    }
                    Button(
                        onClick = {
                            if (isCameraAccessGranted.value){
                                val uri = uriProvider(context)
                                launcher3.launch(uri)
                                lastUri.value = uri
                            }
                            else{checkAndRequestCameraPermission(context, permission, launcher)
                                isCameraAccessGranted.value = true}

                        }

                    ) {
                        Text(text = "Open Camera")
                    }
                    Button(onClick = {
                        processImage(lastUri.value,context,visionOutText)

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
                            val rnds = (0..100).random()
                            mainViewModel.addExtracted(ExtractedImage(rnds,"imagestring","this is image ${rnds}","01.01.2000"))
                        }) {
                            Text(text = "Add to db")
                        }
                        Button(onClick = {
                            var allText = ""
                            for (item in extractedImages){
                                allText+= item.text
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
                    Text(text = visionOutText.value)
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
    val c = Calendar.getInstance()
    val x = c.get(Calendar.MILLISECOND)
    val rnds = (0..100).random()
    val y = (x* 100) + rnds
    val fileName = "extracted" + y +"textphoto.jpg"
    val path = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$fileName"
    val file = File(path);

    return FileProvider.getUriForFile(
        context, context.applicationContext.packageName + ".provider", file
    )
}

fun processImage(uri: Uri, context: Context,visionOutText: MutableState<String>){
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    try {
        val image: InputImage
        image = InputImage.fromFilePath(context, uri)
        val result = recognizer.process(image)
            .addOnSuccessListener { visionText ->
                visionOutText.value = visionText.text
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