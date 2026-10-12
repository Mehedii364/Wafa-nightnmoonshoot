package com.example.processing

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object TransparentPngExporter {

    data class ExportResult(
        val success: Boolean,
        val localFile: File?,
        val publicUri: Uri?,
        val message: String,
        val hasVerifiedAlpha: Boolean
    )

    suspend fun exportPng(
        context: Context,
        bitmap: Bitmap,
        filenamePrefix: String = "Wafa_Moon_Transparent"
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            // Verify alpha channel
            val hasAlpha = bitmap.hasAlpha()

            val timestamp = System.currentTimeMillis()
            val fileName = "${filenamePrefix}_$timestamp.png"

            // 1. Save to app-specific persistent pictures directory
            val picturesDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "MoonStudio")
            if (!picturesDir.exists()) picturesDir.mkdirs()
            val localFile = File(picturesDir, fileName)

            FileOutputStream(localFile).use { fos ->
                val compressed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                if (!compressed) throw IllegalStateException("PNG compression failed")
            }

            // 2. Also register with MediaStore for system gallery access (if permitted)
            var publicUri: Uri? = null
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.DATE_ADDED, timestamp / 1000)
                    put(MediaStore.Images.Media.DATE_TAKEN, timestamp)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/WafaNightShotUltra")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                publicUri = context.contentResolver.insert(collection, contentValues)
                if (publicUri != null) {
                    context.contentResolver.openOutputStream(publicUri)?.use { os: OutputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        context.contentResolver.update(publicUri, contentValues, null, null)
                    }
                }
            } catch (ignored: Exception) {
                // If MediaStore fails or denied, localFile is guaranteed safe
            }

            ExportResult(
                success = true,
                localFile = localFile,
                publicUri = publicUri,
                message = "স্বচ্ছ পিএনজি সফলভাবে সংরক্ষিত হয়েছে: ${localFile.name}",
                hasVerifiedAlpha = hasAlpha
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                localFile = null,
                publicUri = null,
                message = "পিএনজি সংরক্ষণে ব্যর্থ: ${e.localizedMessage}",
                hasVerifiedAlpha = false
            )
        }
    }

    fun shareImage(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "পিএনজি শেয়ার করুন"))
        } catch (e: Exception) {
            // Fallback direct share
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
            }
            context.startActivity(Intent.createChooser(intent, "পিএনজি শেয়ার করুন"))
        }
    }
}
