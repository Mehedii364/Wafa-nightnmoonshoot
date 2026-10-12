package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.camera.MoonDetector
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.model.CaptureMode
import com.example.core.model.DetectionStatus
import com.example.core.model.MoonDetectionResult
import com.example.core.model.NightSubMode
import com.example.core.model.StackingProgress
import com.example.core.model.StackingStage
import com.example.core.preferences.UserPreferences
import com.example.gallery.GalleryRepository
import com.example.processing.MoonStackingEngine
import com.example.ui.components.CameraControlsHud
import com.example.ui.components.CompositionGridOverlay
import com.example.ui.components.MoonReticleOverlay
import com.example.ui.theme.AmberHighlight
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    userPreferences: UserPreferences,
    galleryRepository: GalleryRepository,
    language: AppLanguage,
    onOpenPngStudio: (String) -> Unit,
    onOpenGallery: () -> Unit,
    onOpenMoonInfo: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Camera Permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Camera Mode & Night Sub-modes
    var activeCaptureMode by remember { mutableStateOf(CaptureMode.MOON_PRO) }
    var activeNightSubMode by remember { mutableStateOf(NightSubMode.LIGHT_TRAILS) }

    // Camera State
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }

    var currentZoomRatio by remember { mutableFloatStateOf(1.0f) }
    val maxOpticalZoom = 5.0f // Threshold before digital crop
    var maxDeviceZoom by remember { mutableFloatStateOf(40.0f) }

    var exposureCompensation by remember { mutableFloatStateOf(-1.5f) } // default -1.5 EV for Moon
    var isTorchEnabled by remember { mutableStateOf(false) }
    var isGridEnabled by remember { mutableStateOf(true) }
    var timerSeconds by remember { mutableIntStateOf(0) }
    var isTimerCountingDown by remember { mutableStateOf(false) }
    var countdownRemaining by remember { mutableIntStateOf(0) }

    // Moon Detection state
    val moonDetector = remember { MoonDetector() }
    var detectionResult by remember { mutableStateOf(MoonDetectionResult()) }

    // Multi-frame Stacking State
    var stackingFrameCount by remember { mutableIntStateOf(10) }
    var stackingProgress by remember { mutableStateOf<StackingProgress?>(null) }
    var isCapturingBurst by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("camera_screen")
    ) {
        if (!hasCameraPermission) {
            // Permission Request Card
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera Permission",
                    tint = NeonCyan,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ক্যামেরা পারমিশন প্রয়োজন",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "চাঁদ ও রাতের আকাশের ছবি ক্যাপচার এবং লাইভ ট্র্যাকিংয়ের জন্য ক্যামেরার অনুমতি দিন।",
                    color = LunarSilver,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_grant_camera_permission")
                ) {
                    Text("অনুমতি দিন (Grant Permission)", color = DeepSpaceBlack, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Live Camera Preview
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            val newZoom = (currentZoomRatio * zoom).coerceIn(1.0f, maxDeviceZoom)
                            currentZoomRatio = newZoom
                            cameraControl?.setZoomRatio(newZoom)
                        }
                    }
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                .build()
                            imageCapture = capture

                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                                        val result = moonDetector.analyzeImageProxy(imageProxy)
                                        detectionResult = result
                                    }
                                }

                            val selector = CameraSelector.Builder()
                                .requireLensFacing(lensFacing)
                                .build()

                            try {
                                cameraProvider.unbindAll()
                                val cam = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    selector,
                                    preview,
                                    capture,
                                    analysis
                                )
                                camera = cam
                                cameraControl = cam.cameraControl

                                // Set initial zoom & EV
                                cam.cameraControl.setZoomRatio(currentZoomRatio)
                                cam.cameraControl.setExposureCompensationIndex((exposureCompensation * 3).toInt())

                                cam.cameraInfo.zoomState.observe(lifecycleOwner) { state ->
                                    state?.let {
                                        maxDeviceZoom = it.maxZoomRatio.coerceAtLeast(10f)
                                    }
                                }
                            } catch (e: Exception) {
                                // Camera binding fallback
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Composition Grid Overlay
                if (isGridEnabled) {
                    CompositionGridOverlay()
                }

                // Moon Reticle Overlay (active in MOON_PRO mode)
                if (activeCaptureMode == CaptureMode.MOON_PRO) {
                    MoonReticleOverlay(
                        detectionResult = detectionResult,
                        language = language,
                        onManualTapToAim = { normX, normY ->
                            moonDetector.setManualCoordinates(normX, normY)
                        }
                    )
                }

                // Timer Countdown Display
                if (isTimerCountingDown) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DeepSpaceBlack.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$countdownRemaining",
                            color = NeonCyan,
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Top Floating Bar: Mode Switcher & Quick Navigation
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick shortcuts: Gallery, Moon Info, Settings
                        IconButton(
                            onClick = onOpenGallery,
                            modifier = Modifier
                                .size(40.dp)
                                .background(GraphiteSurface.copy(alpha = 0.7f), CircleShape)
                                .testTag("btn_open_gallery")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = LunarSilver)
                        }

                        // Mode Selector Pills
                        Row(
                            modifier = Modifier
                                .background(GraphiteSurface.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                                .border(1.dp, CosmicBorder, RoundedCornerShape(20.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                CaptureMode.PHOTO to AppStrings.get("tab_camera", language),
                                CaptureMode.MOON_PRO to AppStrings.get("tab_moon", language),
                                CaptureMode.NIGHT_STUDIO to AppStrings.get("tab_night", language)
                            ).forEach { (mode, label) ->
                                val isSelected = activeCaptureMode == mode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) NeonCyan else Color.Transparent)
                                        .clickable { activeCaptureMode = mode }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("mode_${mode.name}")
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) DeepSpaceBlack else LunarSilver,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Moon Info shortcut
                        IconButton(
                            onClick = onOpenMoonInfo,
                            modifier = Modifier
                                .size(40.dp)
                                .background(GraphiteSurface.copy(alpha = 0.7f), CircleShape)
                                .testTag("btn_open_moon_info")
                        ) {
                            Icon(Icons.Default.NightsStay, contentDescription = "Moon Info", tint = NeonCyan)
                        }
                    }

                    // Night Studio Sub-Modes Bar
                    AnimatedVisibility(visible = activeCaptureMode == CaptureMode.NIGHT_STUDIO) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GraphiteSurface.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(
                                NightSubMode.LIGHT_TRAILS to AppStrings.get("night_light_trails", language),
                                NightSubMode.LIGHT_PAINTING to AppStrings.get("night_light_painting", language),
                                NightSubMode.ASTROPHOTOGRAPHY to AppStrings.get("night_astrophotography", language)
                            ).forEach { (subMode, label) ->
                                val isSelected = activeNightSubMode == subMode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) ElectricViolet else Color.Transparent)
                                        .clickable { activeNightSubMode = subMode }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else LunarSilver,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom HUD: Controls & Shutter
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                ) {
                    CameraControlsHud(
                        captureMode = activeCaptureMode,
                        currentZoom = currentZoomRatio,
                        maxOpticalZoom = maxOpticalZoom,
                        exposureCompensation = exposureCompensation,
                        stackingFrames = stackingFrameCount,
                        timerSeconds = timerSeconds,
                        isTorchOn = isTorchEnabled,
                        isGridOn = isGridEnabled,
                        language = language,
                        onZoomChange = { zoomVal ->
                            currentZoomRatio = zoomVal
                            cameraControl?.setZoomRatio(zoomVal)
                        },
                        onExposureChange = { ev ->
                            exposureCompensation = ev
                            cameraControl?.setExposureCompensationIndex((ev * 3).toInt())
                        },
                        onStackingFramesChange = { stackingFrameCount = it },
                        onTimerToggle = {
                            timerSeconds = when (timerSeconds) {
                                0 -> 3
                                3 -> 5
                                5 -> 10
                                else -> 0
                            }
                        },
                        onTorchToggle = {
                            isTorchEnabled = !isTorchEnabled
                            cameraControl?.enableTorch(isTorchEnabled)
                        },
                        onGridToggle = { isGridEnabled = !isGridEnabled },
                        onSwitchLens = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                                CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        },
                        onCaptureClick = {
                            scope.launch {
                                // Execute timer if set
                                if (timerSeconds > 0) {
                                    isTimerCountingDown = true
                                    countdownRemaining = timerSeconds
                                    while (countdownRemaining > 0) {
                                        delay(1000)
                                        countdownRemaining--
                                    }
                                    isTimerCountingDown = false
                                }

                                if (activeCaptureMode == CaptureMode.MOON_PRO) {
                                    // Trigger computational multi-frame stacking sequence
                                    executeMultiFrameStacking(
                                        context = context,
                                        imageCapture = imageCapture,
                                        frameCount = stackingFrameCount,
                                        onProgressUpdate = { progress ->
                                            stackingProgress = progress
                                        },
                                        onCompleted = {
                                            scope.launch {
                                                galleryRepository.refreshGallery()
                                            }
                                        }
                                    )
                                } else {
                                    // Single photo capture
                                    captureSinglePhoto(
                                        context = context,
                                        imageCapture = imageCapture,
                                        isNightMode = activeCaptureMode == CaptureMode.NIGHT_STUDIO,
                                        onSaved = { file ->
                                            scope.launch {
                                                galleryRepository.refreshGallery()
                                                snackbarHostState.showSnackbar("ছবি সফলভাবে সংরক্ষিত হয়েছে: ${file.name}")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        // Stacking Progress Dialog
        if (stackingProgress != null) {
            StackingProcessingDialog(
                progress = stackingProgress!!,
                language = language,
                onDismiss = { stackingProgress = null },
                onOpenInPngStudio = { path ->
                    stackingProgress = null
                    onOpenPngStudio(path)
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private suspend fun captureSinglePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    isNightMode: Boolean,
    onSaved: (File) -> Unit
) = withContext(Dispatchers.IO) {
    val capture = imageCapture ?: return@withContext
    val picturesDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "MoonStudio")
    if (!picturesDir.exists()) picturesDir.mkdirs()

    val prefix = if (isNightMode) "Night" else "Photo"
    val photoFile = File(picturesDir, "${prefix}_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    capture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onSaved(photoFile)
            }
            override fun onError(exception: ImageCaptureException) {
                // error handled
            }
        }
    )
}

private suspend fun executeMultiFrameStacking(
    context: Context,
    imageCapture: ImageCapture?,
    frameCount: Int,
    onProgressUpdate: (StackingProgress) -> Unit,
    onCompleted: () -> Unit
) = withContext(Dispatchers.IO) {
    val capture = imageCapture ?: return@withContext

    onProgressUpdate(
        StackingProgress(
            stage = StackingStage.CAPTURING,
            currentFrame = 0,
            totalFrames = frameCount
        )
    )

    val capturedBitmaps = mutableListOf<Bitmap>()
    val picturesDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "MoonStudio")
    if (!picturesDir.exists()) picturesDir.mkdirs()

    // Capture burst sequence in memory
    for (i in 1..frameCount) {
        onProgressUpdate(
            StackingProgress(
                stage = StackingStage.CAPTURING,
                currentFrame = i,
                totalFrames = frameCount
            )
        )

        val tempFile = File(context.cacheDir, "temp_burst_$i.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(tempFile).build()

        val captured = kotlinx.coroutines.suspendCancellableCoroutine<Boolean> { cont ->
            capture.takePicture(
                options,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        if (cont.isActive) cont.resume(true, null)
                    }
                    override fun onError(exception: ImageCaptureException) {
                        if (cont.isActive) cont.resume(false, null)
                    }
                }
            )
        }

        if (captured && tempFile.exists()) {
            val bmp = BitmapFactory.decodeFile(tempFile.absolutePath)
            if (bmp != null) {
                capturedBitmaps.add(bmp)
            }
            tempFile.delete()
        }
        delay(120) // bounded delay between burst frames for optical stability
    }

    if (capturedBitmaps.isEmpty()) {
        onProgressUpdate(
            StackingProgress(
                stage = StackingStage.FAILED,
                errorMessage = "ক্যাপচার ব্যর্থ হয়েছে"
            )
        )
        return@withContext
    }

    val finalStackedFile = File(picturesDir, "Moon_Stacked_${System.currentTimeMillis()}.jpg")
    MoonStackingEngine.stackFrames(
        frames = capturedBitmaps,
        outputFile = finalStackedFile,
        onProgress = { progress ->
            onProgressUpdate(progress)
        }
    )
    onCompleted()
}
