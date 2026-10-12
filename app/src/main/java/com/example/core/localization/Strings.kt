package com.example.core.localization

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    BANGLA("bn", "Bangla", "বাংলা"),
    ENGLISH("en", "English", "English")
}

object AppStrings {
    fun get(key: String, lang: AppLanguage = AppLanguage.BANGLA): String {
        val entry = strings[key] ?: return key
        return when (lang) {
            AppLanguage.BANGLA -> entry.bn
            AppLanguage.ENGLISH -> entry.en
        }
    }

    private data class Entry(val bn: String, val en: String)

    private val strings = mapOf(
        "app_name" to Entry("ওয়াফা নাইটশট আল্ট্রা", "Wafa NightShot Ultra"),
        "brand_name" to Entry("ওয়াফা জোন", "Wafa Zone"),
        "developer_name" to Entry("মো. মেহেদী হাসান (Mehedi364)", "Md. Mehedi Hasan (Mehedi364)"),
        "tagline" to Entry("প্রফেশনাল চাঁদ ফটোগ্রাফি ও স্বচ্ছ পিএনজি স্টুডিও", "Professional Moon Photography & Transparent PNG Studio"),
        
        // Navigation Tabs
        "tab_camera" to Entry("ক্যামেরা", "Camera"),
        "tab_moon" to Entry("মুন মোড", "Moon Mode"),
        "tab_night" to Entry("নাইট স্টুডিও", "Night Studio"),
        "tab_png_studio" to Entry("পিএনজি স্টুডিও", "PNG Studio"),
        "tab_gallery" to Entry("গ্যালারি", "Gallery"),
        "tab_settings" to Entry("সেটিংস", "Settings"),
        "tab_moon_info" to Entry("চাঁদের তথ্য", "Moon Info"),

        // Moon Detection States
        "moon_searching" to Entry("চাঁদ অনুসন্ধান করা হচ্ছে...", "Searching for Moon..."),
        "moon_detected" to Entry("চাঁদ সনাক্ত হয়েছে • ট্র্যাকিং লক", "Moon Detected • Tracking Locked"),
        "moon_partially_obscured" to Entry("চাঁদ আংশিক মেঘাচ্ছন্ন", "Moon Partially Obscured"),
        "moon_low_confidence" to Entry("কম নির্ভুলতা • ম্যানুয়াল লক্ষ্য করুন", "Low Confidence • Aim Manually"),
        "moon_not_detected" to Entry("চাঁদ পাওয়া যায়নি", "Moon Not Detected"),
        "moon_locked" to Entry("লক করা হয়েছে", "LOCKED"),

        // Camera Controls
        "shutter" to Entry("ছবি তুলুন", "Capture"),
        "zoom" to Entry("জুম", "Zoom"),
        "digital_crop" to Entry("ডিজিটাল ক্রপ", "Digital Crop"),
        "optical_zoom" to Entry("অপটিক্যাল জুম", "Optical Zoom"),
        "exposure" to Entry("এক্সপোজার", "Exposure"),
        "focus" to Entry("ফোকাস", "Focus"),
        "focus_auto" to Entry("অটো ফোকাস", "Auto Focus"),
        "focus_manual" to Entry("ম্যানুয়াল", "Manual"),
        "timer" to Entry("টাইমার", "Timer"),
        "timer_off" to Entry("বন্ধ", "Off"),
        "grid" to Entry("গ্রিড", "Grid"),
        "level" to Entry("লেভেল", "Level"),
        "highlight_warning" to Entry("উজ্জ্বলতা সতর্কতা", "Highlight Warning"),
        "highlight_clipping_alert" to Entry("উজ্জ্বলতা বেশি! এক্সপোজার কমান", "Highlight Clipping! Lower EV"),
        "flash_torch" to Entry("টর্চ", "Torch"),
        "switch_lens" to Entry("ক্যামেরা পরিবর্তন", "Switch Lens"),

        // Multi-frame Stacking
        "stacking_title" to Entry("মাল্টি-ফ্রেম মুন স্ট্যাকিং", "Multi-Frame Moon Stacking"),
        "stack_frames" to Entry("ফ্রেম সংখ্যা", "Frames"),
        "stacking_in_progress" to Entry("স্ট্যাকিং প্রক্রিয়াকরণ হচ্ছে...", "Processing Stacking..."),
        "stack_capturing" to Entry("ফ্রেম ক্যাপচার করা হচ্ছে", "Capturing frames"),
        "stack_aligning" to Entry("লুনার ফিচার সারিবদ্ধ করা হচ্ছে", "Aligning lunar features"),
        "stack_blending" to Entry("নয়েজ হ্রাস ও মিশ্রণ করা হচ্ছে", "Noise reduction & blending"),
        "stack_sharpening" to Entry("সংরক্ষিত ডিটেইল শার্প করা হচ্ছে", "Conservative sharpening"),
        "stack_complete" to Entry("স্ট্যাকিং সফলভাবে সম্পন্ন", "Stacking Complete"),
        "stack_stats_aligned" to Entry("সংযুক্ত ফ্রেম", "Frames Aligned"),
        "stack_stats_rejected" to Entry("বাতিল ফ্রেম (অস্পষ্ট/ঝাপসা)", "Rejected (Blurred)"),
        "stack_stats_noise" to Entry("নয়েজ হ্রাস", "Noise Reduction"),

        // Night Studio
        "night_light_trails" to Entry("লাইট ট্রেইল", "Light Trails"),
        "night_light_painting" to Entry("লাইট পেইন্টিং", "Light Painting"),
        "night_astrophotography" to Entry("অ্যাস্ট্রোফটোগ্রাফি", "Astrophotography"),
        "night_trails_desc" to Entry("গাড়ির হেডলাইট ও উজ্জ্বল আলোর রেখা রেকর্ড করুন", "Capture moving vehicle lights and smooth luminous trails"),
        "night_painting_desc" to Entry("হাতের আলোর সাহায্যে আঁকিবুঁকি ও কম্বাইন্ড এক্সপোজার", "Draw with light torches in a dark environment"),
        "night_astro_desc" to Entry("ট্রাইপড সহকারে পরিষ্কার রাতের আকাশ ও তারা ক্যাপচার", "Tripod-assisted multi-exposure night sky capture"),
        "start_night_capture" to Entry("রেকর্ডিং শুরু করুন", "Start Capture"),
        "stop_night_capture" to Entry("রেকর্ডিং সমাপ্ত করুন", "Stop & Process"),

        // PNG Studio
        "png_studio_title" to Entry("ট্রান্সপারেন্ট মুন পিএনজি স্টুডিও", "Transparent Moon PNG Studio"),
        "png_select_image" to Entry("ছবি নির্বাচন করুন", "Select Image"),
        "png_auto_segment" to Entry("স্বয়ংক্রিয় ব্যাকগ্রাউন্ড অপসারণ", "Auto Segment Moon"),
        "png_brush_add" to Entry("মাস্ক যোগ", "Add Mask"),
        "png_brush_remove" to Entry("মাস্ক বাদ", "Erase Mask"),
        "png_brush_size" to Entry("ব্রাশের সাইজ", "Brush Size"),
        "png_undo" to Entry("পূর্বাবস্থায়", "Undo"),
        "png_redo" to Entry("পুনরায়", "Redo"),
        "png_reset" to Entry("রিসেট", "Reset"),
        "png_bg_checker" to Entry("স্বচ্ছ গ্রিড", "Checkerboard"),
        "png_bg_black" to Entry("কালো", "Black"),
        "png_bg_white" to Entry("সাদা", "White"),
        "png_bg_sky" to Entry("রাতের আকাশ", "Night Sky"),
        "png_split_compare" to Entry("তুলনা", "Compare"),
        "png_export_btn" to Entry("স্বচ্ছ পিএনজি এক্সপোর্ট (32-bit RGBA)", "Export Transparent PNG (32-bit)"),
        "png_export_success" to Entry("পিএনজি সফলভাবে সংরক্ষিত হয়েছে!", "PNG successfully exported!"),

        // Signature & Watermark
        "watermark_title" to Entry("কাস্টম স্বাক্ষর ও লোগো স্টুডিও", "Signature & Watermark Studio"),
        "draw_signature" to Entry("স্বাক্ষর অঙ্কন", "Draw Signature"),
        "clear_signature" to Entry("মুছে ফেলুন", "Clear"),
        "save_signature" to Entry("স্বাক্ষর সংরক্ষণ", "Save Signature"),
        "import_logo" to Entry("পিএনজি লোগো ইম্পোর্ট", "Import PNG Logo"),
        "text_watermark" to Entry("টেক্সট ওয়াটারমার্ক", "Text Watermark"),
        "watermark_opacity" to Entry("স্বচ্ছতা (Opacity)", "Opacity"),
        "watermark_scale" to Entry("আকার (Scale)", "Scale"),
        "watermark_position" to Entry("অবস্থান", "Position"),
        "pos_bottom_right" to Entry("নিচে-ডানে", "Bottom Right"),
        "pos_bottom_left" to Entry("নিচে-বামে", "Bottom Left"),
        "pos_top_right" to Entry("উপরে-ডানে", "Top Right"),
        "pos_top_left" to Entry("উপরে-বামে", "Top Left"),
        "pos_center" to Entry("কেন্দ্রে", "Center"),
        "apply_watermark" to Entry("ওয়াটারমার্ক প্রয়োগ ও সংরক্ষণ", "Apply & Save Copy"),

        // Gallery
        "gallery_all" to Entry("সব ছবি", "All Photos"),
        "gallery_moon" to Entry("চাঁদের ছবি", "Moon"),
        "gallery_stacked" to Entry("স্ট্যাকড", "Stacked"),
        "gallery_png" to Entry("স্বচ্ছ পিএনজি", "Transparent PNG"),
        "gallery_watermarked" to Entry("ওয়াটারমার্কড", "Watermarked"),
        "gallery_empty" to Entry("এখনও কোনো ছবি সংরক্ষিত হয়নি", "No photos captured yet"),
        "gallery_details" to Entry("বিস্তারিত তথ্য", "EXIF Details"),
        "share" to Entry("শেয়ার করুন", "Share"),
        "delete" to Entry("মুছে ফেলুন", "Delete"),
        "confirm_delete" to Entry("আপনি কি নিশ্চিত এই ছবিটি মুছে ফেলতে চান?", "Are you sure you want to delete this photo?"),
        "open_in_png" to Entry("পিএনজি স্টুডিওতে খুলুন", "Open in PNG Studio"),
        "open_in_watermark" to Entry("ওয়াটারমার্ক যোগ করুন", "Add Watermark"),

        // Moon Info / Astronomy
        "moon_phase_title" to Entry("আজকের চাঁদের দশা ও তথ্য", "Current Moon Phase & Details"),
        "illumination" to Entry("আলোকিত অংশ", "Illumination"),
        "lunar_age" to Entry("চাঁদের বয়স", "Lunar Age"),
        "moon_distance" to Entry("পৃথিবী থেকে দূরত্ব", "Moon Distance"),
        "photo_tips" to Entry("ফটোগ্রাফি টিপস", "Photography Tips"),
        "phase_new_moon" to Entry("অমাবস্যা (New Moon)", "New Moon"),
        "phase_waxing_crescent" to Entry("ক্রমবর্ধমান হেলাল (Waxing Crescent)", "Waxing Crescent"),
        "phase_first_quarter" to Entry("প্রথম চতুর্থাংশ (First Quarter)", "First Quarter"),
        "phase_waxing_gibbous" to Entry("ক্রমবর্ধমান কুঁজো চাঁদ (Waxing Gibbous)", "Waxing Gibbous"),
        "phase_full_moon" to Entry("পূর্ণিমা (Full Moon)", "Full Moon"),
        "phase_waning_gibbous" to Entry("ক্ষীয়মাণ কুঁজো চাঁদ (Waning Gibbous)", "Waning Gibbous"),
        "phase_last_quarter" to Entry("শেষ চতুর্থাংশ (Last Quarter)", "Last Quarter"),
        "phase_waning_crescent" to Entry("ক্ষীয়মাণ হেলাল (Waning Crescent)", "Waning Crescent"),

        // Hardware Diagnostics (OPPO Reno13 F 4G CPH2701)
        "hw_report_title" to Entry("হার্ডওয়্যার সামঞ্জস্য রিপোর্ট", "Hardware Compatibility Report"),
        "hw_target_device" to Entry("লক্ষ্য ডিভাইস: OPPO Reno13 F 4G (CPH2701)", "Target Device: OPPO Reno13 F 4G (CPH2701)"),
        "hw_level" to Entry("ক্যামেরা হার্ডওয়্যার স্তর", "Hardware Level"),
        "hw_zoom_range" to Entry("সমর্থিত ডিজিটাল জুম পরিসর", "Supported Zoom Range"),
        "hw_ois" to Entry("অপটিক্যাল স্ট্যাবিলাইজেশন (OIS)", "Optical Stabilization (OIS)"),
        "hw_raw" to Entry("RAW / DNG সমর্থন", "RAW / DNG Capability"),
        "hw_iso_range" to Entry("ISO সংবেদনশীলতা পরিসর", "ISO Sensitivity Range"),
        "hw_exposure_range" to Entry("এক্সপোজার টাইম পরিসর", "Exposure Time Range"),
        "hw_af_modes" to Entry("অটোফোকাস মোডসমূহ", "Supported AF Modes"),
        "hw_focal_lengths" to Entry("ফোকাল দৈর্ঘ্যসমূহ", "Focal Lengths"),

        // Settings & About
        "language_select" to Entry("অ্যাপের ভাষা", "App Language"),
        "pref_grid" to Entry("ক্যামেরা কম্পোজিশন গ্রিড", "Composition Grid"),
        "pref_auto_moon" to Entry("স্বয়ংক্রিয় চাঁদ সনাক্তকরণ", "Auto Moon Detection"),
        "pref_raw" to Entry("RAW ফরম্যাটে সংরক্ষণ (সমর্থিত হলে)", "Save RAW when available"),
        "pref_haptic" to Entry("হ্যাপটিক ফিডব্যাক", "Haptic Feedback"),
        "about_title" to Entry("অ্যাপ সম্পর্কে", "About Application"),
        "developed_by" to Entry("ডেভেলপ করেছেন: Mehedi364", "Developed by: Mehedi364"),
        "dev_full_name" to Entry("মোঃ মেহেদী হাসান (Md. Mehedi Hasan)", "Md. Mehedi Hasan"),
        "brand_label" to Entry("ব্র্যান্ড: Wafa Zone", "Brand: Wafa Zone"),
        "about_note" to Entry("প্রকৃত ক্যামেরা ডেটা ও কম্পিউটেশনাল ফটোগ্রাফি ইঞ্জিনের সমন্বয়ে তৈরি। কৃত্রিম এআই জেনারেশন ছাড়াই নিখুঁত ছবি উপহার দেওয়াই এই অ্যাপের লক্ষ্য।", "Built with real Android camera telemetry & computational stacking algorithms. No fake AI moon textures.")
    )
}
