package com.yusa.phototextextractor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yusa.phototextextractor.ui.theme.PhotoTextExtractorTheme
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.math.log

class MainActivity : ComponentActivity() {
    private val isSaved = mutableStateOf(false)
    private val isPermissionGranted = mutableStateOf(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContent {
            CaptureImageFromCamera(isPermissionGranted,isSaved)
        }
    }
}

@Composable
fun CaptureImageFromCamera(isCameraAccessGranted: MutableState<Boolean>, isSaved: MutableState<Boolean>) {
    PhotoTextExtractorTheme(darkTheme = true) {
        Scaffold(content = {
            val context = LocalContext.current

            val fileName = "cicikustwo.jpg"
            val path = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$fileName"
            val uri = FileProvider.getUriForFile(context,
                context.applicationContext.packageName +".provider",File(path))

            val lastUri = remember {
                mutableStateOf(uri)
            }
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
                            val fileName = "cicikustwo.jpg"
                            val path = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$fileName"
                            val file = File(path);
                            val uri = FileProvider.getUriForFile(context,context.applicationContext.packageName +".provider",file)
                            launcher3.launch(uri)



                        } else {
                            // Show dialog
                            println("zws")
                        }
                    }
                    Button(
                        onClick = {
                            if (isCameraAccessGranted.value){
                                val c = Calendar.getInstance()
                                val x = c.get(Calendar.MILLISECOND)
                                val fileName = "x"+ x + "cicikusthree.jpg"
                                val path = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$fileName"
                                val file = File(path);
                                val uri = FileProvider.getUriForFile(context,context.applicationContext.packageName +".provider",file)
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
                    bitmap.let {
                        val data = it.value
                        if (data != null) {
                            Text(text = visionOutText.value)
                            Image(
                                bitmap = data.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(400.dp)
                            )
                        }
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