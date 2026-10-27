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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.filled.AudioFile
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
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppLanguage
import com.example.data.AudioTrackEntity
import com.example.data.ManualTimeRange
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
import kotlin.math.min
import kotlin.math.sin

/**
 * Custom Canvas-based Real-Time Audio Waveform Visualizer for Recording & Live Input Monitoring.
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

            val barCount = 48
            val slotW = w / barCount
            val barW = max(2.5f, slotW * 0.62f)
            val upperWavePath = Path()

            for (i in 0 until barCount) {
                val rawAmp = if (isRecording && liveAmps.isNotEmpty()) {
                    val idx = (i * liveAmps.size / barCount).coerceIn(0, liveAmps.lastIndex)
                    liveAmps[idx]
                } else {
                    0.14f + 0.18f * abs(sin(phaseShift + i * 0.24f)) *
                        (0.6f + 0.4f * sin(i * 0.13f))
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

                val peakY = midY - halfBarH
                if (i == 0) {
                    upperWavePath.moveTo(centerX, peakY)
                } else {
                    val prevX = (i - 1) * slotW + slotW / 2f
                    val midCtrlX = (prevX + centerX) / 2f
                    upperWavePath.quadraticTo(prevX, peakY, midCtrlX, peakY)
                }
            }

            drawPath(
                path = upperWavePath,
                color = if (isRecording && !isSkippingSilence) EmeraldNeon.copy(alpha = 0.55f) else ElectricCyan.copy(alpha = 0.4f),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )

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
    onImportAudio: () -> Unit,
    onImportVideo: () -> Unit,
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
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (track?.isVideoTrack == true) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(VioletAi.copy(alpha = 0.22f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "VIDEO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VioletAi
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = track?.title ?: tr(language, "লোড হচ্ছে…", "Loading track…"),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1
                            )
                        }
                        val totalDur = track?.durationSec ?: 0f
                        Text(
                            text = "${"%.1f".format(playbackSec)}s / ${"%.1f".format(totalDur)}s • Peak ${"%.1f".format(track?.peakDb ?: -6f)} dB • Floor ${"%.1f".format(track?.noiseFloorDb ?: -42f)} dB",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Import Bar: Gallery Audio | Gallery Video | Demo Track
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onImportAudio,
                    border = ButtonDefaults.outlinedButtonBorder(true).copy(
                        brush = SolidColor(ElectricCyan.copy(alpha = 0.7f))
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("transport_import_audio_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AudioFile,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = tr(language, "গ্যালারি অডিও", "Import Audio"),
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onImportVideo,
                    border = ButtonDefaults.outlinedButtonBorder(true).copy(
                        brush = SolidColor(VioletAi.copy(alpha = 0.7f))
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("transport_import_video_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.VideoFile,
                        contentDescription = null,
                        tint = VioletAi,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = tr(language, "ভিডিও ইনপুট", "Import Video"),
                        style = MaterialTheme.typography.labelSmall,
                        color = VioletAi,
                        maxLines = 1
                    )
                }

                TextButton(
                    onClick = onGenerateDemo,
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("reset_demo_track_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = "Reset studio demo track",
                        tint = AmberGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = tr(language, "ডেমো", "Demo"),
                        style = MaterialTheme.typography.labelSmall,
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
 * Interactive Studio WAV Waveform Visualizer with:
 * - Voice & Silence region visualization
 * - Noise Sample selection window (Violet)
 * - Visual WAV Multi-Cut range selector + saved Multi-Cut overlays (Emerald)
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
    visualCutStartSec: Float = 0f,
    visualCutEndSec: Float = 0f,
    showVisualCutOverlay: Boolean = false,
    savedCutRanges: List<ManualTimeRange> = emptyList(),
    language: AppLanguage = AppLanguage.BN,
    onSelectTimeSec: ((Float) -> Unit)? = null,
    onDragVisualRange: ((Float, Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val safeDur = max(1f, durationSec)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(158.dp)
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
            .pointerInput(safeDur, onDragVisualRange, visualCutStartSec, visualCutEndSec) {
                if (onDragVisualRange != null) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        val w = size.width.coerceAtLeast(1).toFloat()
                        val touchSec = (change.position.x / w).coerceIn(0f, 1f) * safeDur
                        val deltaSec = (dragAmount / w) * safeDur
                        val distToStart = abs(touchSec - visualCutStartSec)
                        val distToEnd = abs(touchSec - visualCutEndSec)
                        if (distToStart <= distToEnd) {
                            val newStart = (visualCutStartSec + deltaSec).coerceIn(0f, max(0f, visualCutEndSec - 0.5f))
                            onDragVisualRange(newStart, visualCutEndSec)
                        } else {
                            val newEnd = (visualCutEndSec + deltaSec).coerceIn(min(safeDur, visualCutStartSec + 0.5f), safeDur)
                            onDragVisualRange(visualCutStartSec, newEnd)
                        }
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

            // Draw Saved Multi-Cut Ranges on the WAV view
            if (savedCutRanges.isNotEmpty() && durationSec > 0f) {
                savedCutRanges.forEach { slot ->
                    val sSec = slot.startSecText.toFloatOrNull() ?: return@forEach
                    val eSec = slot.endSecText.toFloatOrNull() ?: return@forEach
                    val sx = (sSec / safeDur).coerceIn(0f, 1f) * w
                    val ex = (eSec / safeDur).coerceIn(0f, 1f) * w
                    if (ex > sx) {
                        drawRoundRect(
                            color = ElectricCyan.copy(alpha = 0.09f),
                            topLeft = Offset(sx, 6f),
                            size = Size(ex - sx, h - 12f),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawLine(
                            color = ElectricCyan.copy(alpha = 0.45f),
                            start = Offset(sx, 4f),
                            end = Offset(sx, h - 4f),
                            strokeWidth = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                        )
                    }
                }
            }

            // Draw Noise Sample Selection Box if enabled
            if (showSampleWindow && durationSec > 0f) {
                val startX = (sampleStartSec / safeDur).coerceIn(0f, 1f) * w
                val endX = (sampleEndSec / safeDur).coerceIn(0f, 1f) * w
                val boxW = max(6f, endX - startX)
                drawRoundRect(
                    color = VioletAi.copy(alpha = 0.16f),
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

            // Draw Active Visual WAV Multi-Cut Handle Box (Emerald Green)
            if (showVisualCutOverlay && durationSec > 0f) {
                val cutStartX = (visualCutStartSec / safeDur).coerceIn(0f, 1f) * w
                val cutEndX = (visualCutEndSec / safeDur).coerceIn(0f, 1f) * w
                val cutW = max(8f, cutEndX - cutStartX)
                drawRoundRect(
                    color = EmeraldNeon.copy(alpha = 0.20f),
                    topLeft = Offset(cutStartX, 2f),
                    size = Size(cutW, h - 4f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                // Left & Right Visual Cut Handles
                drawLine(
                    color = EmeraldNeon,
                    start = Offset(cutStartX, 0f),
                    end = Offset(cutStartX, h),
                    strokeWidth = 3.dp.toPx()
                )
                drawCircle(
                    color = EmeraldNeon,
                    radius = 6.dp.toPx(),
                    center = Offset(cutStartX, 10.dp.toPx())
                )
                drawLine(
                    color = EmeraldNeon,
                    start = Offset(cutEndX, 0f),
                    end = Offset(cutEndX, h),
                    strokeWidth = 3.dp.toPx()
                )
                drawCircle(
                    color = EmeraldNeon,
                    radius = 6.dp.toPx(),
                    center = Offset(cutEndX, h - 10.dp.toPx())
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LegendDot(color = ElectricCyan, label = tr(language, "ভয়েস", "Voice"))
            LegendDot(color = AmberGold, label = tr(language, "সাইলেন্স", "Silence"))
            if (showVisualCutOverlay) {
                LegendDot(color = EmeraldNeon, label = tr(language, "WAV কাট সিলেকশন", "WAV Cut Range"))
            }
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
                    .heightIn(max = 440.dp)
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
                    icon = Icons.Filled.AudioFile,
                    accent = ElectricCyan,
                    title = tr(
                        language,
                        "১. গ্যালারি থেকে অডিও ও ভিডিও ইনপুট (অফলাইন)",
                        "1. Gallery Audio & Video Import (100% Offline)"
                    ),
                    desc = tr(
                        language,
                        "• উপরের বারে থাকা 'গ্যালারি অডিও' বা 'ভিডিও ইনপুট' বাটনে ট্যাপ করে ফোনের যেকোনো অডিও (MP3/WAV/AAC) বা ভিডিও (MP4) ইনপুট করুন।\n• ভিডিও ইনপুট করলে তার ভেতরের ফ্যান/এসি নয়েজ ও সাইলেন্স ক্লিন করে আবার MP4 আকারে গ্যালারিতে সেভ করা যাবে।",
                        "• Tap 'Import Audio' or 'Import Video' in the top bar to load any audio or video from your phone.\n• Imported videos can be denoised, silence-trimmed, and saved back to Gallery as clean MP4."
                    )
                )

                GuideSectionItem(
                    icon = Icons.Filled.Fingerprint,
                    accent = VioletAi,
                    title = tr(
                        language,
                        "২. নয়েজ প্রোফাইল ও সাইলেন্স স্কিপ রেকর্ডিং",
                        "2. Noise Profile & Silence Skip Recording"
                    ),
                    desc = tr(
                        language,
                        "• ৩-৫ সেকেন্ড ফ্যান/এসি শব্দের 'নয়েজ প্রিন্ট' ক্যাপচার করলে কণ্ঠের একুরেসি ১০০% রেখে ব্যাকগ্রাউন্ড নয়েজ মুছে যাবে।\n• সাইলেন্স স্কিপ স্লাইডারে সেকেন্ড (১-৩ সে.) ও dB সেট করলে চুপ থাকা অংশ অটো-পজ হবে।",
                        "• Capture a 3–5s Noise Print to remove background noise with 100% vocal accuracy.\n• Set Silence Skip seconds & dB threshold to auto-pause during quiet gaps."
                    )
                )

                GuideSectionItem(
                    icon = Icons.Filled.CallSplit,
                    accent = EmeraldNeon,
                    title = tr(
                        language,
                        "৩. WAV ভিউ সিলেক্টর, '+' মাল্টি-কাট ও গ্যালারি সেভ",
                        "3. Visual WAV Multi-Cut, '+' Ranges & Gallery Save"
                    ),
                    desc = tr(
                        language,
                        "• Editor ট্যাবে সরাসরি WAV ওয়েভফর্ম ভিউ থেকে শুরু ও শেষ অংশ সিলেক্ট করে '+ WAV সিলেকশন যোগ করুন' চাপুন, অথবা নিচে '+' বাটন দিয়ে একাধিক টাইম স্লট লিখুন।\n• কাট করা সমস্ত অডিও এক ক্লিকেই MP3, WAV বা AAC ফরম্যাটে ফোনের গ্যালারিতে সেভ করতে পারবেন।",
                        "• Visually drag/select start & end points on the WAV waveform and tap '+' to add to your cut list, or type custom ranges.\n• Save all cut clips to your phone's Gallery in MP3, WAV, or AAC format."
                    )
                )

                GuideSectionItem(
                    icon = Icons.Filled.AutoAwesome,
                    accent = AmberGold,
                    title = tr(
                        language,
                        "৪. অফলাইন AI ভয়েস এনহ্যান্সার, ডি-ব্রেথ ও MP3/WAV/AAC এক্সপোর্ট",
                        "4. Offline AI Voice Studio, De-Breath & Multi-Format Export"
                    ),
                    desc = tr(
                        language,
                        "• Effects ট্যাবে Smart De-Breath (নিঃশ্বাসের শব্দ ও ক্লিক রিমুভ), ভয়েস ক্যারেক্টার (Deep Baritone / News Anchor), ভয়েস বুস্টার ও অটো-নর্মালাইজেশন প্রয়োগ করে MP3, WAV বা AAC ফরম্যাটে এক্সপোর্ট করুন।",
                        "• In Effects tab, apply Smart De-Breath/De-Click, Offline Voice Character, Auto-Normalization, and export as MP3, WAV, AAC, or Clean Video."
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
                        "• Click the AI Studio Settings (⚙️) or Export menu at the top-right of the browser and select 'Download APK' or 'Export ZIP'."
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
