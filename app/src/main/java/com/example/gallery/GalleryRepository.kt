package com.example.gallery

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import com.example.core.model.CapturedImageItem
import com.example.core.model.ImageCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class GalleryRepository(private val context: Context) {

    private val _images = MutableStateFlow<List<CapturedImageItem>>(emptyList())
    val images: StateFlow<List<CapturedImageItem>> = _images.asStateFlow()

    suspend fun refreshGallery() = withContext(Dispatchers.IO) {
        val list = mutableListOf<CapturedImageItem>()

        val baseDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "MoonStudio")
        if (baseDir.exists() && baseDir.isDirectory) {
            val files = baseDir.listFiles() ?: emptyArray()
            for (file in files.sortedByDescending { it.lastModified() }) {
                if (file.isFile && (file.name.endsWith(".jpg", true) || file.name.endsWith(".png", true))) {
                    val isPng = file.name.endsWith(".png", true)
                    val category = when {
                        file.name.contains("Transparent", true) -> ImageCategory.PNG_TRANSPARENT
                        file.name.contains("Stacked", true) -> ImageCategory.STACKED
                        file.name.contains("Watermark", true) -> ImageCategory.WATERMARKED
                        file.name.contains("Night", true) -> ImageCategory.NIGHT_STUDIO
                        file.name.contains("Moon", true) -> ImageCategory.MOON
                        else -> ImageCategory.ORIGINAL
                    }

                    var w = 0
                    var h = 0
                    try {
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(file.absolutePath, options)
                        w = options.outWidth
                        h = options.outHeight
                    } catch (ignored: Exception) {}

                    list.add(
                        CapturedImageItem(
                            id = file.name,
                            uri = Uri.fromFile(file),
                            filePath = file.absolutePath,
                            category = category,
                            title = file.nameWithoutExtension,
                            timestamp = file.lastModified(),
                            width = w,
                            height = h,
                            hasAlphaChannel = isPng
                        )
                    )
                }
            }
        }

        _images.value = list
    }

    suspend fun deleteImage(item: CapturedImageItem): Boolean = withContext(Dispatchers.IO) {
        val file = File(item.filePath)
        val deleted = if (file.exists()) file.delete() else false
        if (deleted) {
            refreshGallery()
        }
        deleted
    }
}
