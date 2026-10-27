package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AudioTrackEntity
import com.example.data.tr
import com.example.ui.StudioUiState
import com.example.ui.components.RealTimeRecordingWaveformCanvas
import com.example.ui.components.SpectralNoisePrintVisualizer
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CrimsonRecord
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAi

@Composable
fun RecorderScreen(
    uiState: StudioUiState,
    savedTracks: List<AudioTrackEntity>,
    onSetNoiseCaptureDuration: (Int) -> Unit,
    onCaptureNoisePrint: () -> Unit,
    onToggleSmartNoiseFilter: (Boolean) -> Unit,
    onNoiseStrengthChange: (Float) -> Unit,
    onToggleSilenceSkip: (Boolean) -> Unit,
    onSilenceDbChange: (Float) -> Unit,
    onSilenceLimitSecChange: (Float) -> Unit,
    onToggleRecord: () -> Unit,
    onImportAudioFromGallery: () -> Unit,
    onImportVideoFromGallery: () -> Unit,
    onGenerateVideoDemo: () -> Unit,
    onSelectTrack: (AudioTrackEntity) -> Unit
) {
    val lang = uiState.language

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            SmartRecordHeroConsole(
                uiState = uiState,
                onToggleRecord = onToggleRecord
            )
        }

        // Gallery Audio & Video Offline Import Hub Card
        item {
            GalleryMediaImportHubCard(
                uiState = uiState,
                onImportAudio = onImportAudioFromGallery,
                onImportVideo = onImportVideoFromGallery,
                onGenerateVideoDemo = onGenerateVideoDemo
            )
        }

        // Feature 1A & 1B: Audacity-style Noise Profile Learning (3-5s) & Smart Noise Filter
        item {
            AudacityNoiseProfileCard(
                uiState = uiState,
                onSetNoiseCaptureDuration = onSetNoiseCaptureDuration,
                onCaptureNoisePrint = onCaptureNoisePrint,
                onToggleSmartNoiseFilter = onToggleSmartNoiseFilter,
                onNoiseStrengthChange = onNoiseStrengthChange
            )
        }

        // Feature 1C: Silence Skip Recording (dB + Seconds Limit)
        item {
            SilenceSkipSettingsCard(
                uiState = uiState,
                onToggleSilenceSkip = onToggleSilenceSkip,
                onSilenceDbChange = onSilenceDbChange,
                onSilenceLimitSecChange = onSilenceLimitSecChange
            )
        }

        // Saved / Recent Studio Tracks list
        if (savedTracks.isNotEmpty()) {
            item {
                Text(
                    text = tr(
                        lang,
                        "রেকর্ড করা ও ইনপুটকৃত অডিও/ভিডিও ট্র্যাকসমূহ (${savedTracks.size})",
                        "Recorded & Imported Audio/Video Tracks (${savedTracks.size})"
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(savedTracks, key = { it.id }) { track ->
                val isSelected = uiState.activeTrack?.id == track.id
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectTrack(track) }
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) ElectricCyan else BorderSubtle,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .testTag("track_item_${track.id}"),
                    color = if (isSelected) StudioSurfaceElevated else StudioSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSelected) ElectricCyan else TextPrimary
                            )
                            Text(
                                text = tr(
                                    lang,
                                    "দৈর্ঘ্য: ${"%.1f".format(track.durationSec)}s • Peak: ${"%.1f".format(track.peakDb)} dB • সাইলেন্স বাদ: ${"%.1f".format(track.silenceRemovedSec)}s",
                                    "Duration: ${"%.1f".format(track.durationSec)}s • Peak: ${"%.1f".format(track.peakDb)} dB • Silence Cut: ${"%.1f".format(track.silenceRemovedSec)}s"
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                        if (track.isNoiseFiltered) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldNeon.copy(alpha = 0.16f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "100% Clean",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldNeon
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun GalleryMediaImportHubCard(
    uiState: StudioUiState,
    onImportAudio: () -> Unit,
    onImportVideo: () -> Unit,
    onGenerateVideoDemo: () -> Unit
) {
    val lang = uiState.language

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = StudioSurface,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.FolderOpen,
                    contentDescription = null,
                    tint = ElectricCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = tr(
                            lang,
                            "গ্যালারি থেকে অডিও বা ভিডিও ইনপুট করুন (১০০% অফলাইন)",
                            "Import Audio or Video from Gallery (100% Offline)"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = tr(
                            lang,
                            "ফোনের যেকোনো অডিও (MP3/WAV/AAC) বা ভিডিও ইনপুট করে নয়েজ/সাইলেন্স কাটুন",
                            "Import phone audio or video files to remove noise, cut silence & split"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onImportAudio,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = ObsidianBg
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("gallery_import_audio_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AudioFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr(lang, "অডিও ইনপুট", "Import Audio"),
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onImportVideo,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VioletAi,
                        contentColor = ObsidianBg
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("gallery_import_video_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.VideoFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr(lang, "ভিডিও ইনপুট", "Import Video"),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            OutlinedButton(
                onClick = onGenerateVideoDemo,
                border = ButtonDefaults.outlinedButtonBorder(true).copy(
                    brush = SolidColor(AmberGold.copy(alpha = 0.7f))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("load_demo_video_track_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.VideoFile,
                    contentDescription = null,
                    tint = AmberGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tr(
                        lang,
                        "টেস্টের জন্য স্যাম্পল ভিডিও + নয়েজ ট্র্যাক লোড করুন",
                        "Load Sample Video + Fan Noise Track for Testing"
                    ),
                    color = AmberGold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun SmartRecordHeroConsole(
    uiState: StudioUiState,
    onToggleRecord: () -> Unit
) {
    val lang = uiState.language
    val buttonColor by animateColorAsState(
        targetValue = if (uiState.isRecording) CrimsonRecord else ElectricCyan,
        label = "rec_btn_color"
    )
    val currentDb = if (uiState.isRecording) {
        uiState.liveTelemetry.currentDb
    } else {
        uiState.activeTrack?.rmsDb ?: -42f
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = StudioSurface,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(StudioSurfaceElevated, StudioSurface)
                    )
                )
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = ElectricCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr(lang, "স্মার্ট স্টুডিও রেকর্ডার", "Smart Studio Recorder"),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                }

                val pillColor = when {
                    uiState.isRecording && uiState.liveTelemetry.isCurrentlySkippingSilence -> AmberGold
                    uiState.isRecording -> CrimsonRecord
                    else -> EmeraldNeon
                }
                val pillText = when {
                    uiState.isRecording && uiState.liveTelemetry.isCurrentlySkippingSilence ->
                        tr(lang, "AUTO-PAUSED (সাইলেন্স স্কিপ)", "AUTO-PAUSED (SILENCE SKIP)")
                    uiState.isRecording -> tr(lang, "লাইভ রেকর্ডিং চলছে", "LIVE RECORDING")
                    else -> "100% OFFLINE • READY"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(pillColor.copy(alpha = 0.16f))
                        .border(1.dp, pillColor.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = pillText,
                        style = MaterialTheme.typography.labelSmall,
                        color = pillColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            RealTimeRecordingWaveformCanvas(
                isRecording = uiState.isRecording,
                isSkippingSilence = uiState.liveTelemetry.isCurrentlySkippingSilence,
                liveAmps = uiState.liveTelemetry.liveWaveformAmps,
                currentDb = currentDb,
                silenceDbThreshold = uiState.silenceDbThreshold,
                language = lang
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tr(lang, "রেকর্ড সময়", "Recorded Time"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "${"%.1f".format(if (uiState.isRecording) uiState.liveTelemetry.elapsedRecordedSec else (uiState.activeTrack?.durationSec ?: 0f))}s",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ElectricCyan
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(buttonColor.copy(alpha = 0.2f))
                        .border(3.dp, buttonColor, CircleShape)
                        .clickable { onToggleRecord() }
                        .testTag("main_record_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(buttonColor)
                    ) {
                        Icon(
                            imageVector = if (uiState.isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = if (uiState.isRecording) "Stop recording" else "Start smart recording",
                            tint = ObsidianBg,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tr(lang, "স্কিপড সাইলেন্স", "Skipped Silence"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    val skipped = if (uiState.isRecording) {
                        uiState.liveTelemetry.skippedSilenceSec
                    } else {
                        uiState.activeTrack?.silenceRemovedSec ?: 0f
                    }
                    Text(
                        text = "${"%.1f".format(skipped)}s",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AmberGold
                    )
                }
            }

            val normLevel = ((currentDb + 60f) / 60f).coerceIn(0.02f, 1f)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = tr(
                            lang,
                            "ইনপুট সিগন্যাল লেভেল: ${"%.1f".format(currentDb)} dB",
                            "Input Signal Level: ${"%.1f".format(currentDb)} dB"
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = tr(
                            lang,
                            "থ্রেশহোল্ড: ${uiState.silenceDbThreshold.toInt()} dB",
                            "Threshold: ${uiState.silenceDbThreshold.toInt()} dB"
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = AmberGold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { normLevel },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (currentDb <= uiState.silenceDbThreshold) AmberGold else EmeraldNeon,
                    trackColor = ObsidianBg
                )
            }
        }
    }
}

@Composable
private fun AudacityNoiseProfileCard(
    uiState: StudioUiState,
    onSetNoiseCaptureDuration: (Int) -> Unit,
    onCaptureNoisePrint: () -> Unit,
    onToggleSmartNoiseFilter: (Boolean) -> Unit,
    onNoiseStrengthChange: (Float) -> Unit
) {
    val lang = uiState.language

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = StudioSurface,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Fingerprint,
                    contentDescription = null,
                    tint = VioletAi
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = tr(
                            lang,
                            "১. অডাছিটি স্টাইল নয়েজ প্রোফাইল লার্নিং",
                            "1. Audacity-Style Noise Profile Learning"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = tr(
                            lang,
                            "৩-৫ সেকেন্ড ফ্যান/এসি শব্দের নয়েজ প্রিন্ট ক্যাপচার ও স্মার্ট ফিল্টার",
                            "Capture 3–5s ambient fan/AC Noise Print & filter with 100% vocal accuracy"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr(lang, "স্যাম্পল সময়:", "Sample Time:"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                listOf(3, 4, 5).forEach { sec ->
                    val selected = uiState.noiseCaptureTargetSec == sec
                    FilterChip(
                        selected = selected,
                        onClick = { onSetNoiseCaptureDuration(sec) },
                        label = { Text(tr(lang, "$sec সেকেন্ড", "${sec}s")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VioletAi.copy(alpha = 0.22f),
                            selectedLabelColor = VioletAi
                        ),
                        modifier = Modifier.testTag("noise_duration_${sec}s_chip")
                    )
                }
            }

            SpectralNoisePrintVisualizer(
                spectralBandsCsv = uiState.activeNoiseProfile?.spectralBandsCsv ?: "",
                isCapturing = uiState.isCapturingNoisePrint,
                captureProgress = uiState.noiseCaptureProgress
            )

            uiState.activeNoiseProfile?.let { profile ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(
                                lang,
                                "সক্রিয় প্রিন্ট: ${profile.name}",
                                "Active Print: ${profile.name}"
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                        Text(
                            text = "Noise Floor: ${"%.1f".format(profile.noiseFloorDb)} dB • Low Hum: ${(profile.lowBandEnergy * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "100% Accuracy",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldNeon
                        )
                    }
                }
            }

            Button(
                onClick = onCaptureNoisePrint,
                enabled = !uiState.isCapturingNoisePrint,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VioletAi,
                    contentColor = ObsidianBg
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("capture_noise_print_button")
            ) {
                Icon(imageVector = Icons.Filled.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.isCapturingNoisePrint) {
                        tr(
                            lang,
                            "নয়েজ প্রিন্ট শেখা হচ্ছে (${(uiState.noiseCaptureProgress * 100).toInt()}%)…",
                            "Learning Noise Print (${(uiState.noiseCaptureProgress * 100).toInt()}%)…"
                        )
                    } else {
                        tr(
                            lang,
                            "নয়েজ প্রিন্ট ক্যাপচার করুন (${uiState.noiseCaptureTargetSec} সেকেন্ড)",
                            "Capture Noise Print (${uiState.noiseCaptureTargetSec} Seconds)"
                        )
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(
                            lang,
                            "স্মার্ট নয়েজ ফিল্টার (Auto Clean)",
                            "Smart Noise Filter (Auto Clean)"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = tr(
                            lang,
                            "কণ্ঠের একুরেসি ১০০% অক্ষুণ্ণ রেখে শুধু ব্যাকগ্রাউন্ড নয়েজ মুছবে",
                            "Removes background fan/AC noise while keeping 100% voice accuracy"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = uiState.smartNoiseFilterEnabled,
                    onCheckedChange = onToggleSmartNoiseFilter,
                    colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBg, checkedTrackColor = EmeraldNeon),
                    modifier = Modifier.testTag("smart_noise_filter_switch")
                )
            }

            if (uiState.smartNoiseFilterEnabled) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tr(lang, "নয়েজ রিডাকশন পাওয়ার", "Noise Reduction Strength"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "${(uiState.noiseReductionStrength * 100).toInt()}% (Voice Safe)",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldNeon
                        )
                    }
                    Slider(
                        value = uiState.noiseReductionStrength,
                        onValueChange = onNoiseStrengthChange,
                        valueRange = 0.30f..0.98f,
                        colors = SliderDefaults.colors(thumbColor = EmeraldNeon, activeTrackColor = EmeraldNeon),
                        modifier = Modifier.testTag("noise_strength_slider")
                    )
                }
            }
        }
    }
}

@Composable
private fun SilenceSkipSettingsCard(
    uiState: StudioUiState,
    onToggleSilenceSkip: (Boolean) -> Unit,
    onSilenceDbChange: (Float) -> Unit,
    onSilenceLimitSecChange: (Float) -> Unit
) {
    val lang = uiState.language

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = StudioSurface,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PauseCircle,
                        contentDescription = null,
                        tint = AmberGold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = tr(
                                lang,
                                "সাইলেন্স স্কিপ রেকর্ডিং (Silence Skip)",
                                "Silence Skip Recording (Auto-Pause)"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = tr(
                                lang,
                                "নির্দিষ্ট সেকেন্ড ও dB-এর বেশি চুপ থাকলে অটো-পজ/স্কিপ হবে",
                                "Automatically pauses recording when quiet longer than selected seconds & dB"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                Switch(
                    checked = uiState.silenceSkipEnabled,
                    onCheckedChange = onToggleSilenceSkip,
                    colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBg, checkedTrackColor = AmberGold),
                    modifier = Modifier.testTag("silence_skip_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr(lang, "কুইক লিমিট:", "Quick Presets:"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                listOf(1.0f, 2.0f, 3.0f).forEach { sec ->
                    val isSelected = kotlin.math.abs(uiState.silenceDurationLimitSec - sec) < 0.15f
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSilenceLimitSecChange(sec) },
                        label = { Text(tr(lang, "${sec.toInt()} সেকেন্ড", "${sec.toInt()}s")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberGold.copy(alpha = 0.22f),
                            selectedLabelColor = AmberGold
                        ),
                        modifier = Modifier.testTag("silence_preset_${sec.toInt()}s_chip")
                    )
                }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = tr(
                            lang,
                            "সাইলেন্ট সময়সীমা (Seconds Limit)",
                            "Silence Duration Limit (Seconds)"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = tr(
                            lang,
                            "${"%.1f".format(uiState.silenceDurationLimitSec)} সেকেন্ড",
                            "${"%.1f".format(uiState.silenceDurationLimitSec)} sec"
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = AmberGold
                    )
                }
                Slider(
                    value = uiState.silenceDurationLimitSec,
                    onValueChange = onSilenceLimitSecChange,
                    valueRange = 0.5f..5.0f,
                    colors = SliderDefaults.colors(thumbColor = AmberGold, activeTrackColor = AmberGold),
                    modifier = Modifier.testTag("silence_duration_slider")
                )
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = tr(
                            lang,
                            "সাইলেন্স ডেসিবেল (dB Threshold)",
                            "Silence Threshold (dB Limit)"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "${uiState.silenceDbThreshold.toInt()} dB",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan
                    )
                }
                Slider(
                    value = uiState.silenceDbThreshold,
                    onValueChange = onSilenceDbChange,
                    valueRange = -55f..-20f,
                    colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan),
                    modifier = Modifier.testTag("silence_db_slider")
                )
            }
        }
    }
}
