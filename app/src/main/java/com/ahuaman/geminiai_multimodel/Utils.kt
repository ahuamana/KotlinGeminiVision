package com.ahuaman.geminiai_multimodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.net.toUri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

fun Context.createImageFile(): File {
    // Create an image file name
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"
    val image = File.createTempFile(
        imageFileName, /* prefix */
        ".jpg", /* suffix */
        externalCacheDir      /* directory */
    )
    return image
}

//Convert the file to a base64 string

fun Uri.toBase64(context: Context): String? {
    val contentResolver = context.contentResolver
    val inputStream = contentResolver.openInputStream(this)
    val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
    val byteArray = byteArrayOutputStream.toByteArray()
    val base64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
    return base64
}

fun Context.resizeAndCompressImage(imageUri: Uri): Uri? {
    try {
        val originalBitmap = BitmapFactory.decodeStream(this.contentResolver.openInputStream(imageUri))
        originalBitmap?.let {
            // Create a new bitmap with the desired dimensions
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 512, 512, true)

            // Compress the bitmap into a JPEG format with a quality of 50%
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
            val compressedBitmapData = outputStream.toByteArray()

            // Ensure the directory exists
            val compressedImagesDir = File(this.getExternalFilesDir(null), "CompressedImages")
            if (!compressedImagesDir.exists()) {
                compressedImagesDir.mkdirs()
            }

            // Write the compressed bitmap data to a new file
            val compressedFile = File(compressedImagesDir, "compressed_image.jpg")
            compressedFile.createNewFile()
            val fileOutputStream = FileOutputStream(compressedFile)
            fileOutputStream.write(compressedBitmapData)
            fileOutputStream.close()

            return compressedFile.toUri()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

fun Context.getFileSizeFromUri(uri: Uri): Long {
    val fileDescriptor = contentResolver.openFileDescriptor(uri, "r") ?: return 0
    val fileSize = fileDescriptor.statSize
    fileDescriptor.close()
    return fileSize
}