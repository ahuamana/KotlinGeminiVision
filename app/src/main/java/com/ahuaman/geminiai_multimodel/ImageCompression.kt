package com.ahuaman.geminiai_multimodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class ImageCompression(private val context: Context) {

    fun compressImage(contentURI: String): String {
        val contentUri = Uri.parse(contentURI)
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        var bmp: Bitmap? = null
        var scaledBitmap: Bitmap? = null

        try {
            val inputStream = context.contentResolver.openInputStream(contentUri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        var actualHeight = options.outHeight
        var actualWidth = options.outWidth

        val maxHeight = 816.0f
        val maxWidth = 612.0f
        var imgRatio = (actualWidth / actualHeight)
        val maxRatio = maxWidth / maxHeight

        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            when {
                imgRatio < maxRatio -> {
                    imgRatio = (maxHeight / actualHeight).toInt()
                    actualWidth = (imgRatio * actualWidth).toInt()
                    actualHeight = maxHeight.toInt()
                }
                imgRatio > maxRatio -> {
                    imgRatio = (maxWidth / actualWidth).toInt()
                    actualHeight = (imgRatio * actualHeight).toInt()
                    actualWidth = maxWidth.toInt()
                }
                else -> {
                    actualHeight = maxHeight.toInt()
                    actualWidth = maxWidth.toInt()
                }
            }
        }

        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight)
        options.inJustDecodeBounds = false
        options.inDither = false
        options.inPurgeable = true
        options.inInputShareable = true
        options.inTempStorage = ByteArray(16 * 1024)

        try {
            bmp = BitmapFactory.decodeFile(contentURI, options)
        } catch (exception: OutOfMemoryError) {
            exception.printStackTrace()
        }

        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888)
        } catch (exception: OutOfMemoryError) {
            exception.printStackTrace()
        }

        val ratioX = actualWidth / options.outWidth.toFloat()
        val ratioY = actualHeight / options.outHeight.toFloat()
        val middleX = actualWidth / 2.0f
        val middleY = actualHeight / 2.0f

        val scaleMatrix = Matrix()
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)

        val canvas = Canvas(scaledBitmap!!)
        canvas.concat(scaleMatrix)
        canvas.drawBitmap(bmp!!, middleX - bmp.width / 2, middleY - bmp.height / 2, Paint(Paint.FILTER_BITMAP_FLAG))

        val exif: ExifInterface
        try {
            exif = ExifInterface(contentURI)

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, 0)
            val matrix = Matrix()
            when (orientation) {
                6 -> {
                    matrix.postRotate(90f)
                }
                3 -> {
                    matrix.postRotate(180f)
                }
                8 -> {
                    matrix.postRotate(270f)
                }
            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                scaledBitmap.width, scaledBitmap.height, matrix,
                true)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val out: FileOutputStream?
        val filename = getFilename()
        try {
            out = FileOutputStream(filename)
            scaledBitmap!!.compress(Bitmap.CompressFormat.JPEG, 80, out)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        return filename
    }

    private fun getFilename(): String {
        val file = File(context.getExternalFilesDir(null)!!.absolutePath + "/Images")
        if (!file.exists()) {
            file.mkdirs()
        }
        return file.absolutePath + "/" + System.currentTimeMillis() + ".jpg"
    }

    private fun getRealPathFromURI(contentURI: String): String {
        val contentUri = Uri.parse(contentURI)
        val cursor = context.contentResolver.query(contentUri, null, null, null, null)
        if (cursor == null) {
            return contentUri.path!!
        } else {
            val columnIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            if (columnIndex == -1) {
                // If the column doesn't exist, return the path from the Uri directly
                return contentUri.path!!
            }
            cursor.moveToFirst()
            return cursor.getString(columnIndex)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
            val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }
        val totalPixels = (width * height).toFloat()
        val totalReqPixelsCap = (reqWidth * reqHeight * 2).toFloat()
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++
        }

        return inSampleSize
    }


}

fun calculateCompression(originalImagePath: String, compressedImagePath: String): Float {
    val originalFile = File(originalImagePath)
    val compressedFile = File(compressedImagePath)

    val originalSize = originalFile.length().toFloat() / 1024 // size in kilobytes
    val compressedSize = compressedFile.length().toFloat() / 1024 // size in kilobytes

    val reduction = originalSize - compressedSize
    val reductionPercentage = (reduction / originalSize) * 100

    return reductionPercentage
}