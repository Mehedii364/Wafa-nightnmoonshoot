package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.processing.MoonSegmentationEngine
import com.example.processing.TransparentPngExporter
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicSurfaceVariant
import com.example.ui.theme.DeepSpaceBlack
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.LunarSilver
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class PngBgMode {
    CHECKERBOARD,
    BLACK,
    WHITE,
    NIGHT_SKY
}

@Composable
fun PngStudioScreen(
    initialFilePath: String?,
    language: AppLanguage,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentMask by remember { mutableStateOf<ByteArray?>(null) }
    var initialMask by remember { mutableStateOf<ByteArray?>(null) }
    var resultTransparentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Brush controls
    var isAddBrushMode by remember { mutableStateOf(false) } // false = erase mask, true = add to mask
    var brushRadius by remember { mutableFloatStateOf(30f) }
    var bgMode by remember { mutableStateOf(PngBgMode.CHECKERBOARD) }
    var isSplitCompareActive by remember { mutableStateOf(false) }
    var splitPosition by remember { mutableFloatStateOf(0.5f) }

    // Undo / Redo history
    val undoStack = remember { mutableStateListOf<ByteArray>() }
    val redoStack = remember { mutableStateListOf<ByteArray>() }

    // Last export result
    var exportedFile by remember { mutableStateOf<File?>(null) }

    // Photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                val bmp = loadBitmapFromUri(context, uri)
                if (bmp != null) {
                    sourceBitmap = bmp
                    val seg = MoonSegmentationEngine.autoSegmentMoon(bmp)
                    currentMask = seg.mask
                    initialMask = seg.mask.copyOf()
                    undoStack.clear()
                    redoStack.clear()
                    resultTransparentBitmap = MoonSegmentationEngine.createTransparentBitmap(bmp, seg.mask)
                }
                isProcessing = false
            }
        }
    }

    // Load initial file if provided
    LaunchedEffect(initialFilePath) {
        if (!initialFilePath.isNullOrBlank()) {
            val file = File(initialFilePath)
            if (file.exists()) {
                isProcessing = true
                val bmp = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeFile(file.absolutePath)
                }
                if (bmp != null) {
                    sourceBitmap = bmp
                    val seg = MoonSegmentationEngine.autoSegmentMoon(bmp)
                    currentMask = seg.mask
                    initialMask = seg.mask.copyOf()
                    resultTransparentBitmap = MoonSegmentationEngine.createTransparentBitmap(bmp, seg.mask)
                }
                isProcessing = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("png_studio_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AppStrings.get("png_studio_title", language),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "32-bit RGBA • বাস্তব স্বচ্ছ ব্যাকগ্রাউন্ড",
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_select_image")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Pick", tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.get("png_select_image", language), color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Display Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, CosmicBorder, RoundedCornerShape(16.dp))
                    .background(GraphiteSurface),
                contentAlignment = Alignment.Center
            ) {
                if (sourceBitmap == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Moon Studio",
                            tint = NeonCyan,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "চাঁদের ছবি নির্বাচন বা ক্যাপচার করুন",
                            color = LunarSilver,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "অটো-সেগমেন্টেশন অ্যালগরিদম রাতের অন্ধকার আকাশ থেকে চাঁদের ক্রেটার ও পৃষ্ঠকে নিখুঁতভাবে আলাদা করবে।",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    val bmp = sourceBitmap!!
                    val resBmp = resultTransparentBitmap

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(isAddBrushMode, brushRadius, currentMask) {
                                detectDragGestures { change, _ ->
                                    val mask = currentMask ?: return@detectDragGestures
                                    val touchX = (change.position.x / size.width) * bmp.width
                                    val touchY = (change.position.y / size.height) * bmp.height
                                    val scaledRadius = (brushRadius / size.width) * bmp.width

                                    undoStack.add(mask.copyOf())
                                    redoStack.clear()

                                    val modifiedMask = MoonSegmentationEngine.applyBrush(
                                        mask = mask,
                                        width = bmp.width,
                                        height = bmp.height,
                                        brushCenterX = touchX,
                                        brushCenterY = touchY,
                                        brushRadius = scaledRadius,
                                        isAddMode = isAddBrushMode
                                    )
                                    currentMask = modifiedMask
                                    resultTransparentBitmap = MoonSegmentationEngine.createTransparentBitmap(bmp, modifiedMask)
                                }
                            }
                    ) {
                        // 1. Draw Background depending on bgMode
                        when (bgMode) {
                            PngBgMode.CHECKERBOARD -> {
                                val squareSize = 16.dp.toPx()
                                val cols = (size.width / squareSize).toInt() + 1
                                val rows = (size.height / squareSize).toInt() + 1
                                for (r in 0 until rows) {
                                    for (c in 0 until cols) {
                                        val color = if ((r + c) % 2 == 0) Color(0xFF232733) else Color(0xFF161922)
                                        drawRect(color, Offset(c * squareSize, r * squareSize), Size(squareSize, squareSize))
                                    }
                                }
                            }
                            PngBgMode.BLACK -> drawRect(DeepSpaceBlack)
                            PngBgMode.WHITE -> drawRect(Color.White)
                            PngBgMode.NIGHT_SKY -> drawRect(Color(0xFF030712))
                        }

                        // 2. Draw Image (Transparent or Split Compare)
                        if (isSplitCompareActive && resBmp != null) {
                            // Split view: Left = Original, Right = Transparent Result
                            val splitX = size.width * splitPosition
                            drawImage(bmp.asImageBitmap())
                            drawLine(NeonCyan, Offset(splitX, 0f), Offset(splitX, size.height), 3.dp.toPx())
                        } else if (resBmp != null) {
                            drawImage(resBmp.asImageBitmap())
                        } else {
                            drawImage(bmp.asImageBitmap())
                        }
                    }
                }

                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ObsidianBackground.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Toolbar: Background Modes, Split Compare, Undo, Redo, Reset
            if (sourceBitmap != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Background Mode Selector
                    Row(
                        modifier = Modifier
                            .background(CosmicSurfaceVariant, RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            PngBgMode.CHECKERBOARD to AppStrings.get("png_bg_checker", language),
                            PngBgMode.BLACK to AppStrings.get("png_bg_black", language),
                            PngBgMode.WHITE to AppStrings.get("png_bg_white", language)
                        ).forEach { (mode, label) ->
                            val isSelected = bgMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) NeonCyan else Color.Transparent)
                                    .clickable { bgMode = mode }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) DeepSpaceBlack else LunarSilver,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Undo / Redo / Reset
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = {
                                if (undoStack.isNotEmpty()) {
                                    val previous = undoStack.removeAt(undoStack.lastIndex)
                                    redoStack.add(currentMask!!.copyOf())
                                    currentMask = previous
                                    resultTransparentBitmap = MoonSegmentationEngine.createTransparentBitmap(sourceBitmap!!, previous)
                                }
                            },
                            enabled = undoStack.isNotEmpty(),
                            modifier = Modifier.testTag("btn_undo")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if (undoStack.isNotEmpty()) NeonCyan else Color.Gray)
                        }

                        IconButton(
                            onClick = {
                                if (redoStack.isNotEmpty()) {
                                    val next = redoStack.removeAt(redoStack.lastIndex)
                                    undoStack.add(currentMask!!.copyOf())
                                    currentMask = next
                                    resultTransparentBitmap = MoonSegmentationEngine.createTransparentBitmap(sourceBitmap!!, next)
                                }
                            },
                            enabled = redoStack.isNotEmpty(),
                            modifier = Modifier.testTag("btn_redo")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = if (redoStack.isNotEmpty()) NeonCyan else Color.Gray)
                        }

                        IconButton(
                            onClick = {
                                if (initialMask != null) {
                                    undoStack.add(currentMask!!.copyOf())
                                    currentMask = initialMask!!.copyOf()
                                    resultTransparentBitmap = MoonSegmentationEngine.createTransparentBitmap(sourceBitmap!!, initialMask!!)
                                }
                            },
                            modifier = Modifier.testTag("btn_reset")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = LunarSilver)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Brush Tools Row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CosmicSurfaceVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Erase Brush
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isAddBrushMode) ElectricViolet else GraphiteSurface)
                                    .clickable { isAddBrushMode = false }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    AppStrings.get("png_brush_remove", language),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Add Brush
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isAddBrushMode) NeonCyan else GraphiteSurface)
                                    .clickable { isAddBrushMode = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    AppStrings.get("png_brush_add", language),
                                    color = if (isAddBrushMode) DeepSpaceBlack else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "${AppStrings.get("png_brush_size", language)}: ${brushRadius.toInt()}px",
                            color = LunarSilver,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = brushRadius,
                        onValueChange = { brushRadius = it },
                        valueRange = 10f..120f,
                        colors = SliderDefaults.colors(
                            thumbColor = if (isAddBrushMode) NeonCyan else ElectricViolet,
                            activeTrackColor = if (isAddBrushMode) NeonCyan else ElectricViolet,
                            inactiveTrackColor = CosmicBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Export Button
                Button(
                    onClick = {
                        val res = resultTransparentBitmap ?: return@Button
                        scope.launch {
                            isProcessing = true
                            val export = TransparentPngExporter.exportPng(context, res)
                            isProcessing = false
                            if (export.success) {
                                exportedFile = export.localFile
                                snackbarHostState.showSnackbar(AppStrings.get("png_export_success", language))
                            } else {
                                snackbarHostState.showSnackbar(export.message)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_export_png")
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Export", tint = DeepSpaceBlack)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("png_export_btn", language),
                        color = DeepSpaceBlack,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // If exported, show share action
                if (exportedFile != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CosmicSurfaceVariant, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "সংরক্ষিত: ${exportedFile!!.name}",
                            color = EmeraldSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Button(
                            onClick = { TransparentPngExporter.shareImage(context, exportedFile!!) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppStrings.get("share", language), color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (e: Exception) {
        null
    }
}
