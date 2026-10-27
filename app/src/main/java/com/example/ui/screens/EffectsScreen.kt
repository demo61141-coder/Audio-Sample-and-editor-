package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AiVoicePreset
import com.example.data.AudioExportFormat
import com.example.data.OfflineVoiceCharacter
import com.example.data.tr
import com.example.ui.StudioUiState
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAi

@Composable
fun EffectsScreen(
    uiState: StudioUiState,
    onSelectAiPreset: (AiVoicePreset) -> Unit,
    onVoiceBoostChange: (Float) -> Unit,
    onToggleAutoNormalize: (Boolean) -> Unit,
    onTargetPeakChange: (Float) -> Unit,
    onWarmthAndClarityChange: (Float, Float) -> Unit,
    onToggleDeBreathAndClick: (Boolean) -> Unit,
    onSelectVoiceCharacter: (OfflineVoiceCharacter) -> Unit,
    onSpeechSpeedChange: (Float) -> Unit,
    onApplyMastering: () -> Unit,
    onSelectExportFormat: (AudioExportFormat) -> Unit,
    onSelectExportBitrate: (Int) -> Unit,
    onExportTrackToGallery: () -> Unit,
    onCleanAndExportVideoToGallery: () -> Unit
) {
    val lang = uiState.language
    val track = uiState.activeTrack

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Multi-Format Export (MP3 / WAV / AAC) & Offline Video Noise Cleaner Card
        item {
            Spacer(modifier = Modifier.height(4.dp))
            MultiFormatExportAndVideoStudioCard(
                uiState = uiState,
                onSelectExportFormat = onSelectExportFormat,
                onSelectExportBitrate = onSelectExportBitrate,
                onExportTrackToGallery = onExportTrackToGallery,
                onCleanAndExportVideoToGallery = onCleanAndExportVideoToGallery
            )
        }

        // 2. Telemetry Summary Card
        item {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Equalizer,
                            contentDescription = null,
                            tint = ElectricCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = tr(
                                    lang,
                                    "৫. এআই স্পিচ ক্লিয়ারিটি ও সাউন্ড এনহ্যান্সমেন্ট",
                                    "5. AI Speech Clarity & Sound Enhancement"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = tr(
                                    lang,
                                    "ভয়েস বুস্টার, অটো-নর্মালাইজেশন ও স্টুডিও পডকাস্ট ভয়েস মাস্টারিং",
                                    "Voice Booster, Auto-Normalization & Studio Podcast Mastering"
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
                        MetricTile(
                            label = "Peak Level",
                            value = "${"%.1f".format(track?.peakDb ?: -6.0f)} dB",
                            accent = ElectricCyan,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "RMS Loudness",
                            value = "${"%.1f".format(track?.rmsDb ?: -18.0f)} dB",
                            accent = EmeraldNeon,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Noise Floor",
                            value = "${"%.1f".format(track?.noiseFloorDb ?: -45.0f)} dB",
                            accent = VioletAi,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Unique 100% Offline Pro Features: Smart De-Breath/De-Click & Vocal Character / Speed Studio
        item {
            OfflineUniqueVoiceStudioCard(
                uiState = uiState,
                onToggleDeBreathAndClick = onToggleDeBreathAndClick,
                onSelectVoiceCharacter = onSelectVoiceCharacter,
                onSpeechSpeedChange = onSpeechSpeedChange
            )
        }

        // 4. Feature 5A: Voice Booster & Auto-Normalization Card
        item {
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VolumeUp,
                                contentDescription = null,
                                tint = EmeraldNeon
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = tr(
                                        lang,
                                        "ভয়েস বুস্টার ও অটো-নর্মালাইজেশন",
                                        "Voice Booster & Auto-Normalization"
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = tr(
                                        lang,
                                        "পুরো অডিওর ভলিউম এক লেভেলে রাখবে (কম/বেশি সাউন্ড ব্যালেন্স)",
                                        "Balances quiet and loud speech to a uniform studio volume"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = uiState.autoNormalizeEnabled,
                            onCheckedChange = onToggleAutoNormalize,
                            colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBg, checkedTrackColor = EmeraldNeon),
                            modifier = Modifier.testTag("auto_normalize_switch")
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
                                    "ভয়েস বুস্টার গেইন (Voice Booster):",
                                    "Voice Booster Gain:"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = "+${"%.1f".format(uiState.voiceBoostDb)} dB",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldNeon
                            )
                        }
                        Slider(
                            value = uiState.voiceBoostDb,
                            onValueChange = onVoiceBoostChange,
                            valueRange = 0f..12f,
                            colors = SliderDefaults.colors(thumbColor = EmeraldNeon, activeTrackColor = EmeraldNeon),
                            modifier = Modifier.testTag("voice_boost_slider")
                        )
                    }

                    if (uiState.autoNormalizeEnabled) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = tr(
                                        lang,
                                        "টার্গেট পিক সিলিং (Target Ceiling):",
                                        "Target Peak Ceiling (Anti-Clip):"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${"%.1f".format(uiState.targetPeakDb)} dB",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ElectricCyan
                                )
                            }
                            Slider(
                                value = uiState.targetPeakDb,
                                onValueChange = onTargetPeakChange,
                                valueRange = -3.5f..-0.2f,
                                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan),
                                modifier = Modifier.testTag("target_peak_slider")
                            )
                        }
                    }
                }
            }
        }

        // 5. Feature 5B: AI Studio Voice Enhancer (Podcast Crystal Clarity Presets & EQ)
        item {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = VioletAi
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = tr(
                                    lang,
                                    "এআই ভয়েস এনহ্যান্সার (Studio Podcast Clarity)",
                                    "AI Voice Enhancer (Studio Podcast Clarity)"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = tr(
                                    lang,
                                    "সাধারণ রেকর্ডিংকে স্টুডিও-কোয়ালিটি ক্রিস্টাল ক্লিয়ার ভয়েসে রূপান্তর",
                                    "Transform raw recordings into crystal-clear studio podcast voice"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    AiVoicePreset.entries.forEach { preset ->
                        val isSelected = uiState.selectedAiPreset == preset
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) StudioSurfaceElevated else ObsidianBg)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) VioletAi else BorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectAiPreset(preset) }
                                .padding(12.dp)
                                .testTag("ai_preset_${preset.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.title(lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isSelected) VioletAi else TextPrimary
                                )
                                Text(
                                    text = preset.desc(lang),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(VioletAi.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VioletAi
                                    )
                                }
                            }
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
                                    "ভয়েস বডি ও ওয়ার্মথ (180Hz Warmth):",
                                    "Vocal Body & Warmth (180Hz):"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = "+${"%.1f".format(uiState.customWarmthDb)} dB",
                                style = MaterialTheme.typography.labelLarge,
                                color = AmberGold
                            )
                        }
                        Slider(
                            value = uiState.customWarmthDb,
                            onValueChange = { onWarmthAndClarityChange(it, uiState.customClarityDb) },
                            valueRange = 0f..8f,
                            colors = SliderDefaults.colors(thumbColor = AmberGold, activeTrackColor = AmberGold),
                            modifier = Modifier.testTag("warmth_slider")
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
                                    "ক্রিস্টাল স্পিচ ক্লিয়ারিটি (3.2kHz Presence):",
                                    "Crystal Speech Clarity (3.2kHz Presence):"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = "+${"%.1f".format(uiState.customClarityDb)} dB",
                                style = MaterialTheme.typography.labelLarge,
                                color = VioletAi
                            )
                        }
                        Slider(
                            value = uiState.customClarityDb,
                            onValueChange = { onWarmthAndClarityChange(uiState.customWarmthDb, it) },
                            valueRange = 0f..8f,
                            colors = SliderDefaults.colors(thumbColor = VioletAi, activeTrackColor = VioletAi),
                            modifier = Modifier.testTag("clarity_slider")
                        )
                    }

                    Button(
                        onClick = onApplyMastering,
                        enabled = !uiState.isProcessingDsp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VioletAi,
                            contentColor = ObsidianBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("apply_studio_mastering_button")
                    ) {
                        Icon(imageVector = Icons.Filled.GraphicEq, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isProcessingDsp) {
                                tr(lang, "স্টুডিও মাস্টারিং প্রসেস হচ্ছে…", "Processing Studio Mastering…")
                            } else {
                                tr(
                                    lang,
                                    "স্টুডিও মাস্টারিং ও ভয়েস এনহ্যান্স প্রয়োগ করুন",
                                    "Apply Studio Mastering & Voice Enhancement"
                                )
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun MultiFormatExportAndVideoStudioCard(
    uiState: StudioUiState,
    onSelectExportFormat: (AudioExportFormat) -> Unit,
    onSelectExportBitrate: (Int) -> Unit,
    onExportTrackToGallery: () -> Unit,
    onCleanAndExportVideoToGallery: () -> Unit
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
                .border(1.5.dp, ElectricCyan.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    tint = ElectricCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = tr(
                            lang,
                            "মাল্টি-ফরম্যাট অডিও (MP3 / WAV / AAC) ও ভিডিও এক্সপোর্ট",
                            "Multi-Format Audio (MP3 / WAV / AAC) & Video Export"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = uiState.selectedExportFormat.description(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Format Selector Cards (MP3, WAV, AAC)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AudioExportFormat.entries.forEach { fmt ->
                    val selected = uiState.selectedExportFormat == fmt
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) ElectricCyan else ObsidianBg)
                            .border(
                                1.dp,
                                if (selected) ElectricCyan else BorderSubtle,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectExportFormat(fmt) }
                            .padding(vertical = 10.dp)
                            .testTag("export_format_${fmt.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = fmt.extension.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) ObsidianBg else TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bitrate Selector (for MP3 / AAC)
            if (uiState.selectedExportFormat != AudioExportFormat.WAV) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(lang, "কোয়ালিটি বিটরেট:", "Bitrate:"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    listOf(128, 192, 320).forEach { kbps ->
                        val selected = uiState.selectedExportBitrateKbps == kbps
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectExportBitrate(kbps) },
                            label = { Text("$kbps kbps") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                selectedLabelColor = ElectricCyan
                            ),
                            modifier = Modifier.testTag("bitrate_${kbps}_chip")
                        )
                    }
                }
            }

            Button(
                onClick = onExportTrackToGallery,
                enabled = !uiState.isProcessingDsp,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = ObsidianBg
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("export_main_track_button")
            ) {
                Icon(imageVector = Icons.Filled.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr(
                        lang,
                        "${uiState.selectedExportFormat.extension.uppercase()} ফরম্যাটে গ্যালারিতে সেভ করুন",
                        "Export Audio as ${uiState.selectedExportFormat.extension.uppercase()} to Gallery"
                    ),
                    fontWeight = FontWeight.Bold
                )
            }

            // Video Noise & Silence Cleaner Export Button
            OutlinedButton(
                onClick = onCleanAndExportVideoToGallery,
                enabled = !uiState.isProcessingDsp,
                border = ButtonDefaults.outlinedButtonBorder(true).copy(
                    brush = SolidColor(EmeraldNeon)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("clean_and_export_video_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.VideoFile,
                    contentDescription = null,
                    tint = EmeraldNeon,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr(
                        lang,
                        "ভিডিওর নয়েজ ক্লিন করে MP4 গ্যালারিতে সেভ করুন",
                        "Clean Video Noise & Save MP4 to Gallery"
                    ),
                    color = EmeraldNeon,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.lastExportedPathDisplay.isNotEmpty()) {
                Text(
                    text = tr(
                        lang,
                        "সর্বশেষ সেভকৃত ফাইল: ${uiState.lastExportedPathDisplay}",
                        "Last Saved File: ${uiState.lastExportedPathDisplay}"
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldNeon
                )
            }
        }
    }
}

@Composable
private fun OfflineUniqueVoiceStudioCard(
    uiState: StudioUiState,
    onToggleDeBreathAndClick: (Boolean) -> Unit,
    onSelectVoiceCharacter: (OfflineVoiceCharacter) -> Unit,
    onSpeechSpeedChange: (Float) -> Unit
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.RecordVoiceOver,
                    contentDescription = null,
                    tint = AmberGold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = tr(
                            lang,
                            "ইউনিক অফলাইন AI ফিচার: ডি-ব্রেথ ও ভয়েস ক্যারেক্টার স্টুডিও",
                            "Unique Offline AI Studio: Smart De-Breath & Voice Character"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = tr(
                            lang,
                            "ইন্টারনেট ছাড়াই নিঃশ্বাসের শব্দ/মাউথ-ক্লিক রিমুভ এবং কণ্ঠের ধরন ও স্পিড পরিবর্তন",
                            "100% offline breath/mouth-click cleaner, vocal tone shifter & speed control"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
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
                            "স্মার্ট ডি-ব্রেথ ও মাউথ-ক্লিক ক্লিনার (De-Breath & De-Click)",
                            "Smart De-Breath & Mouth-Click Cleaner"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = tr(
                            lang,
                            "কথার মাঝের জোরে শ্বাস নেওয়ার শব্দ ও মুখের ক্লিক শব্দ অটোমেটিক রিমুভ করবে",
                            "Automatically suppresses harsh breath gasps & mouth clicks between words"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = uiState.deBreathAndClickCleanEnabled,
                    onCheckedChange = onToggleDeBreathAndClick,
                    colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBg, checkedTrackColor = AmberGold),
                    modifier = Modifier.testTag("de_breath_switch")
                )
            }

            Text(
                text = tr(lang, "অফলাইন ভয়েস ক্যারেক্টার (Vocal Tone):", "Offline Vocal Character:"),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            OfflineVoiceCharacter.entries.forEach { character ->
                val selected = uiState.selectedVoiceCharacter == character
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) StudioSurfaceElevated else ObsidianBg)
                        .border(
                            1.dp,
                            if (selected) AmberGold else BorderSubtle,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectVoiceCharacter(character) }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                        .testTag("voice_char_${character.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = character.title(lang),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) AmberGold else TextPrimary
                    )
                    if (selected) {
                        Text(
                            text = "SELECTED",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberGold
                        )
                    }
                }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = tr(lang, "কণ্ঠের গতি (Speech Speed):", "Speech Speed:"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "${"%.2f".format(uiState.speechSpeedFactor)}x",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan
                    )
                }
                Slider(
                    value = uiState.speechSpeedFactor,
                    onValueChange = onSpeechSpeedChange,
                    valueRange = 0.75f..1.40f,
                    colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan),
                    modifier = Modifier.testTag("speech_speed_slider")
                )
            }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianBg)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            fontWeight = FontWeight.Bold
        )
    }
}
