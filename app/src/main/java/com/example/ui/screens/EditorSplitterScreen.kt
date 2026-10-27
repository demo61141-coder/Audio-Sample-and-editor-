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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AudioSplitClip
import com.example.data.SilenceRemovalMode
import com.example.data.tr
import com.example.ui.StudioUiState
import com.example.ui.components.InteractiveWaveformCanvas
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
fun EditorSplitterScreen(
    uiState: StudioUiState,
    onSilenceModeChange: (SilenceRemovalMode) -> Unit,
    onSilenceDbChange: (Float) -> Unit,
    onSilenceLimitSecChange: (Float) -> Unit,
    onUpdateSampleWindow: (Float, Float) -> Unit,
    onCaptureNoiseFromSelection: () -> Unit,
    onOneClickNoiseRemoval: () -> Unit,
    onExecuteSilenceRemoval: () -> Unit,
    onRunAutoSilenceSplit: () -> Unit,
    onAddManualRangeSlot: () -> Unit,
    onUpdateManualRangeSlot: (Long, String, String) -> Unit,
    onRemoveManualRangeSlot: (Long) -> Unit,
    onRunManualRangeSplit: () -> Unit,
    onToggleClipPlay: (AudioSplitClip) -> Unit
) {
    val lang = uiState.language
    val trackDur = uiState.activeTrack?.durationSec ?: 32f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Waveform Trimmer & Sample Selector Card
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = StudioSurface,
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column {
                        Text(
                            text = tr(
                                lang,
                                "ওয়েভফর্ম ট্রিমার ও সাইলেন্স ডিটেক্টর",
                                "Waveform Trimmer & Silence Detector"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = tr(
                                lang,
                                "শনাক্তকৃত সাইলেন্স: ${uiState.detectedSilenceCount}টি অংশ (মোট ~${"%.1f".format(uiState.estimatedSilenceSec)} সেকেন্ড)",
                                "Detected Silence: ${uiState.detectedSilenceCount} segments (~${"%.1f".format(uiState.estimatedSilenceSec)}s total)"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberGold
                        )
                    }

                    InteractiveWaveformCanvas(
                        waveformBins = uiState.waveformBins,
                        silenceMaskBins = uiState.silenceMaskBins,
                        durationSec = trackDur,
                        playbackSec = uiState.playbackPositionSec,
                        sampleStartSec = uiState.sampleWindowStartSec,
                        sampleEndSec = uiState.sampleWindowEndSec,
                        showSampleWindow = true,
                        language = lang,
                        onSelectTimeSec = { tappedSec ->
                            onUpdateSampleWindow(tappedSec, (tappedSec + 3.2f).coerceAtMost(trackDur))
                        }
                    )

                    Text(
                        text = tr(
                            lang,
                            "টিপ: ওয়েভফর্মের যেকোনো জায়গায় ট্যাপ করে নয়েজ স্যাম্পল এরিয়া (${"%.1f".format(uiState.sampleWindowStartSec)}s - ${"%.1f".format(uiState.sampleWindowEndSec)}s) সিলেক্ট করুন।",
                            "Tip: Tap anywhere on the waveform to select a Noise Sample window (${"%.1f".format(uiState.sampleWindowStartSec)}s - ${"%.1f".format(uiState.sampleWindowEndSec)}s)."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCaptureNoiseFromSelection,
                            border = ButtonDefaults.outlinedButtonBorder(true).copy(
                                brush = SolidColor(VioletAi)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("sample_noise_from_waveform_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fingerprint,
                                contentDescription = null,
                                tint = VioletAi,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr(lang, "স্যাম্পল নিন", "Sample Noise"),
                                color = VioletAi,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Button(
                            onClick = onOneClickNoiseRemoval,
                            enabled = !uiState.isProcessingDsp,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldNeon,
                                contentColor = ObsidianBg
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("one_click_denoise_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr(lang, "১-ক্লিকে নয়েজ মুছুন", "1-Click Clean Noise"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Feature 3: Dual-Mode Silence Removal Panel
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ContentCut,
                            contentDescription = null,
                            tint = AmberGold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = tr(
                                    lang,
                                    "৩. অ্যাডভান্সড ডুয়েল-মোড সাইলেন্স রিমুভাল",
                                    "3. Advanced Dual-Mode Silence Removal"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = tr(
                                    lang,
                                    "dB মোড অথবা সেকেন্ড লিমিট মোড দিয়ে অপ্রয়োজনীয় নীরবতা কাটুন",
                                    "Cut unwanted silence using dB Threshold Mode or Seconds Limit Mode"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    SilenceRemovalMode.entries.forEach { mode ->
                        val selected = uiState.silenceRemovalMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) StudioSurfaceElevated else ObsidianBg)
                                .border(
                                    width = 1.dp,
                                    color = if (selected) AmberGold else BorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSilenceModeChange(mode) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("silence_mode_${mode.name}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSilenceModeChange(mode) },
                                colors = RadioButtonDefaults.colors(selectedColor = AmberGold)
                            )
                            Column(modifier = Modifier.padding(start = 6.dp)) {
                                Text(
                                    text = mode.title(lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selected) AmberGold else TextPrimary
                                )
                                Text(
                                    text = mode.subtitle(lang),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    if (uiState.silenceRemovalMode != SilenceRemovalMode.SECONDS_LIMIT_ONLY) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = tr(
                                        lang,
                                        "ডেসিবেল লিমিট (dB Mode Threshold):",
                                        "Decibel Limit (dB Mode Threshold):"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = tr(
                                        lang,
                                        "${uiState.silenceDbThreshold.toInt()} dB-এর নিচে",
                                        "Below ${uiState.silenceDbThreshold.toInt()} dB"
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ElectricCyan
                                )
                            }
                            Slider(
                                value = uiState.silenceDbThreshold,
                                onValueChange = onSilenceDbChange,
                                valueRange = -55f..-20f,
                                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan),
                                modifier = Modifier.testTag("editor_silence_db_slider")
                            )
                        }
                    }

                    if (uiState.silenceRemovalMode != SilenceRemovalMode.DB_ONLY) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = tr(
                                        lang,
                                        "সেকেন্ড লিমিট (যেমন ২ সে. বেশি সাইলেন্ট):",
                                        "Seconds Limit (e.g. > 2s silent):"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = tr(
                                        lang,
                                        "> ${"%.1f".format(uiState.silenceDurationLimitSec)} সেকেন্ড",
                                        "> ${"%.1f".format(uiState.silenceDurationLimitSec)} sec"
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
                                modifier = Modifier.testTag("editor_silence_sec_slider")
                            )
                        }
                    }

                    Button(
                        onClick = onExecuteSilenceRemoval,
                        enabled = !uiState.isProcessingDsp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGold,
                            contentColor = ObsidianBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("execute_silence_removal_button")
                    ) {
                        Icon(imageVector = Icons.Filled.ContentCut, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(
                                lang,
                                "সাইলেন্স কেটে বাদ দিন (Remove Silence)",
                                "Cut & Remove Silence Now"
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Feature 4: Flexible Audio Splitter (Auto up to 100 clips + Manual '+' Ranges)
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
                            imageVector = Icons.Filled.CallSplit,
                            contentDescription = null,
                            tint = ElectricCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = tr(
                                    lang,
                                    "৪. ফ্লেক্সিবল অডিও স্প্লিটার (সর্বোচ্চ ১০০ টুকরো)",
                                    "4. Flexible Audio Splitter (Up to 100 Clips)"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = tr(
                                    lang,
                                    "সাইলেন্স বেসড অটো-স্প্লিট অথবা '+' বাটন চেপে ম্যানুয়াল টাইম রেঞ্জ",
                                    "Silence-based Auto-Split or Manual Time Ranges with '+' button"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = onRunAutoSilenceSplit,
                        enabled = !uiState.isProcessingDsp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = ObsidianBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auto_split_silence_button")
                    ) {
                        Icon(imageVector = Icons.Filled.CallSplit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(
                                lang,
                                "সাইলেন্স অনুযায়ী অটো-স্প্লিট (Up to 100 Clips)",
                                "Auto-Split by Silence (Up to 100 Clips)"
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr(
                                lang,
                                "ম্যানুয়াল টাইম রেঞ্জ স্লট (${uiState.manualTimeRanges.size}টি):",
                                "Manual Time Range Slots (${uiState.manualTimeRanges.size}):"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        OutlinedButton(
                            onClick = onAddManualRangeSlot,
                            border = ButtonDefaults.outlinedButtonBorder(true).copy(
                                brush = SolidColor(EmeraldNeon)
                            ),
                            modifier = Modifier.testTag("add_manual_time_range_plus_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add new time range slot",
                                tint = EmeraldNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tr(lang, "+ নতুন রেঞ্জ", "+ Add Range"),
                                color = EmeraldNeon,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    uiState.manualTimeRanges.forEachIndexed { index, slot ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianBg)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(ElectricCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ElectricCyan
                                )
                            }

                            OutlinedTextField(
                                value = slot.startSecText,
                                onValueChange = { onUpdateManualRangeSlot(slot.id, it, slot.endSecText) },
                                label = { Text(tr(lang, "শুরু (সে.)", "Start (s)")) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricCyan,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("range_start_input_$index")
                            )

                            Text(
                                text = tr(lang, "থেকে", "to"),
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            OutlinedTextField(
                                value = slot.endSecText,
                                onValueChange = { onUpdateManualRangeSlot(slot.id, slot.startSecText, it) },
                                label = { Text(tr(lang, "শেষ (সে.)", "End (s)")) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricCyan,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("range_end_input_$index")
                            )

                            IconButton(
                                onClick = { onRemoveManualRangeSlot(slot.id) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("remove_range_button_$index")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Delete time range",
                                    tint = CrimsonRecord
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onRunManualRangeSplit,
                        enabled = !uiState.isProcessingDsp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldNeon,
                            contentColor = ObsidianBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("execute_manual_split_button")
                    ) {
                        Icon(imageVector = Icons.Filled.CallSplit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(
                                lang,
                                "টাইম রেঞ্জ অনুযায়ী অডিও টুকরো করুন (${uiState.manualTimeRanges.size} Clips)",
                                "Split Audio by Custom Time Ranges (${uiState.manualTimeRanges.size} Clips)"
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Generated Split Clips Output List
        if (uiState.generatedSplitClips.isNotEmpty()) {
            item {
                Text(
                    text = tr(
                        lang,
                        "টুকরো করা অডিও ক্লিপসমূহ (${uiState.generatedSplitClips.size}টি টুকরো প্রস্তুত):",
                        "Generated Audio Split Clips (${uiState.generatedSplitClips.size} clips ready):"
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldNeon
                )
            }
            items(uiState.generatedSplitClips, key = { it.filePath }) { clip ->
                val isPlayingThis = uiState.playingClipFilePath == clip.filePath
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isPlayingThis) ElectricCyan else BorderSubtle,
                            RoundedCornerShape(14.dp)
                        )
                        .testTag("split_clip_card_${clip.index}"),
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(14.dp)
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
                                text = clip.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "${"%.1f".format(clip.startSec)}s → ${"%.1f".format(clip.endSec)}s • ${"%.1f".format(clip.durationSec)}s • Peak: ${"%.1f".format(clip.peakDb)} dB",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                        IconButton(
                            onClick = { onToggleClipPlay(clip) },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isPlayingThis) CrimsonRecord else ElectricCyan)
                                .testTag("play_split_clip_${clip.index}")
                        ) {
                            Icon(
                                imageVector = if (isPlayingThis) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlayingThis) "Stop clip" else "Play split clip",
                                tint = ObsidianBg
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
