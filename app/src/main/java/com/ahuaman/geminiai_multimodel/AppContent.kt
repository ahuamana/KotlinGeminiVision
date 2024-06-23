package com.ahuaman.geminiai_multimodel

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.Objects

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun AppContent(modifier: Modifier = Modifier) {

    val context = LocalContext.current
    val file = context.createImageFile()
    val uri = FileProvider.getUriForFile(Objects.requireNonNull(context), BuildConfig.APPLICATION_ID + ".provider", file)
    
    var capturedImageUri by remember {
        mutableStateOf<Uri>(Uri.EMPTY)
    }

    var newImageUri by remember {
        mutableStateOf<Uri>(Uri.EMPTY)
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            capturedImageUri = uri
        } else {
            capturedImageUri = Uri.EMPTY
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            cameraLauncher.launch(uri)
        }else {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Button(onClick = {
           openCamera(context, onResultSuccessCallback = {
               cameraLauncher.launch(uri)
           } , onFailPermissionCallback = {
               permissionLauncher.launch(android.Manifest.permission.CAMERA)
           })

        }) {
            Text(text = "Capture Image From Camera")
        }

        if(capturedImageUri != Uri.EMPTY){
            Image(
                modifier = Modifier
                    .padding(16.dp, 8.dp).size(200.dp),
                painter = rememberAsyncImagePainter(capturedImageUri),
                contentDescription = null
            )

            Button(onClick = {
                val base64 = capturedImageUri.toBase64(context)
                Toast.makeText(context, base64, Toast.LENGTH_SHORT).show()
                println("image path: $capturedImageUri")


                val absolutePath = context.resizeAndCompressImage(capturedImageUri)
                absolutePath?.let {
                    val uncompressedImage = context.getFileSizeFromUri(capturedImageUri)
                    val compressedImage = context.getFileSizeFromUri(absolutePath)

                    println("Uncompressed Image Size: $uncompressedImage")
                    println("Compressed Image Size: $compressedImage")

                    newImageUri = it
                }

            }) {
                Text(text = "Compress Image and Show Base64 String")
            }

            if(newImageUri != Uri.EMPTY){
                Image(
                    modifier = Modifier
                        .padding(16.dp, 8.dp).size(200.dp),
                    painter = rememberAsyncImagePainter(newImageUri),
                    contentDescription = null
                )

                Button(onClick = { val generativeModel = GenerativeModel(
                    // The Gemini 1.5 models are versatile and work with both text-only and multimodal prompts
                    modelName = "gemini-1.5-pro",
                    // Access your API key as a Build Configuration variable (see "Set up your API key" above)
                    apiKey = BuildConfig.API_KEY
                )

                    val inputContent = content {
                        newImageUri.toBitmap(context)?.let {
                            image(it)
                        }
                        text("his is organic or inoganic container, dont use the labels written on the item? The result give me on a JSON format. where include classification, type, percentage in the JSON -- \n" +
                                "\n" +
                                "{\n" +
                                "\"classification\": \"\",\n" +
                                "\"type\": \"\",\n" +
                                "\"percentage\": \"\"\n" +
                                "} \n -- IMPRINT IN ONE LINE\n" )
                    }

                    GlobalScope.launch {
                        println("GEMINI AI - Generating content from image...")
                        val response = generativeModel.generateContent(inputContent)
                        val responseText = response.text?.replace("\n", "")?.replace("\r", "")
                        println("GEMINI AI - $responseText")
                    }

                }) {
                    Text(text = "Generate format from image using AI")

                }
            }

        }
    }
    


}

fun openCamera(context: Context, onResultSuccessCallback: () -> Unit , onFailPermissionCallback: ()-> Unit){
    val permissionCheckResult = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
    if (permissionCheckResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
        onResultSuccessCallback()
    } else {
        onFailPermissionCallback()
    }
}

@Preview
@Composable
private fun AppContentPreview() {
    AppContent()
}