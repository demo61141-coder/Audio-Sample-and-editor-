package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AppLanguage(val code: String, val displayName: String) {
    BN("bn", "বাংলা"),
    EN("en", "English")
}

fun tr(lang: AppLanguage, bn: String, en: String): String =
    if (lang == AppLanguage.BN) bn else en

@Entity(tableName = "audio_tracks")
data class AudioTrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val durationSec: Float,
    val sampleRate: Int = 22050,
    val peakDb: Float = -6.0f,
    val rmsDb: Float = -18.0f,
    val noiseFloorDb: Float = -48.0f,
    val silenceRemovedSec: Float = 0.0f,
    val isNoiseFiltered: Boolean = false,
    val isNormalized: Boolean = false,
    val aiPresetApplied: String = "Original",
    val waveformCsv: String = "",
    val silenceMaskCsv: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "noise_profiles")
data class NoiseProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val capturedDurationSec: Float,
    val noiseFloorDb: Float,
    val lowBandEnergy: Float,   // 60 - 250 Hz (Fan/AC Hum)
    val midBandEnergy: Float,   // 250 - 2000 Hz (Room Ambience)
    val highBandEnergy: Float,  // 2000 - 8000 Hz (Hiss / Air)
    val spectralBandsCsv: String, // 16-band fingerprint for visualizer & spectral subtraction
    val sourceDescription: String,
    val createdAt: Long = System.currentTimeMillis()
)

enum class SilenceRemovalMode(
    val labelBn: String,
    val labelEn: String,
    val subtitleBn: String,
    val subtitleEn: String
) {
    DUAL_COMBINED(
        labelBn = "ডুয়েল মোড (dB + সেকেন্ড লিমিট)",
        labelEn = "Dual Mode (dB + Seconds Limit)",
        subtitleBn = "নির্দিষ্ট dB-এর নিচে এবং নির্দিষ্ট সেকেন্ডের বেশি সাইলেন্ট থাকলে কাটবে",
        subtitleEn = "Removes segments below the dB threshold that last longer than the time limit"
    ),
    DB_ONLY(
        labelBn = "dB মোড (শুধু ডেসিবেল থ্রেশহোল্ড)",
        labelEn = "dB Threshold Mode Only",
        subtitleBn = "নির্দিষ্ট ডেসিবেলের নিচের সব সাইলেন্ট অংশ কেটে বাদ দেবে",
        subtitleEn = "Cuts all quiet sections falling below your chosen decibel (dB) level"
    ),
    SECONDS_LIMIT_ONLY(
        labelBn = "সেকেন্ড লিমিট মোড (Time Limit)",
        labelEn = "Seconds Limit Mode (e.g. > 2s)",
        subtitleBn = "যেমন: ২ সেকেন্ডের বেশি একটানা সাইলেন্ট থাকলে রিমুভ করবে",
        subtitleEn = "Removes any continuous pause longer than your selected seconds limit"
    );

    fun title(lang: AppLanguage): String = tr(lang, labelBn, labelEn)
    fun subtitle(lang: AppLanguage): String = tr(lang, subtitleBn, subtitleEn)
}

enum class AiVoicePreset(
    val id: String,
    val titleBn: String,
    val titleEn: String,
    val descBn: String,
    val descEn: String,
    val highPassHz: Float,
    val warmthBoostDb: Float,
    val clarityBoostDb: Float,
    val deEssAmount: Float,
    val targetPeakDb: Float,
    val compressionRatio: Float
) {
    CRYSTAL_PODCAST(
        id = "crystal_podcast",
        titleBn = "ক্রিস্টাল পডকাস্ট (Crystal Podcast)",
        titleEn = "Crystal Podcast Studio",
        descBn = "গভীর কণ্ঠ, ঝকঝকে উচ্চারণ ও স্টুডিও লেভেলিং (-14 LUFS)",
        descEn = "Deep vocal body, articulate presence & broadcast loudness leveling",
        highPassHz = 85f,
        warmthBoostDb = 3.5f,
        clarityBoostDb = 4.8f,
        deEssAmount = 0.45f,
        targetPeakDb = -1.0f,
        compressionRatio = 3.2f
    ),
    BROADCAST_WARM(
        id = "broadcast_warm",
        titleBn = "ব্রডকাস্ট ওয়ার্ম ভয়েস (Broadcast Radio)",
        titleEn = "Broadcast Warm Radio",
        descBn = "ভরাট বেস ও রেডিও অ্যানাউন্সার স্টাইল কণ্ঠস্বর",
        descEn = "Rich low-mid warmth & smooth dynamic compression for voiceovers",
        highPassHz = 70f,
        warmthBoostDb = 5.2f,
        clarityBoostDb = 3.0f,
        deEssAmount = 0.55f,
        targetPeakDb = -0.8f,
        compressionRatio = 4.0f
    ),
    LECTURE_CLARITY(
        id = "lecture_clarity",
        titleBn = "লেকচার ও টিউটোরিয়াল ক্লিয়ারিটি",
        titleEn = "Lecture & Tutorial Clarity",
        descBn = "হালকা গলার কথাও পরিষ্কার ও সমান ভলিউমে শোনাবে",
        descEn = "Boosts soft spoken syllables & maximizes speech intelligibility",
        highPassHz = 110f,
        warmthBoostDb = 1.8f,
        clarityBoostDb = 6.0f,
        deEssAmount = 0.35f,
        targetPeakDb = -1.0f,
        compressionRatio = 4.5f
    ),
    DOCUMENTARY_CRISP(
        id = "documentary_crisp",
        titleBn = "ডকুমেন্টারি ক্রিস্প (Ultra Crisp)",
        titleEn = "Documentary Ultra-Crisp",
        descBn = "রুম ইকো কমিয়ে সম্পূর্ণ ন্যাচারাল ও শার্প ভয়েস",
        descEn = "Tightens low-end mud and delivers natural, studio-grade narration",
        highPassHz = 95f,
        warmthBoostDb = 2.4f,
        clarityBoostDb = 5.2f,
        deEssAmount = 0.50f,
        targetPeakDb = -1.2f,
        compressionRatio = 2.8f
    );

    fun title(lang: AppLanguage): String = tr(lang, titleBn, titleEn)
    fun desc(lang: AppLanguage): String = tr(lang, descBn, descEn)
}

data class ManualTimeRange(
    val id: Long,
    val startSecText: String,
    val endSecText: String,
    val label: String
)

data class AudioSplitClip(
    val index: Int,
    val label: String,
    val startSec: Float,
    val endSec: Float,
    val durationSec: Float,
    val peakDb: Float,
    val filePath: String,
    val isManualRange: Boolean
)
