package com.yusa.phototextextractor

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
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

            MainUi(
                mainViewModel)
        }

    }
}
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun MainUi(
    mainViewModel: MainViewModel){
    PhotoTextExtractorTheme(darkTheme = true){
        Scaffold() {
            val context = LocalContext.current
            val focusRequester = remember { FocusRequester() }
            val focusManager = LocalFocusManager.current
            val uriOfZoomed = remember{ mutableStateOf<Uri>(Uri.EMPTY)}
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
            val launcherSelectFromGallery = rememberLauncherForActivityResult(
                contract = GetPicturesContract()){
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                try {
                    context.contentResolver.takePersistableUriPermission(it!!,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val image: InputImage
                    image = InputImage.fromFilePath(context, it)
                    val result = recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            //searchedList.clear()    NEED TO FIX CLEAR Method
                            val searchResult = SnapshotStateList<ExtractedImage>()
                            val extractedImages : List<ExtractedImage>
                                    = mainViewModel.getAllExtracted().value!!
                            val parts = visionText.text.split("\n")
                            for (part in parts){
                                val words = part.split(" ")
                                for (word in words){
                                    for(image in extractedImages){
                                        if (image.text.toString().contains("-*-" + word+"-*-")){
                                            if(!searchResult.contains(image)){
                                                searchResult.add(image)
                                            }

                                        }
                                    }
                                }
                            }
                            mainViewModel.searchedList.swapList(searchResult)

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
                    mainViewModel.isGallery.value = false

                } else {
                    // Show dialog
                    println("zws") } }
            val permission = Manifest.permission.CAMERA

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(10.dp, 5.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Color.DarkGray), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
                    Column(modifier = Modifier.fillMaxWidth(0.4f),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Search text")
                        BasicTextField(value = mainViewModel.searchText.value, onValueChange = {
                            mainViewModel.searchText.value = it

                        }, decorationBox = { innerTextField ->
                            Row(
                                Modifier
                                    .background(Color.LightGray, RoundedCornerShape(percent = 20))
                                    .padding(4.dp)
                                    .fillMaxWidth()
                                    .height(20.dp)
                                    .focusRequester(focusRequester)
                            ) {
                                //...
                                innerTextField()
                            }
                        })
                    }

                    Button(modifier = Modifier
                        .height(50.dp)
                        .width(100.dp), onClick = {
                        mainViewModel.searchFromText()
                        focusManager.clearFocus()
                    }) {
                        Text(text = "Search with text", fontSize = 9.sp)
                    }
                    Button(modifier = Modifier
                        .height(50.dp)
                        .width(100.dp),onClick = {
                        mainViewModel.searchFromImage(launcherSelectFromGallery)
                        focusManager.clearFocus()
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
                    Spacer(modifier = Modifier.fillMaxWidth(0.02f))
                    Column(modifier = Modifier
                        .fillMaxHeight()
                        .width(180.dp)
                        .background(Color.Gray),horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement =Arrangement.SpaceAround) {
                        Text(text = "Image Processing Options",modifier = Modifier
                                .height(20.dp), fontSize = 12.sp)
                        Button(modifier = Modifier
                            .height(50.dp)
                            .width(120.dp),onClick = {
                            mainViewModel.getImageFromCamera(context,launcherWithUri,permission,launcher)
                            }) {
                            Text(text = "From camera", fontSize = 9.sp)
                        }
                        Button(modifier = Modifier
                            .height(50.dp)
                            .width(120.dp),onClick = { mainViewModel.getImageFromGallery(launcherOfGallery) }) {
                            Text(text = "From device", fontSize = 9.sp)
                        }
                    }
                    Spacer(modifier = Modifier.fillMaxWidth(0.05f))
                    Column(modifier = Modifier
                        .fillMaxHeight()
                        .width(180.dp)
                        .background(Color.Gray), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement =Arrangement.SpaceAround) {
                        Text(text = "Database Options",modifier = Modifier
                            .height(20.dp),fontSize = 12.sp)
                        Button(modifier = Modifier
                            .height(50.dp)
                            .width(120.dp),onClick = {
                            mainViewModel.loadAllDatabase()
                            val searchTextVal = mainViewModel.searchText.value
                            mainViewModel.searchText.value = searchTextVal
                            }) {
                            Text(text = "Load all db", fontSize = 9.sp)
                        }
                        Button(modifier = Modifier
                            .height(50.dp)
                            .width(120.dp),onClick = { mainViewModel.killAllDatabase() }) {
                            Text(text = "Kill all db", fontSize = 9.sp)
                        }
                    }
                    Spacer(modifier = Modifier.fillMaxWidth(0.02f))

                }
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.03f))
                Box(modifier = Modifier.fillMaxWidth()){
                    LazyColumn(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally){
                        itemsIndexed(mainViewModel.searchedList){
                                index, item ->
                            Box(modifier = Modifier
                                .size(300.dp)
                                .background(Color.LightGray), contentAlignment = Alignment.TopStart){
                                AsyncImage(model = item.image, contentDescription = null, modifier = Modifier
                                    .padding(8.dp)
                                    .size(270.dp, 270.dp)
                                    .clickable { uriOfZoomed.value = item.image!!.toUri() })
                                Text(text = "Date: " + item.date.toString(),Modifier.padding(10.dp,280.dp,0.dp,0.dp), fontSize = 9.sp, color = Color.Black)
                                Text(text = "ID: " + item.id.toString(),Modifier.padding(120.dp,280.dp,0.dp,0.dp), fontSize = 9.sp, color = Color.Black)

                            }
                            Spacer(modifier = Modifier.size(10.dp))
                        }
                    }
                    if (uriOfZoomed.value != Uri.EMPTY){
                        AsyncImage(model = uriOfZoomed.value, contentDescription = null, modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                uriOfZoomed.value = Uri.EMPTY
                            })
                    }

                }
            }
        }
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
fun <T> SnapshotStateList<T>.swapList(newList: List<T>){
    clear()
    addAll(newList)
}
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    // CaptureImageFromCamera()
}