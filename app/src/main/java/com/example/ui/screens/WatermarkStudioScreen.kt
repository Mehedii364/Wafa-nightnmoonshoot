package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.os.Environment
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.processing.WatermarkComposer
import com.example.processing.WatermarkOptions
import com.example.processing.WatermarkPosition
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
import java.io.FileOutputStream

data class SignaturePath(
    val points: List<Offset>,
    val strokeWidth: Float = 4f
)

@Composable
fun WatermarkStudioScreen(
    initialFilePath: String?,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var watermarkText by remember { mutableStateOf("Wafa NightShot Ultra") }
    var includeTimestamp by remember { mutableStateOf(true) }
    var selectedPosition by remember { mutableStateOf(WatermarkPosition.BOTTOM_RIGHT) }
    var opacity by remember { mutableFloatStateOf(0.85f) }
    var scale by remember { mutableFloatStateOf(1.0f) }
    var rotation by remember { mutableFloatStateOf(0f) }

    // Signature state
    var isDrawingSignatureMode by remember { mutableStateOf(false) }
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val paths = remember { mutableStateListOf<SignaturePath>() }
    var currentPathPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var signatureStrokeWidth by remember { mutableFloatStateOf(5f) }

    // Imported Logo state
    var logoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Photo picker for main photo
    val mainPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                sourceBitmap = loadBitmap(context, uri)
            }
        }
    }

    // Photo picker for logo
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                logoBitmap = loadBitmap(context, uri)
            }
        }
    }

    LaunchedEffect(initialFilePath) {
        if (!initialFilePath.isNullOrBlank()) {
            val file = File(initialFilePath)
            if (file.exists()) {
                sourceBitmap = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeFile(file.absolutePath)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("watermark_studio_screen")
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
                        text = AppStrings.get("watermark_title", language),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "স্বাক্ষর, পিএনজি লোগো ও কপিরাইট ওয়াটারমার্ক",
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = {
                        mainPhotoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Pick", tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.get("png_select_image", language), color = Color.White, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Signature Drawing Pad Toggle & Pad
            if (isDrawingSignatureMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CosmicSurfaceVariant, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.get("draw_signature", language),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            IconButton(onClick = {
                                if (paths.isNotEmpty()) paths.removeAt(paths.lastIndex)
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = NeonCyan)
                            }
                            IconButton(onClick = {
                                paths.clear()
                                currentPathPoints = emptyList()
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    }

                    // Drawing Surface
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DeepSpaceBlack)
                            .border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentPathPoints = listOf(offset)
                                        },
                                        onDrag = { change, _ ->
                                            currentPathPoints = currentPathPoints + change.position
                                        },
                                        onDragEnd = {
                                            if (currentPathPoints.size > 1) {
                                                paths.add(SignaturePath(currentPathPoints, signatureStrokeWidth))
                                            }
                                            currentPathPoints = emptyList()
                                        }
                                    )
                                }
                        ) {
                            for (p in paths) {
                                for (i in 0 until p.points.size - 1) {
                                    drawLine(
                                        color = NeonCyan,
                                        start = p.points[i],
                                        end = p.points[i + 1],
                                        strokeWidth = p.strokeWidth
                                    )
                                }
                            }
                            for (i in 0 until currentPathPoints.size - 1) {
                                drawLine(
                                    color = NeonCyan,
                                    start = currentPathPoints[i],
                                    end = currentPathPoints[i + 1],
                                    strokeWidth = signatureStrokeWidth
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                // Render signature to transparent bitmap
                                val bmp = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bmp)
                                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = android.graphics.Color.WHITE
                                    strokeWidth = 6f
                                    style = Paint.Style.STROKE
                                    strokeCap = Paint.Cap.ROUND
                                    strokeJoin = Paint.Join.ROUND
                                }
                                for (p in paths) {
                                    for (i in 0 until p.points.size - 1) {
                                        canvas.drawLine(p.points[i].x, p.points[i].y, p.points[i + 1].x, p.points[i + 1].y, paint)
                                    }
                                }
                                signatureBitmap = bmp
                                isDrawingSignatureMode = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save", tint = DeepSpaceBlack, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppStrings.get("save_signature", language), color = DeepSpaceBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { isDrawingSignatureMode = false },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("বাতিল", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Image Preview Surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GraphiteSurface)
                    .border(1.5.dp, CosmicBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (sourceBitmap == null) {
                    Text(
                        text = "ওয়াটারমার্ক যোগ করার জন্য একটি ছবি নির্বাচন করুন",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                } else {
                    ImageWatermarkPreview(
                        bitmap = sourceBitmap!!,
                        text = watermarkText,
                        includeTimestamp = includeTimestamp,
                        position = selectedPosition,
                        opacity = opacity,
                        signatureBitmap = signatureBitmap,
                        logoBitmap = logoBitmap
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Draw Signature, Import Logo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { isDrawingSignatureMode = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Draw, contentDescription = "Signature", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.get("draw_signature", language), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        logoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Logo", tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.get("import_logo", language), color = Color.White, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Text Watermark Input Field & Timestamp Switch
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmicSurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                OutlinedTextField(
                    value = watermarkText,
                    onValueChange = { watermarkText = it },
                    label = { Text(AppStrings.get("text_watermark", language)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = LunarSilver,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CosmicBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "তারিখ ও সময় অন্তর্ভুক্তি (Date & Time)",
                        color = LunarSilver,
                        fontSize = 12.sp
                    )
                    Switch(
                        checked = includeTimestamp,
                        onCheckedChange = { includeTimestamp = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = CosmicBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Position Preset Selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmicSurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = AppStrings.get("watermark_position", language),
                    color = LunarSilver,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        WatermarkPosition.BOTTOM_RIGHT to AppStrings.get("pos_bottom_right", language),
                        WatermarkPosition.BOTTOM_LEFT to AppStrings.get("pos_bottom_left", language),
                        WatermarkPosition.TOP_RIGHT to AppStrings.get("pos_top_right", language),
                        WatermarkPosition.CENTER to AppStrings.get("pos_center", language)
                    ).forEach { (pos, label) ->
                        val isSelected = selectedPosition == pos
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NeonCyan else GraphiteSurface)
                                .clickable { selectedPosition = pos }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sliders: Opacity
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmicSurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(AppStrings.get("watermark_opacity", language), color = LunarSilver, fontSize = 11.sp)
                    Text("${(opacity * 100).toInt()}%", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Slider(
                    value = opacity,
                    onValueChange = { opacity = it },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = CosmicBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Save / Apply Button
            Button(
                onClick = {
                    val bmp = sourceBitmap ?: return@Button
                    scope.launch {
                        val picturesDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "MoonStudio")
                        if (!picturesDir.exists()) picturesDir.mkdirs()
                        val outputFile = File(picturesDir, "Watermark_${System.currentTimeMillis()}.jpg")

                        val options = WatermarkOptions(
                            text = watermarkText,
                            includeTimestamp = includeTimestamp,
                            position = selectedPosition,
                            opacity = opacity,
                            scaleFactor = scale,
                            rotationDegrees = rotation,
                            signatureBitmap = signatureBitmap,
                            logoBitmap = logoBitmap
                        )

                        val success = WatermarkComposer.applyWatermark(bmp, options, outputFile)
                        if (success) {
                            snackbarHostState.showSnackbar("ছবিতে সফলভাবে ওয়াটারমার্ক যুক্ত করা হয়েছে!")
                        } else {
                            snackbarHostState.showSnackbar("ওয়াটারমার্ক যুক্ত করতে ব্যর্থ হয়েছে")
                        }
                    }
                },
                enabled = sourceBitmap != null,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_watermark")
            ) {
                Text(
                    text = AppStrings.get("apply_watermark", language),
                    color = DeepSpaceBlack,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ImageWatermarkPreview(
    bitmap: Bitmap,
    text: String,
    includeTimestamp: Boolean,
    position: WatermarkPosition,
    opacity: Float,
    signatureBitmap: Bitmap?,
    logoBitmap: Bitmap?
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawImage(bitmap.asImageBitmap())

        // Simulated preview overlay
        val overlayColor = Color.White.copy(alpha = opacity)
        // Draw position indicator
        val margin = 20.dp.toPx()
        val cx = when (position) {
            WatermarkPosition.BOTTOM_RIGHT -> size.width - margin - 40.dp.toPx()
            WatermarkPosition.BOTTOM_LEFT -> margin + 40.dp.toPx()
            WatermarkPosition.TOP_RIGHT -> size.width - margin - 40.dp.toPx()
            WatermarkPosition.TOP_LEFT -> margin + 40.dp.toPx()
            WatermarkPosition.CENTER -> size.width / 2f
        }
        val cy = when (position) {
            WatermarkPosition.BOTTOM_RIGHT -> size.height - margin
            WatermarkPosition.BOTTOM_LEFT -> size.height - margin
            WatermarkPosition.TOP_RIGHT -> margin + 20.dp.toPx()
            WatermarkPosition.TOP_LEFT -> margin + 20.dp.toPx()
            WatermarkPosition.CENTER -> size.height / 2f
        }

        if (signatureBitmap != null) {
            drawImage(signatureBitmap.asImageBitmap(), Offset(cx - 30.dp.toPx(), cy - 20.dp.toPx()))
        }
        if (logoBitmap != null) {
            drawImage(logoBitmap.asImageBitmap(), Offset(cx, cy))
        }
    }
}

private suspend fun loadBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (e: Exception) {
        null
    }
}
