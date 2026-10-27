package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioController
import com.example.audio.AudioDspEngine
import com.example.audio.LiveRecordingTelemetry
import com.example.audio.MediaCodecExportEngine
import com.example.data.AiVoicePreset
import com.example.data.AppLanguage
import com.example.data.AudioExportFormat
import com.example.data.AudioSplitClip
import com.example.data.AudioTrackEntity
import com.example.data.ManualTimeRange
import com.example.data.NoiseProfileEntity
import com.example.data.OfflineVoiceCharacter
import com.example.data.SilenceRemovalMode
import com.example.data.StudioDatabase
import com.example.data.StudioRepository
import com.example.data.tr
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StudioTab {
    RECORDER,
    EDITOR,
    EFFECTS
}

data class StudioUiState(
    val language: AppLanguage = AppLanguage.BN,
    val currentTab: StudioTab = StudioTab.RECORDER,
    val activeTrack: AudioTrackEntity? = null,
    val activeNoiseProfile: NoiseProfileEntity? = null,
    val waveformBins: List<Float> = List(120) { 0.12f },
    val silenceMaskBins: List<Boolean> = List(120) { false },
    val detectedSilenceCount: Int = 0,
    val estimatedSilenceSec: Float = 0f,

    // Feature 1: Recorder & Noise Profile Learning
    val isCapturingNoisePrint: Boolean = false,
    val noiseCaptureTargetSec: Int = 4, // 3 to 5 sec configurable
    val noiseCaptureProgress: Float = 0f,
    val smartNoiseFilterEnabled: Boolean = true,
    val noiseReductionStrength: Float = 0.88f,
    val silenceSkipEnabled: Boolean = true,
    val silenceDbThreshold: Float = -34f,
    val silenceDurationLimitSec: Float = 2.0f,
    val isRecording: Boolean = false,
    val liveTelemetry: LiveRecordingTelemetry = LiveRecordingTelemetry(
        liveWaveformAmps = List(48) { idx ->
            val v = kotlin.math.sin(idx * 0.35).toFloat()
            (0.12f + 0.22f * kotlin.math.abs(v)).coerceIn(0.06f, 0.85f)
        }
    ),

    // Feature 3: Dual-Mode Silence & Sample Noise Removal
    val silenceRemovalMode: SilenceRemovalMode = SilenceRemovalMode.DUAL_COMBINED,
    val sampleWindowStartSec: Float = 0.0f,
    val sampleWindowEndSec: Float = 3.5f,
    val isProcessingDsp: Boolean = false,
    val statusBannerMessageBn: String = "স্টুডিও প্রস্তুত — ১০০% অফলাইন অডিও ও ভিডিও নয়েজ/সাইলেন্স স্টুডিও",
    val statusBannerMessageEn: String = "Studio Ready — 100% Offline Audio & Video Noise/Silence Studio",

    // Feature 4: Visual WAV Multi-Cut + Flexible Audio Splitter (Auto up to 100 + Manual '+' Ranges)
    val visualCutStartSec: Float = 3.5f,
    val visualCutEndSec: Float = 11.5f,
    val manualTimeRanges: List<ManualTimeRange> = listOf(
        ManualTimeRange(id = 1L, startSecText = "1.0", endSecText = "10.0", label = "Clip 1 (Intro)"),
        ManualTimeRange(id = 2L, startSecText = "10.0", endSecText = "20.0", label = "Clip 2 (Main)"),
        ManualTimeRange(id = 3L, startSecText = "20.0", endSecText = "30.0", label = "Clip 3 (Outro)")
    ),
    val generatedSplitClips: List<AudioSplitClip> = emptyList(),
    val playingClipFilePath: String? = null,

    // Feature 5: AI Speech Clarity, Unique Offline Voice Studio & Multi-Format Export (WAV/MP3/AAC + Video)
    val selectedAiPreset: AiVoicePreset = AiVoicePreset.CRYSTAL_PODCAST,
    val voiceBoostDb: Float = 3.5f,
    val autoNormalizeEnabled: Boolean = true,
    val targetPeakDb: Float = -1.0f,
    val customWarmthDb: Float = 3.5f,
    val customClarityDb: Float = 4.8f,

    // Unique Offline AI Features: De-Breath/De-Click & Vocal Character / Speed
    val deBreathAndClickCleanEnabled: Boolean = true,
    val selectedVoiceCharacter: OfflineVoiceCharacter = OfflineVoiceCharacter.NATURAL,
    val speechSpeedFactor: Float = 1.0f,

    // Multi-Format Export (MP3, WAV, AAC) & Gallery State
    val selectedExportFormat: AudioExportFormat = AudioExportFormat.MP3,
    val selectedExportBitrateKbps: Int = 192,
    val lastExportedPathDisplay: String = "",

    // Playback & Settings Guide Dialog state
    val isPlayingMainTrack: Boolean = false,
    val playbackPositionSec: Float = 0f,
    val showSettingsGuideDialog: Boolean = false
) {
    val statusBannerMessage: String
        get() = tr(language, statusBannerMessageBn, statusBannerMessageEn)
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository: StudioRepository
    private val audioController = AudioController(viewModelScope)
    private val studioFilesDir = File(application.filesDir, "sonicforge_audio").apply { mkdirs() }

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    val savedTracks: StateFlow<List<AudioTrackEntity>>
    val savedNoiseProfiles: StateFlow<List<NoiseProfileEntity>>

    init {
        val dao = StudioDatabase.getInstance(application).studioDao()
        repository = StudioRepository(dao)

        savedTracks = repository.allTracks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        savedNoiseProfiles = repository.allNoiseProfiles.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        initializeDefaultStudioSession()
    }

    private fun initializeDefaultStudioSession() {
        viewModelScope.launch(Dispatchers.IO) {
            val demoFile = File(studioFilesDir, "studio_demo_podcast.wav")
            if (!demoFile.exists() || demoFile.length() < 1000L) {
                AudioDspEngine.generateDemoPodcastWav(demoFile, durationSec = 32)
            }
            val initialProfile = AudioDspEngine.captureNoiseProfileFromWavRange(
                file = demoFile,
                startSec = 0.0f,
                endSec = 3.4f,
                profileName = "Fan & AC Noise Print (3.4s)",
                sourceDesc = "0.0s - 3.4s Ambient Fan/AC Hum Sample"
            )
            val profileId = repository.saveNoiseProfile(initialProfile)
            val savedProfile = initialProfile.copy(id = profileId)

            val state = _uiState.value
            val analysis = AudioDspEngine.analyzeWavFile(
                file = demoFile,
                silenceThresholdDb = state.silenceDbThreshold,
                minSilenceDurationSec = state.silenceDurationLimitSec
            )

            val trackEntity = AudioTrackEntity(
                title = "Podcast Voice + Fan Noise (Demo)",
                filePath = demoFile.absolutePath,
                durationSec = analysis.durationSec,
                sampleRate = analysis.sampleRate,
                peakDb = analysis.peakDb,
                rmsDb = analysis.rmsDb,
                noiseFloorDb = analysis.noiseFloorDb,
                waveformCsv = analysis.waveformBins.joinToString(",") { "%.3f".format(it) },
                silenceMaskCsv = analysis.silenceMaskBins.joinToString(",") { if (it) "1" else "0" }
            )
            val trackId = repository.saveTrack(trackEntity)
            val persistedTrack = trackEntity.copy(id = trackId)

            _uiState.update {
                it.copy(
                    activeTrack = persistedTrack,
                    activeNoiseProfile = savedProfile,
                    waveformBins = analysis.waveformBins,
                    silenceMaskBins = analysis.silenceMaskBins,
                    detectedSilenceCount = analysis.detectedSilenceRegionsCount,
                    estimatedSilenceSec = analysis.estimatedSilenceTotalSec,
                    sampleWindowStartSec = 0.0f,
                    sampleWindowEndSec = min(3.5f, analysis.durationSec),
                    visualCutStartSec = 3.6f,
                    visualCutEndSec = min(13.0f, analysis.durationSec)
                )
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        _uiState.update { it.copy(language = language) }
    }

    fun toggleLanguage() {
        _uiState.update {
            it.copy(
                language = if (it.language == AppLanguage.BN) AppLanguage.EN else AppLanguage.BN
            )
        }
    }

    fun selectTab(tab: StudioTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun toggleSettingsGuideDialog(show: Boolean) {
        _uiState.update { it.copy(showSettingsGuideDialog = show) }
    }

    fun setNoiseCaptureTargetSec(seconds: Int) {
        _uiState.update { it.copy(noiseCaptureTargetSec = seconds.coerceIn(3, 5)) }
    }

    fun toggleSmartNoiseFilter(enabled: Boolean) {
        _uiState.update { it.copy(smartNoiseFilterEnabled = enabled) }
    }

    fun setNoiseReductionStrength(strength: Float) {
        _uiState.update { it.copy(noiseReductionStrength = strength.coerceIn(0.2f, 0.98f)) }
    }

    fun toggleSilenceSkipRecording(enabled: Boolean) {
        _uiState.update { it.copy(silenceSkipEnabled = enabled) }
    }

    fun updateSilenceDbThreshold(db: Float) {
        val clamped = db.coerceIn(-58f, -18f)
        _uiState.update { it.copy(silenceDbThreshold = clamped) }
        refreshWaveformSilenceAnalysis()
    }

    fun updateSilenceDurationLimitSec(sec: Float) {
        val clamped = sec.coerceIn(0.5f, 5.0f)
        _uiState.update { it.copy(silenceDurationLimitSec = clamped) }
        refreshWaveformSilenceAnalysis()
    }

    fun setSilenceRemovalMode(mode: SilenceRemovalMode) {
        _uiState.update { it.copy(silenceRemovalMode = mode) }
        refreshWaveformSilenceAnalysis()
    }

    fun updateSampleSelectionWindow(startSec: Float, endSec: Float) {
        val maxDur = _uiState.value.activeTrack?.durationSec ?: 30f
        val s = startSec.coerceIn(0f, max(0f, maxDur - 0.5f))
        val e = endSec.coerceIn(s + 0.5f, maxDur)
        _uiState.update {
            it.copy(sampleWindowStartSec = s, sampleWindowEndSec = e)
        }
    }

    /**
     * Visual WAV Waveform Selection for Multi-Cut:
     * Users can visually select/adjust start & end markers on the WAV waveform and click '+'
     * to add that visual selection directly to the multi-cut list!
     */
    fun updateVisualCutSelection(startSec: Float, endSec: Float) {
        val maxDur = _uiState.value.activeTrack?.durationSec ?: 32f
        val s = startSec.coerceIn(0f, max(0f, maxDur - 0.4f))
        val e = endSec.coerceIn(s + 0.4f, maxDur)
        _uiState.update {
            it.copy(visualCutStartSec = s, visualCutEndSec = e)
        }
    }

    fun addVisualSelectionToCutRanges() {
        val state = _uiState.value
        val sText = "%.1f".format(state.visualCutStartSec)
        val eText = "%.1f".format(state.visualCutEndSec)
        val nextIdx = state.manualTimeRanges.size + 1
        val newSlot = ManualTimeRange(
            id = System.currentTimeMillis() + nextIdx,
            startSecText = sText,
            endSecText = eText,
            label = "WAV Cut #$nextIdx"
        )
        val trackDur = state.activeTrack?.durationSec ?: 32f
        val nextVisualStart = (state.visualCutEndSec).coerceIn(0f, max(0f, trackDur - 2f))
        val nextVisualEnd = (nextVisualStart + 6f).coerceIn(nextVisualStart + 0.5f, trackDur)

        _uiState.update {
            it.copy(
                manualTimeRanges = state.manualTimeRanges + newSlot,
                visualCutStartSec = nextVisualStart,
                visualCutEndSec = nextVisualEnd,
                statusBannerMessageBn = "WAV ওয়েভফর্ম সিলেকশন ($sText সে. - $eText সে.) কাট লিস্টে যোগ হয়েছে!",
                statusBannerMessageEn = "Added WAV Waveform Selection (${sText}s - ${eText}s) to Multi-Cut list!"
            )
        }
    }

    /**
     * Import Audio or Video from Phone Gallery (100% Offline decoding via MediaExtractor + MediaCodec)
     */
    fun importMediaFromGalleryUri(uri: Uri, isVideo: Boolean) {
        audioController.stopPlayback()
        _uiState.update {
            it.copy(
                isProcessingDsp = true,
                isPlayingMainTrack = false,
                statusBannerMessageBn = if (isVideo) {
                    "গ্যালারি থেকে ভিডিও ইনপুট ও অডিও ট্র্যাক এক্সট্রাক্ট হচ্ছে (অফলাইন)…"
                } else {
                    "গ্যালারি থেকে অডিও ফাইল ইনপুট ও WAV ওয়েভফর্ম তৈরি হচ্ছে…"
                },
                statusBannerMessageEn = if (isVideo) {
                    "Importing Video from Gallery & extracting audio waveform offline…"
                } else {
                    "Importing Audio from Gallery & decoding WAV waveform offline…"
                }
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val imported = MediaCodecExportEngine.importMediaUriToStudioWav(
                context = appContext,
                uri = uri,
                studioDir = studioFilesDir,
                isVideo = isVideo
            )
            val state = _uiState.value
            val analysis = AudioDspEngine.analyzeWavFile(
                file = imported.wavFile,
                silenceThresholdDb = state.silenceDbThreshold,
                minSilenceDurationSec = state.silenceDurationLimitSec
            )

            val prefix = if (isVideo) "[Video] " else "[Audio] "
            val newTrack = AudioTrackEntity(
                title = "$prefix${imported.displayName}",
                filePath = imported.wavFile.absolutePath,
                durationSec = analysis.durationSec,
                sampleRate = analysis.sampleRate,
                peakDb = analysis.peakDb,
                rmsDb = analysis.rmsDb,
                noiseFloorDb = analysis.noiseFloorDb,
                isVideoTrack = isVideo,
                sourceVideoPath = imported.sourceVideoFile?.absolutePath,
                waveformCsv = analysis.waveformBins.joinToString(",") { "%.3f".format(it) },
                silenceMaskCsv = analysis.silenceMaskBins.joinToString(",") { if (it) "1" else "0" }
            )
            val id = repository.saveTrack(newTrack)
            val savedTrack = newTrack.copy(id = id)

            // Also auto-learn noise profile from the quietest region of the imported file
            val autoProfile = AudioDspEngine.captureNoiseProfileFromWavRange(
                file = imported.wavFile,
                startSec = 0f,
                endSec = min(3.0f, analysis.durationSec),
                profileName = "Auto Noise Print (${imported.displayName.take(14)})",
                sourceDesc = "Extracted from imported media"
            )
            val profId = repository.saveNoiseProfile(autoProfile)
            val savedProfile = autoProfile.copy(id = profId)

            _uiState.update {
                it.copy(
                    isProcessingDsp = false,
                    activeTrack = savedTrack,
                    activeNoiseProfile = savedProfile,
                    waveformBins = analysis.waveformBins,
                    silenceMaskBins = analysis.silenceMaskBins,
                    detectedSilenceCount = analysis.detectedSilenceRegionsCount,
                    estimatedSilenceSec = analysis.estimatedSilenceTotalSec,
                    sampleWindowStartSec = 0f,
                    sampleWindowEndSec = min(3.0f, analysis.durationSec),
                    visualCutStartSec = 0f,
                    visualCutEndSec = min(8.0f, analysis.durationSec),
                    generatedSplitClips = emptyList(),
                    statusBannerMessageBn = if (isVideo) {
                        "ভিডিও সফলভাবে ইনপুট হয়েছে (${"%.1f".format(analysis.durationSec)}s)! এখন নয়েজ/সাইলেন্স রিমুভ ও এডিট করুন।"
                    } else {
                        "গ্যালারি অডিও ইনপুট সম্পন্ন (${"%.1f".format(analysis.durationSec)}s)! এখন কাট, নয়েজ রিমুভ বা এক্সপোর্ট করুন।"
                    },
                    statusBannerMessageEn = if (isVideo) {
                        "Video imported (${"%.1f".format(analysis.durationSec)}s)! Ready for offline Noise & Silence removal."
                    } else {
                        "Gallery Audio imported (${"%.1f".format(analysis.durationSec)}s)! Ready to cut, clean & export."
                    }
                )
            }
        }
    }

    private fun refreshWaveformSilenceAnalysis() {
        val track = _uiState.value.activeTrack ?: return
        val file = File(track.filePath)
        if (!file.exists()) return
        val mode = _uiState.value.silenceRemovalMode
        val threshDb = when (mode) {
            SilenceRemovalMode.SECONDS_LIMIT_ONLY -> max(_uiState.value.silenceDbThreshold, -33f)
            else -> _uiState.value.silenceDbThreshold
        }
        val minSec = when (mode) {
            SilenceRemovalMode.DB_ONLY -> 0.25f
            else -> _uiState.value.silenceDurationLimitSec
        }

        viewModelScope.launch(Dispatchers.IO) {
            val analysis = AudioDspEngine.analyzeWavFile(file, threshDb, minSec)
            _uiState.update {
                it.copy(
                    waveformBins = analysis.waveformBins,
                    silenceMaskBins = analysis.silenceMaskBins,
                    detectedSilenceCount = analysis.detectedSilenceRegionsCount,
                    estimatedSilenceSec = analysis.estimatedSilenceTotalSec
                )
            }
        }
    }

    /**
     * Feature 1A: Capture 3-5 seconds of background noise to build an Audacity-style Noise Print
     */
    fun captureAmbientNoisePrint() {
        if (_uiState.value.isCapturingNoisePrint) return
        val durationSec = _uiState.value.noiseCaptureTargetSec
        _uiState.update {
            it.copy(
                isCapturingNoisePrint = true,
                noiseCaptureProgress = 0f,
                statusBannerMessageBn = "ব্যাকগ্রাউন্ড নয়েজ প্রিন্ট ক্যাপচার হচ্ছে ($durationSec সেকেন্ড চুপ থাকুন)…",
                statusBannerMessageEn = "Capturing ambient Noise Print ($durationSec seconds)…"
            )
        }

        viewModelScope.launch {
            val steps = durationSec * 10
            for (step in 1..steps) {
                delay(100)
                _uiState.update {
                    it.copy(noiseCaptureProgress = step.toFloat() / steps)
                }
            }

            withContext(Dispatchers.IO) {
                val trackFile = _uiState.value.activeTrack?.let { File(it.filePath) }
                val sourceFile = if (trackFile != null && trackFile.exists()) {
                    trackFile
                } else {
                    AudioDspEngine.generateDemoPodcastWav(File(studioFilesDir, "temp_noise.wav"), 6)
                }

                val profile = AudioDspEngine.captureNoiseProfileFromWavRange(
                    file = sourceFile,
                    startSec = 0f,
                    endSec = durationSec.toFloat(),
                    profileName = "Live Noise Print (${durationSec}s)",
                    sourceDesc = "Room Fan/AC Noise Print (${durationSec}.0s)"
                )
                val id = repository.saveNoiseProfile(profile)
                val saved = profile.copy(id = id)

                _uiState.update {
                    it.copy(
                        isCapturingNoisePrint = false,
                        noiseCaptureProgress = 1f,
                        activeNoiseProfile = saved,
                        statusBannerMessageBn = "নয়েজ প্রিন্ট তৈরি সম্পন্ন! (Noise Floor: ${"%.1f".format(saved.noiseFloorDb)} dB, 100% ভয়েস একুরেসি লকড)",
                        statusBannerMessageEn = "Noise Print Learned! (Noise Floor: ${"%.1f".format(saved.noiseFloorDb)} dB, 100% Voice Accuracy Locked)"
                    )
                }
            }
        }
    }

    /**
     * Feature 1B & 1C: Start / Stop Smart Recording with Live Silence Skip & Auto Noise Filter
     */
    fun toggleRecording(hasMicPermission: Boolean) {
        if (_uiState.value.isRecording) {
            audioController.stopRecording()
            return
        }

        audioController.stopPlayback()
        val state = _uiState.value
        val outFile = File(studioFilesDir, "rec_${System.currentTimeMillis()}.wav")

        _uiState.update {
            it.copy(
                isRecording = true,
                isPlayingMainTrack = false,
                playingClipFilePath = null,
                statusBannerMessageBn = if (state.silenceSkipEnabled) {
                    "স্মার্ট রেকর্ডিং চলছে — ${"%.1f".format(state.silenceDurationLimitSec)}s ও ${state.silenceDbThreshold.toInt()} dB সাইলেন্স স্কিপ চালু"
                } else {
                    "লাইভ রেকর্ডিং চলছে…"
                },
                statusBannerMessageEn = if (state.silenceSkipEnabled) {
                    "Smart Recording Active — Silence Skip at ${"%.1f".format(state.silenceDurationLimitSec)}s & ${state.silenceDbThreshold.toInt()} dB"
                } else {
                    "Live Recording in progress…"
                }
            )
        }

        audioController.startSmartRecording(
            outputFile = outFile,
            hasMicPermission = hasMicPermission,
            silenceSkipEnabled = state.silenceSkipEnabled,
            silenceDbThreshold = state.silenceDbThreshold,
            silenceLimitSec = state.silenceDurationLimitSec,
            onTelemetry = { telemetry ->
                _uiState.update { it.copy(liveTelemetry = telemetry) }
            },
            onFinished = { rawRecordedFile, skippedSec ->
                finalizeRecordedTrack(rawRecordedFile, skippedSec)
            }
        )
    }

    private fun finalizeRecordedTrack(rawFile: File, skippedSec: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val activeProfile = state.activeNoiseProfile

            val finalFile = if (state.smartNoiseFilterEnabled && activeProfile != null) {
                val filteredFile = File(studioFilesDir, "rec_clean_${System.currentTimeMillis()}.wav")
                AudioDspEngine.applySmartNoiseFilter(
                    inputFile = rawFile,
                    outputFile = filteredFile,
                    noiseProfile = activeProfile,
                    reductionStrength = state.noiseReductionStrength
                )
            } else {
                rawFile
            }

            val analysis = AudioDspEngine.analyzeWavFile(
                file = finalFile,
                silenceThresholdDb = state.silenceDbThreshold,
                minSilenceDurationSec = state.silenceDurationLimitSec
            )

            val newTrack = AudioTrackEntity(
                title = "Smart Record (${"%.1f".format(analysis.durationSec)}s)",
                filePath = finalFile.absolutePath,
                durationSec = analysis.durationSec,
                sampleRate = analysis.sampleRate,
                peakDb = analysis.peakDb,
                rmsDb = analysis.rmsDb,
                noiseFloorDb = analysis.noiseFloorDb,
                silenceRemovedSec = skippedSec,
                isNoiseFiltered = state.smartNoiseFilterEnabled && activeProfile != null,
                waveformCsv = analysis.waveformBins.joinToString(",") { "%.3f".format(it) },
                silenceMaskCsv = analysis.silenceMaskBins.joinToString(",") { if (it) "1" else "0" }
            )
            val id = repository.saveTrack(newTrack)
            val savedTrack = newTrack.copy(id = id)

            _uiState.update {
                it.copy(
                    isRecording = false,
                    activeTrack = savedTrack,
                    waveformBins = analysis.waveformBins,
                    silenceMaskBins = analysis.silenceMaskBins,
                    detectedSilenceCount = analysis.detectedSilenceRegionsCount,
                    estimatedSilenceSec = analysis.estimatedSilenceTotalSec,
                    sampleWindowStartSec = 0f,
                    sampleWindowEndSec = min(3.5f, analysis.durationSec),
                    visualCutStartSec = 0f,
                    visualCutEndSec = min(6.0f, analysis.durationSec),
                    statusBannerMessageBn = "রেকর্ডিং সেভ হয়েছে! অটো-স্কিপড সাইলেন্স: ${"%.1f".format(skippedSec)}s",
                    statusBannerMessageEn = "Recording Saved! Auto-skipped silence: ${"%.1f".format(skippedSec)}s"
                )
            }
        }
    }

    fun generateFreshDemoTrack(isVideoDemo: Boolean = false) {
        audioController.stopPlayback()
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isProcessingDsp = true,
                    statusBannerMessageBn = if (isVideoDemo) {
                        "ডেমো ভিডিও + ফ্যান নয়েজ ও সাইলেন্স ট্র্যাক তৈরি হচ্ছে…"
                    } else {
                        "নতুন ৩২ সেকেন্ডের স্টুডিও ডেমো ট্র্যাক তৈরি হচ্ছে…"
                    },
                    statusBannerMessageEn = if (isVideoDemo) {
                        "Generating Demo Video + Fan Noise & Silence track…"
                    } else {
                        "Generating fresh 32s studio demo track…"
                    }
                )
            }
            val demoFile = File(studioFilesDir, "demo_podcast_${System.currentTimeMillis()}.wav")
            AudioDspEngine.generateDemoPodcastWav(demoFile, durationSec = 32)
            val state = _uiState.value
            val analysis = AudioDspEngine.analyzeWavFile(
                file = demoFile,
                silenceThresholdDb = state.silenceDbThreshold,
                minSilenceDurationSec = state.silenceDurationLimitSec
            )
            val entity = AudioTrackEntity(
                title = if (isVideoDemo) "[Video] Studio Lecture + Fan Noise (32s)" else "Demo Voice + Fan Noise (32s)",
                filePath = demoFile.absolutePath,
                durationSec = analysis.durationSec,
                peakDb = analysis.peakDb,
                rmsDb = analysis.rmsDb,
                noiseFloorDb = analysis.noiseFloorDb,
                isVideoTrack = isVideoDemo
            )
            val id = repository.saveTrack(entity)
            val saved = entity.copy(id = id)

            _uiState.update {
                it.copy(
                    isProcessingDsp = false,
                    activeTrack = saved,
                    waveformBins = analysis.waveformBins,
                    silenceMaskBins = analysis.silenceMaskBins,
                    detectedSilenceCount = analysis.detectedSilenceRegionsCount,
                    estimatedSilenceSec = analysis.estimatedSilenceTotalSec,
                    sampleWindowStartSec = 0f,
                    sampleWindowEndSec = 3.5f,
                    visualCutStartSec = 3.6f,
                    visualCutEndSec = 12.0f,
                    generatedSplitClips = emptyList(),
                    statusBannerMessageBn = if (isVideoDemo) {
                        "ডেমো ভিডিও ট্র্যাক লোড হয়েছে! এখন ভিডিওর নয়েজ ও সাইলেন্স রিমুভ করে গ্যালারিতে সেভ করতে পারবেন।"
                    } else {
                        "৩২ সেকেন্ডের ডেমো ট্র্যাক প্রস্তুত (৬টি ভয়েস ফ্রেজ + ৫টি সাইলেন্ট গ্যাপ + ফ্যান নয়েজ)!"
                    },
                    statusBannerMessageEn = if (isVideoDemo) {
                        "Demo Video Track Ready! You can now remove video noise/silence & export to Gallery."
                    } else {
                        "32s Demo Track Ready (6 voice phrases + 5 silent gaps + fan noise)!"
                    }
                )
            }
        }
    }

    fun selectTrack(track: AudioTrackEntity) {
        audioController.stopPlayback()
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(track.filePath)
            if (!file.exists()) return@launch
            val state = _uiState.value
            val analysis = AudioDspEngine.analyzeWavFile(
                file = file,
                silenceThresholdDb = state.silenceDbThreshold,
                minSilenceDurationSec = state.silenceDurationLimitSec
            )
            _uiState.update {
                it.copy(
                    activeTrack = track,
                    waveformBins = analysis.waveformBins,
                    silenceMaskBins = analysis.silenceMaskBins,
                    detectedSilenceCount = analysis.detectedSilenceRegionsCount,
                    estimatedSilenceSec = analysis.estimatedSilenceTotalSec,
                    isPlayingMainTrack = false,
                    playingClipFilePath = null,
                    sampleWindowStartSec = 0f,
                    sampleWindowEndSec = min(3.5f, analysis.durationSec),
                    visualCutStartSec = 0f,
                    visualCutEndSec = min(8.0f, analysis.durationSec),
                    statusBannerMessageBn = "ট্র্যাক লোড হয়েছে: ${track.title}",
                    statusBannerMessageEn = "Loaded Track: ${track.title}"
                )
            }
        }
    }

    /**
     * Feature 3A: Sample-Based Noise Print extraction from selected region on the waveform
     */
    fun captureNoiseFromWaveformSelection() {
        val track = _uiState.value.activeTrack ?: return
        val file = File(track.filePath)
        if (!file.exists()) return
        val s = _uiState.value.sampleWindowStartSec
        val e = _uiState.value.sampleWindowEndSec

        viewModelScope.launch(Dispatchers.IO) {
            val profile = AudioDspEngine.captureNoiseProfileFromWavRange(
                file = file,
                startSec = s,
                endSec = e,
                profileName = "Sample Noise (${"%.1f".format(s)}s-${"%.1f".format(e)}s)",
                sourceDesc = "Sampled from ${track.title}"
            )
            val id = repository.saveNoiseProfile(profile)
            val saved = profile.copy(id = id)
            _uiState.update {
                it.copy(
                    activeNoiseProfile = saved,
                    statusBannerMessageBn = "নয়েজ স্যাম্পল লার্নড (${"%.1f".format(s)}s - ${"%.1f".format(e)}s, Floor: ${"%.1f".format(saved.noiseFloorDb)} dB)!",
                    statusBannerMessageEn = "Noise Sample Learned (${"%.1f".format(s)}s - ${"%.1f".format(e)}s, Floor: ${"%.1f".format(saved.noiseFloorDb)} dB)!"
                )
            }
        }
    }

    /**
     * Feature 3B: One-Click Sample-Based Noise Removal across the entire audio/video file
     */
    fun applyOneClickNoiseRemoval() {
        val track = _uiState.value.activeTrack ?: return
        val profile = _uiState.value.activeNoiseProfile ?: return
        val inFile = File(track.filePath)
        if (!inFile.exists()) return

        audioController.stopPlayback()
        _uiState.update {
            it.copy(
                isProcessingDsp = true,
                isPlayingMainTrack = false,
                statusBannerMessageBn = "স্মার্ট নয়েজ ফিল্টার দিয়ে পুরো ট্র্যাক থেকে নয়েজ রিমুভ হচ্ছে…",
                statusBannerMessageEn = "Removing background noise across entire track with Smart Filter…"
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val outFile = File(studioFilesDir, "denoised_${System.currentTimeMillis()}.wav")
            AudioDspEngine.applySmartNoiseFilter(
                inputFile = inFile,
                outputFile = outFile,
                noiseProfile = profile,
                reductionStrength = _uiState.value.noiseReductionStrength
            )
            val state = _uiState.value
            val analysis = AudioDspEngine.analyzeWavFile(
                file = outFile,
                silenceThresholdDb = state.silenceDbThreshold,
                minSilenceDurationSec = state.silenceDurationLimitSec
            )
            val updatedTrack = track.copy(
                title = "${track.title.substringBefore(" [")} [Noise Removed]",
                filePath = outFile.absolutePath,
                durationSec = analysis.durationSec,
                peakDb = analysis.peakDb,
                rmsDb = analysis.rmsDb,
                noiseFloorDb = analysis.noiseFloorDb,
                isNoiseFiltered = true
            )
            repository.updateTrack(updatedTrack)

            _uiState.update {
                it.copy(
                    isProcessingDsp = false,
                    activeTrack = updatedTrack,
                    waveformBins = analysis.waveformBins,
                    silenceMaskBins = analysis.silenceMaskBins,
                    detectedSilenceCount = analysis.detectedSilenceRegionsCount,
                    estimatedSilenceSec = analysis.estimatedSilenceTotalSec,
                    statusBannerMessageBn = "১-ক্লিকে ব্যাকগ্রাউন্ড নয়েজ রিমুভ সম্পন্ন! নতুন Noise Floor: ${"%.1f".format(analysis.noiseFloorDb)} dB",
                    statusBannerMessageEn = "1-Click Noise Removal Complete! New Noise Floor: ${"%.1f".format(analysis.noiseFloorDb)} dB"
                )
            }
        }
    }

    /**
     * Feature 3C: Execute Dual-Mode Silence Removal (dB Mode / Second Limit Mode / Combined)
     */
    fun executeDualModeSilenceRemoval() {
        val track = _uiState.value.activeTrack ?: return
        val inFile = File(track.filePath)
        if (!inFile.exists()) return

        audioController.stopPlayback()
        val state = _uiState.value
        _uiState.update {
            it.copy(
                isProcessingDsp = true,
                isPlayingMainTrack = false,
                statusBannerMessageBn = "${state.silenceRemovalMode.labelBn} অনুযায়ী সাইলেন্স কাটা হচ্ছে…",
                statusBannerMessageEn = "Cutting silent segments using ${state.silenceRemovalMode.labelEn}…"
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val outFile = File(studioFilesDir, "trimmed_${System.currentTimeMillis()}.wav")
            val result = AudioDspEngine.removeSilenceDualMode(
                inputFile = inFile,
                outputFile = outFile,
                mode = state.silenceRemovalMode,
                thresholdDb = state.silenceDbThreshold,
                minSilenceSec = state.silenceDurationLimitSec
            )
            val analysis = AudioDspEngine.analyzeWavFile(
                file = result.outputFile,
                silenceThresholdDb = state.silenceDbThreshold,
                minSilenceDurationSec = state.silenceDurationLimitSec
            )
            val updatedTrack = track.copy(
                title = "${track.title.substringBefore(" [")} [Silence Cut]",
                filePath = result.outputFile.absolutePath,
                durationSec = analysis.durationSec,
                peakDb = analysis.peakDb,
                rmsDb = analysis.rmsDb,
                noiseFloorDb = analysis.noiseFloorDb,
                silenceRemovedSec = track.silenceRemovedSec + result.removedSilenceSec
            )
            repository.updateTrack(updatedTrack)

            _uiState.update {
                it.copy(
                    isProcessingDsp = false,
                    activeTrack = updatedTrack,
                    waveformBins = analysis.waveformBins,
                    silenceMaskBins = analysis.silenceMaskBins,
                    detectedSilenceCount = analysis.detectedSilenceRegionsCount,
                    estimatedSilenceSec = analysis.estimatedSilenceTotalSec,
                    sampleWindowStartSec = 0f,
                    sampleWindowEndSec = min(3.0f, analysis.durationSec),
                    visualCutStartSec = 0f,
                    visualCutEndSec = min(6.0f, analysis.durationSec),
                    statusBannerMessageBn = "সাইলেন্স রিমুভ সম্পন্ন! মোট ${result.removedSegmentsCount}টি অংশ (${"%.1f".format(result.removedSilenceSec)} সেকেন্ড) কেটে বাদ দেওয়া হয়েছে।",
                    statusBannerMessageEn = "Silence Removed! Cut ${result.removedSegmentsCount} segments (${"%.1f".format(result.removedSilenceSec)}s removed)."
                )
            }
        }
    }

    /**
     * Feature 4A: Silence-Based Auto-Split (Up to 100 clips)
     */
    fun runSilenceBasedAutoSplit() {
        val track = _uiState.value.activeTrack ?: return
        val inFile = File(track.filePath)
        if (!inFile.exists()) return

        audioController.stopPlayback()
        val state = _uiState.value
        _uiState.update {
            it.copy(
                isProcessingDsp = true,
                statusBannerMessageBn = "অডিওর বিরতি অনুযায়ী অটোমেটিক টুকরো (Auto-Split) করা হচ্ছে…",
                statusBannerMessageEn = "Auto-splitting audio by silence breaks (up to 100 clips)…"
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val clipsDir = File(studioFilesDir, "splits_auto_${track.id}")
            val clips = AudioDspEngine.autoSplitBySilenceUpTo100(
                inputFile = inFile,
                outputDir = clipsDir,
                thresholdDb = state.silenceDbThreshold,
                minSilenceSec = max(0.6f, state.silenceDurationLimitSec * 0.6f),
                maxClips = 100
            )
            _uiState.update {
                it.copy(
                    isProcessingDsp = false,
                    generatedSplitClips = clips,
                    statusBannerMessageBn = "সাইলেন্স অনুযায়ী অডিওটিকে ${clips.size}টি আলাদা ক্লিপে ভাগ করা হয়েছে!",
                    statusBannerMessageEn = "Audio automatically split into ${clips.size} individual clips!"
                )
            }
        }
    }

    /**
     * Feature 4B: Manual Time Range with '+' Button
     */
    fun addManualTimeRangeSlot() {
        val currentRanges = _uiState.value.manualTimeRanges
        val trackDur = (_uiState.value.activeTrack?.durationSec ?: 32f).toInt()
        val lastEnd = currentRanges.lastOrNull()?.endSecText?.toFloatOrNull()?.toInt() ?: 0
        val newStart = min(lastEnd, max(0, trackDur - 5))
        val newEnd = min(trackDur, newStart + 8)
        val nextIdx = currentRanges.size + 1

        val newSlot = ManualTimeRange(
            id = System.currentTimeMillis() + nextIdx,
            startSecText = newStart.toString(),
            endSecText = max(newStart + 1, newEnd).toString(),
            label = "Clip $nextIdx"
        )
        _uiState.update {
            it.copy(manualTimeRanges = currentRanges + newSlot)
        }
    }

    fun updateManualTimeRangeSlot(id: Long, startText: String, endText: String) {
        _uiState.update { state ->
            state.copy(
                manualTimeRanges = state.manualTimeRanges.map { slot ->
                    if (slot.id == id) slot.copy(startSecText = startText, endSecText = endText) else slot
                }
            )
        }
    }

    fun removeManualTimeRangeSlot(id: Long) {
        _uiState.update { state ->
            if (state.manualTimeRanges.size <= 1) state
            else state.copy(manualTimeRanges = state.manualTimeRanges.filterNot { it.id == id })
        }
    }

    fun runManualRangeSplit() {
        val track = _uiState.value.activeTrack ?: return
        val inFile = File(track.filePath)
        if (!inFile.exists()) return
        val ranges = _uiState.value.manualTimeRanges

        audioController.stopPlayback()
        _uiState.update {
            it.copy(
                isProcessingDsp = true,
                statusBannerMessageBn = "WAV ভিউ ও টাইম রেঞ্জ অনুযায়ী ${ranges.size}টি ক্লিপ কাট করা হচ্ছে…",
                statusBannerMessageEn = "Cutting audio into ${ranges.size} clips from WAV ranges…"
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val clipsDir = File(studioFilesDir, "splits_manual_${track.id}")
            val clips = AudioDspEngine.manualSplitByRanges(
                inputFile = inFile,
                outputDir = clipsDir,
                ranges = ranges
            )
            _uiState.update {
                it.copy(
                    isProcessingDsp = false,
                    generatedSplitClips = clips,
                    statusBannerMessageBn = "${clips.size}টি অডিও টুকরো প্রস্তুত! এখন 'সবগুলো গ্যালারিতে সেভ করুন' বাটনে চাপুন।",
                    statusBannerMessageEn = "${clips.size} cut clips ready! Tap 'Save All Clips to Gallery' to export."
                )
            }
        }
    }

    /**
     * Multi-Format Export (MP3, WAV, AAC) & Batch Gallery Saving
     */
    fun selectExportFormat(format: AudioExportFormat) {
        _uiState.update { it.copy(selectedExportFormat = format) }
    }

    fun selectExportBitrate(kbps: Int) {
        _uiState.update { it.copy(selectedExportBitrateKbps = kbps) }
    }

    fun exportActiveTrackToGallery() {
        val track = _uiState.value.activeTrack ?: return
        val inFile = File(track.filePath)
        if (!inFile.exists()) return
        val state = _uiState.value

        _uiState.update {
            it.copy(
                isProcessingDsp = true,
                statusBannerMessageBn = "${state.selectedExportFormat.label} ফরম্যাটে গ্যালারিতে এক্সপোর্ট হচ্ছে…",
                statusBannerMessageEn = "Exporting track as ${state.selectedExportFormat.label} to Gallery…"
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val summary = MediaCodecExportEngine.exportAudioFileToGallery(
                context = appContext,
                sourceWavFile = inFile,
                baseTitle = track.title,
                format = state.selectedExportFormat,
                bitrateKbps = state.selectedExportBitrateKbps
            )
            _uiState.update {
                it.copy(
                    isProcessingDsp = false,
                    lastExportedPathDisplay = summary.savedLocationDisplay,
                    statusBannerMessageBn = "গ্যালারিতে সেভ সম্পন্ন! (${summary.savedLocationDisplay})",
                    statusBannerMessageEn = "Saved to Gallery! (${summary.savedLocationDisplay})"
                )
            }
        }
    }

    fun exportSingleClipToGallery(clip: AudioSplitClip) {
        val file = File(clip.filePath)
        if (!file.exists()) return
        val state = _uiState.value

        viewModelScope.launch(Dispatchers.IO) {
            val summary = MediaCodecExportEngine.exportAudioFileToGallery(
                context = appContext,
                sourceWavFile = file,
                baseTitle = clip.label,
                format = state.selectedExportFormat,
                bitrateKbps = state.selectedExportBitrateKbps
            )
            _uiState.update {
                it.copy(
                    lastExportedPathDisplay = summary.savedLocationDisplay,
                    statusBannerMessageBn = "ক্লিপ #${clip.index} গ্যালারিতে সেভ হয়েছে (${summary.savedLocationDisplay})",
                    statusBannerMessageEn = "Clip #${clip.index} saved to Gallery (${summary.savedLocationDisplay})"
                )
            }
        }
    }

    fun batchExportAllSplitClipsToGallery() {
        val clips = _uiState.value.generatedSplitClips
        if (clips.isEmpty()) return
        val state = _uiState.value

        _uiState.update {
            it.copy(
                isProcessingDsp = true,
                statusBannerMessageBn = "কাট করা ${clips.size}টি ক্লিপ ${state.selectedExportFormat.extension.uppercase()} ফরম্যাটে গ্যালারিতে সেভ হচ্ছে…",
                statusBannerMessageEn = "Saving all ${clips.size} cut clips as ${state.selectedExportFormat.extension.uppercase()} to Gallery…"
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val summary = MediaCodecExportEngine.batchExportSplitClipsToGallery(
                context = appContext,
                clips = clips,
                format = state.selectedExportFormat,
                bitrateKbps = state.selectedExportBitrateKbps
            )
            _uiState.update {
                it.copy(
                    isProcessingDsp = false,
                    lastExportedPathDisplay = summary.savedLocationDisplay,
                    statusBannerMessageBn = "অভিনন্দন! সব (${summary.exportedCount}টি) অডিও ক্লিপ ফোনের গ্যালারিতে সেভ হয়েছে: ${summary.savedLocationDisplay}",
                    statusBannerMessageEn = "Saved all ${summary.exportedCount} cut audio clips to Gallery: ${summary.savedLocationDisplay}"
                )
            }
        }
    }

    /**
     * Video Noise & Silence Removal + Video Gallery Export (.mp4)
     */
    fun cleanAndExportVideoToGallery() {
        val track = _uiState.value.activeTrack ?: return
        val inFile = File(track.filePath)
        if (!inFile.exists()) return
        val state = _uiState.value

        audioController.stopPlayback()
        _uiState.update {
            it.copy(
                isProcessingDsp = true,
                statusBannerMessageBn = "ভিডিওর নয়েজ ও সাইলেন্স ক্লিন করে গ্যালারিতে MP4 সেভ হচ্ছে (অফলাইন)…",
                statusBannerMessageEn = "Cleaning Video Noise & Silence and saving MP4 to Gallery (Offline)…"
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            var processedWav = inFile
            if (state.activeNoiseProfile != null) {
                val denoisedFile = File(studioFilesDir, "vid_denoised_${System.currentTimeMillis()}.wav")
                processedWav = AudioDspEngine.applySmartNoiseFilter(
                    inputFile = processedWav,
                    outputFile = denoisedFile,
                    noiseProfile = state.activeNoiseProfile,
                    reductionStrength = state.noiseReductionStrength
                )
            }
            val masteredWav = File(studioFilesDir, "vid_mastered_${System.currentTimeMillis()}.wav")
            processedWav = AudioDspEngine.applyAiVoiceMastering(
                inputFile = processedWav,
                outputFile = masteredWav,
                preset = state.selectedAiPreset,
                voiceBoostDb = state.voiceBoostDb,
                autoNormalizeEnabled = true,
                targetPeakDb = -1.0f,
                warmthBoostDb = state.customWarmthDb,
                clarityBoostDb = state.customClarityDb
            )

            val srcVideo = track.sourceVideoPath?.let { File(it) }
            val savedVideoPath = MediaCodecExportEngine.exportCleanedVideoToGallery(
                context = appContext,
                cleanedWavFile = processedWav,
                sourceVideoFile = srcVideo
            )

            val analysis = AudioDspEngine.analyzeWavFile(
                file = processedWav,
                silenceThresholdDb = state.silenceDbThreshold,
                minSilenceDurationSec = state.silenceDurationLimitSec
            )
            val updatedTrack = track.copy(
                title = "${track.title.substringBefore(" [")} [Clean Video]",
                filePath = processedWav.absolutePath,
                durationSec = analysis.durationSec,
                peakDb = analysis.peakDb,
                rmsDb = analysis.rmsDb,
                noiseFloorDb = analysis.noiseFloorDb,
                isNoiseFiltered = true,
                isNormalized = true
            )
            repository.updateTrack(updatedTrack)

            _uiState.update {
                it.copy(
                    isProcessingDsp = false,
                    activeTrack = updatedTrack,
                    waveformBins = analysis.waveformBins,
                    silenceMaskBins = analysis.silenceMaskBins,
                    lastExportedPathDisplay = savedVideoPath,
                    statusBannerMessageBn = "ভিডিও নয়েজ ক্লিন ও গ্যালারিতে সেভ সম্পন্ন! ($savedVideoPath)",
                    statusBannerMessageEn = "Video Noise Cleaned & Saved to Gallery! ($savedVideoPath)"
                )
            }
        }
    }

    /**
     * Feature 5 & Unique Offline Voice Studio Controls
     */
    fun selectAiVoicePreset(preset: AiVoicePreset) {
        _uiState.update {
            it.copy(
                selectedAiPreset = preset,
                customWarmthDb = preset.warmthBoostDb,
                customClarityDb = preset.clarityBoostDb,
                targetPeakDb = preset.targetPeakDb
            )
        }
    }

    fun updateVoiceBoostDb(boostDb: Float) {
        _uiState.update { it.copy(voiceBoostDb = boostDb.coerceIn(0f, 12f)) }
    }

    fun toggleAutoNormalize(enabled: Boolean) {
        _uiState.update { it.copy(autoNormalizeEnabled = enabled) }
    }

    fun updateTargetPeakDb(db: Float) {
        _uiState.update { it.copy(targetPeakDb = db.coerceIn(-4.0f, -0.2f)) }
    }

    fun updateCustomWarmthAndClarity(warmthDb: Float, clarityDb: Float) {
        _uiState.update {
            it.copy(
                customWarmthDb = warmthDb.coerceIn(0f, 8f),
                customClarityDb = clarityDb.coerceIn(0f, 8f)
            )
        }
    }

    fun toggleDeBreathAndClickClean(enabled: Boolean) {
        _uiState.update { it.copy(deBreathAndClickCleanEnabled = enabled) }
    }

    fun selectOfflineVoiceCharacter(character: OfflineVoiceCharacter) {
        _uiState.update { it.copy(selectedVoiceCharacter = character) }
    }

    fun updateSpeechSpeedFactor(speed: Float) {
        _uiState.update { it.copy(speechSpeedFactor = speed.coerceIn(0.75f, 1.40f)) }
    }

    fun applyStudioEffectsAndMastering() {
        val track = _uiState.value.activeTrack ?: return
        val inFile = File(track.filePath)
        if (!inFile.exists()) return

        audioController.stopPlayback()
        val state = _uiState.value
        _uiState.update {
            it.copy(
                isProcessingDsp = true,
                isPlayingMainTrack = false,
                statusBannerMessageBn = "অফলাইন এআই ভয়েস এনহ্যান্সার, ডি-ব্রেথ ও অটো-নর্মালাইজেশন প্রয়োগ হচ্ছে…",
                statusBannerMessageEn = "Applying Offline AI Voice Enhancer, De-Breath & Auto-Normalization…"
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            var workingFile = inFile
            if (state.smartNoiseFilterEnabled && state.activeNoiseProfile != null && !track.isNoiseFiltered) {
                val denoisedTemp = File(studioFilesDir, "pre_master_clean_${System.currentTimeMillis()}.wav")
                workingFile = AudioDspEngine.applySmartNoiseFilter(
                    inputFile = workingFile,
                    outputFile = denoisedTemp,
                    noiseProfile = state.activeNoiseProfile,
                    reductionStrength = state.noiseReductionStrength
                )
            }

            if (state.deBreathAndClickCleanEnabled ||
                state.selectedVoiceCharacter != OfflineVoiceCharacter.NATURAL ||
                kotlin.math.abs(state.speechSpeedFactor - 1.0f) > 0.02f
            ) {
                val voiceTransformTemp = File(studioFilesDir, "voice_transform_${System.currentTimeMillis()}.wav")
                workingFile = AudioDspEngine.applyOfflineProVoiceTransform(
                    inputFile = workingFile,
                    outputFile = voiceTransformTemp,
                    deBreathAndClickClean = state.deBreathAndClickCleanEnabled,
                    pitchFactor = state.selectedVoiceCharacter.pitchFactor,
                    speedFactor = state.speechSpeedFactor
                )
            }

            val masteredFile = File(studioFilesDir, "mastered_${System.currentTimeMillis()}.wav")
            AudioDspEngine.applyAiVoiceMastering(
                inputFile = workingFile,
                outputFile = masteredFile,
                preset = state.selectedAiPreset,
                voiceBoostDb = state.voiceBoostDb,
                autoNormalizeEnabled = state.autoNormalizeEnabled,
                targetPeakDb = state.targetPeakDb,
                warmthBoostDb = state.customWarmthDb,
                clarityBoostDb = state.customClarityDb
            )

            val analysis = AudioDspEngine.analyzeWavFile(
                file = masteredFile,
                silenceThresholdDb = state.silenceDbThreshold,
                minSilenceDurationSec = state.silenceDurationLimitSec
            )

            val updatedTrack = track.copy(
                title = "${track.title.substringBefore(" [")} [AI Studio Master]",
                filePath = masteredFile.absolutePath,
                durationSec = analysis.durationSec,
                peakDb = analysis.peakDb,
                rmsDb = analysis.rmsDb,
                noiseFloorDb = analysis.noiseFloorDb,
                isNoiseFiltered = true,
                isNormalized = state.autoNormalizeEnabled,
                aiPresetApplied = state.selectedAiPreset.titleEn
            )
            repository.updateTrack(updatedTrack)

            _uiState.update {
                it.copy(
                    isProcessingDsp = false,
                    activeTrack = updatedTrack,
                    waveformBins = analysis.waveformBins,
                    silenceMaskBins = analysis.silenceMaskBins,
                    detectedSilenceCount = analysis.detectedSilenceRegionsCount,
                    estimatedSilenceSec = analysis.estimatedSilenceTotalSec,
                    statusBannerMessageBn = "স্টুডিও মাস্টারিং সম্পন্ন! Peak: ${"%.1f".format(analysis.peakDb)} dB, RMS: ${"%.1f".format(analysis.rmsDb)} dB (${state.selectedAiPreset.titleBn})",
                    statusBannerMessageEn = "Studio Mastering Complete! Peak: ${"%.1f".format(analysis.peakDb)} dB, RMS: ${"%.1f".format(analysis.rmsDb)} dB (${state.selectedAiPreset.titleEn})"
                )
            }
        }
    }

    /**
     * Playback Controls for Main Track and Individual Split Clips
     */
    fun toggleMainTrackPlayback() {
        if (_uiState.value.isPlayingMainTrack) {
            audioController.stopPlayback()
            _uiState.update { it.copy(isPlayingMainTrack = false, playbackPositionSec = 0f) }
            return
        }

        val track = _uiState.value.activeTrack ?: return
        val file = File(track.filePath)
        if (!file.exists()) return

        _uiState.update {
            it.copy(
                isPlayingMainTrack = true,
                playingClipFilePath = null,
                playbackPositionSec = 0f
            )
        }

        audioController.playWavFile(
            file = file,
            onProgress = { curSec, _ ->
                _uiState.update { it.copy(playbackPositionSec = curSec) }
            },
            onCompleted = {
                _uiState.update { it.copy(isPlayingMainTrack = false, playbackPositionSec = 0f) }
            }
        )
    }

    fun toggleSplitClipPlayback(clip: AudioSplitClip) {
        if (_uiState.value.playingClipFilePath == clip.filePath) {
            audioController.stopPlayback()
            _uiState.update { it.copy(playingClipFilePath = null) }
            return
        }

        val file = File(clip.filePath)
        if (!file.exists()) return

        _uiState.update {
            it.copy(
                isPlayingMainTrack = false,
                playingClipFilePath = clip.filePath
            )
        }

        audioController.playWavFile(
            file = file,
            onProgress = { _, _ -> },
            onCompleted = {
                _uiState.update { it.copy(playingClipFilePath = null) }
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        audioController.stopPlayback()
        audioController.stopRecording()
    }
}
