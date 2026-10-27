package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppLanguage
import com.example.data.AudioTrackEntity
import com.example.data.tr
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/**
 * Custom Canvas-based Real-Time Audio Waveform Visualizer for Recording & Live Input Monitoring.
 * Renders:
 * 1. Studio oscilloscope time/dB grid lines
 * 2. Dynamic Silence-Skip dB Threshold gate reference lines (upper & lower)
 * 3. Mirrored real-time vertical amplitude bars with peak caps
 * 4. Smooth Bezier spline envelope wave overlay
 * 5. Live recording head indicator & telemetry badges
 */
@Composable
fun RealTimeRecordingWaveformCanvas(
    isRecording: Boolean,
    isSkippingSilence: Boolean,
    liveAmps: List<Float>,
    currentDb: Float,
    silenceDbThreshold: Float,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_idle_anim")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_shift"
    )

    val thresholdNorm = ((silenceDbThreshold + 60f) / 60f).coerceIn(0.08f, 0.85f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(142.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF060A12), Color(0xFF0D1626), Color(0xFF060A12))
                )
            )
            .border(
                width = 1.2.dp,
                color = when {
                    isRecording && isSkippingSilence -> AmberGold.copy(alpha = 0.7f)
                    isRecording -> CrimsonRecord.copy(alpha = 0.7f)
                    else -> BorderSubtle
                },
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("realtime_recording_waveform_canvas")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            val w = size.width
            val h = size.height
            val midY = h / 2f

            // 1. Subtle Studio Oscilloscope Grid Lines
            val vSteps = 6
            for (g in 1 until vSteps) {
                val gx = (w / vSteps) * g
                drawLine(
                    color = BorderSubtle.copy(alpha = 0.35f),
                    start = Offset(gx, 0f),
                    end = Offset(gx, h),
                    strokeWidth = 1.dp.toPx()
                )
            }
            drawLine(
                color = BorderSubtle.copy(alpha = 0.75f),
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 1.dp.toPx()
            )

            // 2. Dynamic dB Silence Threshold Gate Lines (Upper & Lower mirrored)
            val gateOffset = thresholdNorm * (midY * 0.88f)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            drawLine(
                color = AmberGold.copy(alpha = 0.55f),
                start = Offset(0f, midY - gateOffset),
                end = Offset(w, midY - gateOffset),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = dashEffect
            )
            drawLine(
                color = AmberGold.copy(alpha = 0.55f),
                start = Offset(0f, midY + gateOffset),
                end = Offset(w, midY + gateOffset),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = dashEffect
            )

            // 3. Prepare 48 real-time amplitude bars (or animated studio standby wave when idle)
            val barCount = 48
            val slotW = w / barCount
            val barW = max(2.5f, slotW * 0.62f)
            val upperWavePath = Path()

            for (i in 0 until barCount) {
                val rawAmp = if (isRecording && liveAmps.isNotEmpty()) {
                    val idx = (i * liveAmps.size / barCount).coerceIn(0, liveAmps.lastIndex)
                    liveAmps[idx]
                } else {
                    // Gentle standby studio monitor wave
                    val env = 0.14f + 0.18f * abs(sin(phaseShift + i * 0.24f)) *
                        (0.6f + 0.4f * sin(i * 0.13f))
                    env
                }

                val clampedAmp = rawAmp.coerceIn(0.04f, 0.98f)
                val halfBarH = max(3f, clampedAmp * (midY * 0.90f))
                val centerX = i * slotW + slotW / 2f

                val isBelowGate = clampedAmp <= thresholdNorm * 0.85f
                val barBrush = when {
                    isRecording && isSkippingSilence -> Brush.verticalGradient(
                        listOf(AmberGold.copy(alpha = 0.45f), AmberGold, AmberGold.copy(alpha = 0.45f))
                    )
                    isRecording && !isBelowGate -> Brush.verticalGradient(
                        listOf(EmeraldNeon, ElectricCyan, EmeraldNeon)
                    )
                    isRecording -> Brush.verticalGradient(
                        listOf(AmberGold.copy(alpha = 0.7f), ElectricCyan.copy(alpha = 0.7f), AmberGold.copy(alpha = 0.7f))
                    )
                    else -> Brush.verticalGradient(
                        listOf(ElectricCyan.copy(alpha = 0.45f), ElectricCyan, ElectricCyan.copy(alpha = 0.45f))
                    )
                }

                drawRoundRect(
                    brush = barBrush,
                    topLeft = Offset(centerX - barW / 2f, midY - halfBarH),
                    size = Size(barW, halfBarH * 2f),
                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                )

                // Build smooth spline path along upper waveform peaks
                val peakY = midY - halfBarH
                if (i == 0) {
                    upperWavePath.moveTo(centerX, peakY)
                } else {
                    val prevX = (i - 1) * slotW + slotW / 2f
                    val midCtrlX = (prevX + centerX) / 2f
                    upperWavePath.quadraticTo(prevX, peakY, midCtrlX, peakY)
                }
            }

            // 4. Draw glowing envelope spline stroke over the bars
            drawPath(
                path = upperWavePath,
                color = if (isRecording && !isSkippingSilence) EmeraldNeon.copy(alpha = 0.55f) else ElectricCyan.copy(alpha = 0.4f),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )

            // 5. Right-edge Live Record Playhead Line
            if (isRecording) {
                val headX = w - 4.dp.toPx()
                drawLine(
                    color = if (isSkippingSilence) AmberGold else CrimsonRecord,
                    start = Offset(headX, 2f),
                    end = Offset(headX, h - 2f),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Top-left Live Oscilloscope Label
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ObsidianBg.copy(alpha = 0.82f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isRecording && isSkippingSilence -> AmberGold
                            isRecording -> CrimsonRecord
                            else -> ElectricCyan
                        }
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when {
                    isRecording && isSkippingSilence -> tr(
                        language,
                        "সাইলেন্স স্কিপড (অটো-পজ)",
                        "SILENCE SKIPPED (AUTO-PAUSED)"
                    )
                    isRecording -> tr(
                        language,
                        "লাইভ ওয়েভফর্ম ক্যানভাস • ${"%.1f".format(currentDb)} dB",
                        "LIVE WAVEFORM CANVAS • ${"%.1f".format(currentDb)} dB"
                    )
                    else -> tr(
                        language,
                        "রিয়েল-টাইম অডিও ওয়েভফর্ম মনিটর",
                        "REAL-TIME AUDIO WAVEFORM MONITOR"
                    )
                },
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary
            )
        }

        // Bottom-right Gate Threshold Legend
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ObsidianBg.copy(alpha = 0.82f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = tr(
                    language,
                    "গেট থ্রেশহোল্ড: ${silenceDbThreshold.toInt()} dB",
                    "Gate Threshold: ${silenceDbThreshold.toInt()} dB"
                ),
                style = MaterialTheme.typography.labelSmall,
                color = AmberGold
            )
        }
    }
}

@Composable
fun ActiveTrackTransportBar(
    language: AppLanguage,
    track: AudioTrackEntity?,
    isPlaying: Boolean,
    playbackSec: Float,
    statusMessage: String,
    onTogglePlay: () -> Unit,
    onGenerateDemo: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        color = StudioSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) CrimsonRecord else ElectricCyan)
                            .testTag("transport_play_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Stop playback" else "Play active audio track",
                            tint = ObsidianBg
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = track?.title ?: tr(language, "লোড হচ্ছে…", "Loading track…"),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        val totalDur = track?.durationSec ?: 0f
                        Text(
                            text = "${"%.1f".format(playbackSec)}s / ${"%.1f".format(totalDur)}s • Peak ${"%.1f".format(track?.peakDb ?: -6f)} dB • Floor ${"%.1f".format(track?.noiseFloorDb ?: -42f)} dB",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                    }
                }

                TextButton(
                    onClick = onGenerateDemo,
                    modifier = Modifier.testTag("reset_demo_track_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = "Reset studio demo track",
                        tint = AmberGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = tr(language, "ডেমো ট্র্যাক", "Demo Track"),
                        style = MaterialTheme.typography.labelMedium,
                        color = AmberGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianBg.copy(alpha = 0.75f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldNeon,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * Interactive Studio Waveform Visualizer for Editor/Trimmer screen
 */
@Composable
fun InteractiveWaveformCanvas(
    waveformBins: List<Float>,
    silenceMaskBins: List<Boolean>,
    durationSec: Float,
    playbackSec: Float,
    sampleStartSec: Float,
    sampleEndSec: Float,
    showSampleWindow: Boolean,
    language: AppLanguage = AppLanguage.BN,
    onSelectTimeSec: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val safeDur = max(1f, durationSec)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF070B12), Color(0xFF0E1726), Color(0xFF070B12))
                )
            )
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .pointerInput(safeDur, onSelectTimeSec) {
                if (onSelectTimeSec != null) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        onSelectTimeSec(ratio * safeDur)
                    }
                }
            }
            .testTag("studio_waveform_canvas")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            val w = size.width
            val h = size.height
            val midY = h / 2f
            val count = max(1, waveformBins.size)
            val barSlot = w / count
            val barWidth = max(2f, barSlot * 0.68f)

            drawLine(
                color = BorderSubtle,
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = AmberGold.copy(alpha = 0.28f),
                start = Offset(0f, midY * 0.68f),
                end = Offset(w, midY * 0.68f),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )

            if (showSampleWindow && durationSec > 0f) {
                val startX = (sampleStartSec / safeDur).coerceIn(0f, 1f) * w
                val endX = (sampleEndSec / safeDur).coerceIn(0f, 1f) * w
                val boxW = max(6f, endX - startX)
                drawRoundRect(
                    color = VioletAi.copy(alpha = 0.18f),
                    topLeft = Offset(startX, 0f),
                    size = Size(boxW, h),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawLine(
                    color = VioletAi,
                    start = Offset(startX, 0f),
                    end = Offset(startX, h),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = VioletAi,
                    start = Offset(endX, 0f),
                    end = Offset(endX, h),
                    strokeWidth = 2.dp.toPx()
                )
            }

            for (i in 0 until count) {
                val amp = waveformBins.getOrElse(i) { 0.05f }.coerceIn(0.04f, 1.0f)
                val isSilent = silenceMaskBins.getOrElse(i) { false }
                val x = i * barSlot

                if (isSilent) {
                    drawRect(
                        color = AmberGold.copy(alpha = 0.14f),
                        topLeft = Offset(x, 4f),
                        size = Size(barSlot + 1f, h - 8f)
                    )
                }

                val halfBarH = max(3f, amp * (midY * 0.90f))
                val barColor = when {
                    isSilent -> AmberGold
                    amp > 0.75f -> EmeraldNeon
                    else -> ElectricCyan
                }

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x + (barSlot - barWidth) / 2f, midY - halfBarH),
                    size = Size(barWidth, halfBarH * 2f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }

            if (playbackSec > 0f && durationSec > 0f) {
                val playX = (playbackSec / safeDur).coerceIn(0f, 1f) * w
                drawLine(
                    color = CrimsonRecord,
                    start = Offset(playX, 0f),
                    end = Offset(playX, h),
                    strokeWidth = 2.5.dp.toPx()
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ObsidianBg.copy(alpha = 0.85f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LegendDot(color = ElectricCyan, label = tr(language, "ভয়েস", "Voice"))
            LegendDot(color = AmberGold, label = tr(language, "সাইলেন্স", "Silence"))
            if (showSampleWindow) {
                LegendDot(color = VioletAi, label = tr(language, "নয়েজ স্যাম্পল", "Noise Sample"))
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

/**
 * 16-Band Spectral Noise Print Fingerprint Bar Chart
 */
@Composable
fun SpectralNoisePrintVisualizer(
    spectralBandsCsv: String,
    isCapturing: Boolean,
    captureProgress: Float,
    modifier: Modifier = Modifier
) {
    val bands = spectralBandsCsv.split(",")
        .mapNotNull { it.trim().toFloatOrNull() }
        .ifEmpty { List(16) { 0.35f } }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianBg)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        val count = bands.size
        val slotW = size.width / count
        val barW = slotW * 0.65f

        for (i in 0 until count) {
            val rawVal = bands[i].coerceIn(0.1f, 1.0f)
            val displayVal = if (isCapturing) {
                (rawVal * (0.4f + 0.6f * captureProgress)).coerceIn(0.1f, 1.0f)
            } else {
                rawVal
            }
            val barH = displayVal * size.height
            val color = when {
                i < 5 -> AmberGold
                i < 11 -> ElectricCyan
                else -> VioletAi
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(i * slotW + (slotW - barW) / 2f, size.height - barH),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}

/**
 * Settings & Concise Feature Usage Guide Dialog (triggered from the top Settings icon)
 */
@Composable
fun SettingsAndFeatureGuideDialog(
    language: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = ElectricCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr(
                            language,
                            "সেটিংস ও ফিচার ব্যবহারের গাইড",
                            "Settings & Feature Guide"
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Language Switcher Card inside Settings
                Surface(
                    color = ObsidianBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr(language, "অ্যাপের ভাষা (Language):", "App Language:"),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AppLanguage.entries.forEach { lang ->
                                val selected = language == lang
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) ElectricCyan else StudioSurfaceElevated)
                                        .clickable { onSelectLanguage(lang) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("settings_lang_${lang.code}")
                                ) {
                                    Text(
                                        text = lang.displayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) ObsidianBg else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                GuideSectionItem(
                    icon = Icons.Filled.Fingerprint,
                    accent = VioletAi,
                    title = tr(
                        language,
                        "১. নয়েজ প্রোফাইল ও স্মার্ট রেকর্ডিং",
                        "1. Noise Profile & Smart Recording"
                    ),
                    desc = tr(
                        language,
                        "• Recorder ট্যাবে গিয়ে ৩-৫ সেকেন্ড সিলেক্ট করে 'নয়েজ প্রিন্ট ক্যাপচার' বাটন চাপুন।\n• 'স্মার্ট নয়েজ ফিল্টার' চালু রাখলে মূল কণ্ঠের একুরেসি ১০০% রেখে ফ্যান/এসি শব্দ মুছে যাবে।\n• 'সাইলেন্স স্কিপ' স্লাইডারে সেকেন্ড (১-৩ সে.) ও dB সেট করে রেকর্ড করলে চুপ থাকা অংশ অটো-পজ হবে।",
                        "• In the Recorder tab, choose 3–5s and tap 'Capture Noise Print' to learn room fan/AC noise.\n• Enable 'Smart Noise Filter' for 100% voice accuracy noise removal.\n• Set Silence Skip seconds (1–3s) & dB threshold to auto-pause during quiet gaps."
                    )
                )

                GuideSectionItem(
                    icon = Icons.Filled.ContentCut,
                    accent = AmberGold,
                    title = tr(
                        language,
                        "২. ডুয়েল-মোড সাইলেন্স ও ১-ক্লিক নয়েজ রিমুভ",
                        "2. Dual-Mode Silence & Sample Noise Removal"
                    ),
                    desc = tr(
                        language,
                        "• Editor ট্যাবে ওয়েভফর্মের যেকোনো নয়েজ অংশে ট্যাপ করে 'স্যাম্পল নিন' ও '১-ক্লিকে নয়েজ মুছুন' চাপুন।\n• সাইলেন্স রিমুভালে dB মোড অথবা সেকেন্ড লিমিট মোড (যেমন > ২ সেকেন্ড) সিলেক্ট করে 'সাইলেন্স কেটে বাদ দিন' বাটনে চাপুন।",
                        "• In the Editor tab, tap any region on the waveform, click 'Sample Noise', then '1-Click Clean'.\n• Choose Dual Mode, dB Mode, or Seconds Limit Mode (e.g. > 2s) and tap 'Remove Silence'."
                    )
                )

                GuideSectionItem(
                    icon = Icons.Filled.CallSplit,
                    accent = ElectricCyan,
                    title = tr(
                        language,
                        "৩. অটো-স্প্লিট (১০০ টুকরো) ও '+' টাইম রেঞ্জ",
                        "3. Auto-Split (100 Clips) & '+' Time Ranges"
                    ),
                    desc = tr(
                        language,
                        "• 'সাইলেন্স অনুযায়ী অটো-স্প্লিট' চাপলে লম্বা অডিও বিরতি অনুযায়ী ১০০ টুকরো পর্যন্ত ভাগ হয়ে যাবে।\n• নিজের মতো টুকরো করতে '+ নতুন রেঞ্জ' বাটনে চেপে শুরু ও শেষ সেকেন্ড (যেমন ১ থেকে ১২ সে.) লিখে স্প্লিট করুন এবং প্রতিটি টুকরো Play করে শুনুন।",
                        "• Tap 'Auto-Split by Silence' to slice long recordings into up to 100 clips automatically.\n• Or tap '+ Add Range' to enter custom start/end seconds (e.g. 1 to 20s) and play each clip."
                    )
                )

                GuideSectionItem(
                    icon = Icons.Filled.AutoAwesome,
                    accent = EmeraldNeon,
                    title = tr(
                        language,
                        "৪. এআই ভয়েস এনহ্যান্সার ও অটো-নর্মালাইজেশন",
                        "4. AI Voice Enhancer & Auto-Normalization"
                    ),
                    desc = tr(
                        language,
                        "• Effects ট্যাবে গিয়ে 'ভয়েস বুস্টার' ও 'অটো-নর্মালাইজেশন' চালু করলে পুরো অডিওর সাউন্ড এক লেভেলে চলে আসবে।\n• পডকাস্ট কোয়ালিটি ভয়েসের জন্য Crystal Podcast বা Broadcast প্রিসেট সিলেক্ট করে মাস্টারিং বাটনে চাপুন।",
                        "• In the Effects tab, enable Auto-Normalization and adjust Voice Booster (+dB) for uniform loudness.\n• Pick an AI Studio Preset (Crystal Podcast, Broadcast Warm, etc.) and tap Apply Mastering."
                    )
                )

                GuideSectionItem(
                    icon = Icons.Filled.Download,
                    accent = ElectricCyan,
                    title = tr(
                        language,
                        "৫. কীভাবে APK ডাউনলোড করবেন?",
                        "5. How to Download the APK"
                    ),
                    desc = tr(
                        language,
                        "• AI Studio-এর উপরের ডান কোণায় Settings (⚙️) বা Export মেনুতে ক্লিক করে 'Download APK' সিলেক্ট করলেই সরাসরি আপনার ফোনে বা পিসিতে ইনস্টলযোগ্য APK ফাইল ডাউনলোড হয়ে যাবে।",
                        "• Click the AI Studio Settings (⚙️) or Export menu at the top-right of the browser and select 'Download APK' or 'Export ZIP' to install the app on your device."
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBg),
                modifier = Modifier.testTag("close_settings_guide_button")
            ) {
                Text(
                    text = tr(language, "বন্ধ করুন (Done)", "Done"),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun GuideSectionItem(
    icon: ImageVector,
    accent: Color,
    title: String,
    desc: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianBg.copy(alpha = 0.7f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}
