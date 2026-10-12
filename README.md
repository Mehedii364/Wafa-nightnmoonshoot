# Wafa NightShot Ultra (ওয়াফা নাইটশট আল্ট্রা)

**Professional Moon Photography, Night Camera & Transparent PNG Studio**

- **Developer:** Mehedi364 (Md. Mehedi Hasan)
- **Brand:** Wafa Zone
- **Target Device:** OPPO Reno13 F 4G (CPH2701)
- **Primary Language:** বাংলা (Bangla)
- **Secondary Language:** English
- **Package ID:** `site.wafazone.nightshotultra`
- **Architecture:** Kotlin, Jetpack Compose, Material 3, CameraX & Camera2, Custom Computational Vision & Stacking Pipeline

---

## 🌟 Features Overview

### 1. 🌙 Moon Photography Pro
- **Live Viewfinder & Tracking:** Real-time on-device computer vision frame analysis detecting circular and crescent lunar contours with smoothed coordinates to eliminate handheld jitter.
- **Continuous Tracking States:** Searching for Moon, Moon Detected (Lock), Moon Partially Obscured, Low Confidence, and Moon Not Detected.
- **Zoom System (1x – 40x+):** Direct presets (`1x`, `2x`, `5x`, `10x`, `15x`, `20x`, `30x`, `40x`), smooth pinch-to-zoom, and explicit **Digital Crop** indicator when exceeding physical sensor bounds.
- **Highlight Clipping Assistant:** Real-time luminance monitoring preventing lunar maria craters from blowing out. Prompts user to lower exposure (EV -2.0 to -3.0).
- **Multi-Frame Moon Stacking Engine:** 
  - Captures burst sequences (5, 10, 20, 30 frames).
  - Laplacian variance filter rejects blurred or shaken frames.
  - Sub-pixel centroid alignment adjusts for lunar drift and vibration.
  - Mean/median weighted blending delivers true SNR noise reduction.
  - Conservative unsharp masking preserves real surface craters without fake AI hallucinations.

### 2. 🌌 Night Photography Studio
- **Light Trails:** Long-exposure compositing for traffic headlights and light streaks.
- **Light Painting:** Multi-frame live accumulation mode.
- **Astrophotography:** Multi-exposure night sky mode with noise reduction.

### 3. ✂️ Transparent Moon PNG Studio (32-bit RGBA)
- **Luminance & Edge Adaptive Segmentation:** Isolates the lunar disk and crescent horns from dark night skies.
- **Mask Refinement Brush:** Interactive Add-to-mask and Remove-from-mask brushes with size adjustment, undo/redo, and reset.
- **Preview Modes:** View cutouts on Checkerboard (transparency), Solid Black, Solid White, and Night Sky backgrounds, with before/after split slider.
- **Lossless PNG Export:** Exports true 32-bit RGBA PNG with alpha transparency channel.

### 4. ✍️ Signature & Watermark Studio
- **Touch/Stylus Signature Pad:** Draw custom signatures with bezier curve smoothing and transparent PNG export.
- **PNG Logo Import:** Import transparent logos via Android Photo Picker.
- **Text & Timestamp Watermarks:** Position presets (Bottom-Right, Bottom-Left, Top-Right, Top-Left, Center) with opacity and scaling sliders.

### 5. 🔭 Astronomical Planning & Hardware Discovery
- **Astronomical Calculator:** Real mathematical approximation for current lunar phase, illumination percentage, synodic age (0..29.53 days), and distance.
- **Hardware Diagnostics:** Live inspection of the camera sensor capabilities on OPPO Reno13 F 4G (CPH2701) and other Android devices.

---

## 📦 Verified APK Downloads

Compiled directly with the Android Gradle Plugin and verified:

- **Local Build Output:** `.build-outputs/app-debug.apk`
- **Direct Download Folder:** `APK_DOWNLOAD/app-debug.apk`

### Verification Summary
- **Format:** 32-bit compiled Android APK (Zip archive, Dalvik Executable, verified AndroidManifest.xml)
- **Size:** ~26.5 MB
- **Signature:** Debug keystore verified
- **Integrity:** `unzip -t` verified (zero corruption errors)

---

## 🛠️ Local Build Instructions

### Prerequisites
- Android Studio Ladybug / Meerkat or higher
- JDK 17 or higher
- Android SDK 36 (minSdk 24)

### Clone & Compile
```bash
git clone https://github.com/Mehedii364/WafaNightShotUltra.git
cd WafaNightShotUltra

# Run unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug
```

Compiled APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🤖 GitHub Actions CI/CD Pipeline

The repository includes `.github/workflows/android-build.yml` configured to:
1. Check out code and set up JDK 17.
2. Run local unit & Robolectric tests.
3. Build the debug APK with Gradle.
4. Verify checksums and upload the APK as an artifact.
5. Automatically sync the APK back into `APK_DOWNLOAD/app-debug.apk`.

---

## 👨‍💻 Developer & Brand Attribution

- **Application:** Wafa NightShot Ultra
- **Brand:** Wafa Zone
- **Lead Developer:** Mehedi364 (Md. Mehedi Hasan)
