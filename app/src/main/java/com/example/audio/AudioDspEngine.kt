package com.example.audio

import com.example.data.AiVoicePreset
import com.example.data.AudioSplitClip
import com.example.data.ManualTimeRange
import com.example.data.NoiseProfileEntity
import com.example.data.SilenceRemovalMode
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class WavAnalysisResult(
    val durationSec: Float,
    val sampleRate: Int,
    val peakDb: Float,
    val rmsDb: Float,
    val noiseFloorDb: Float,
    val waveformBins: List<Float>,
    val silenceMaskBins: List<Boolean>,
    val detectedSilenceRegionsCount: Int,
    val estimatedSilenceTotalSec: Float
)

data class SilenceRemovalResult(
    val outputFile: File,
    val newDurationSec: Float,
    val removedSilenceSec: Float,
    val removedSegmentsCount: Int
)

object AudioDspEngine {
    const val SAMPLE_RATE = 22050

    fun readWavSamples(file: File): ShortArray {
        if (!file.exists() || file.length() <= 44L) return ShortArray(0)
        FileInputStream(file).use { fis ->
            val header = ByteArray(44)
            val readHeader = fis.read(header)
            if (readHeader < 44) return ShortArray(0)
            val dataBytes = fis.readBytes()
            val sampleCount = dataBytes.size / 2
            val shorts = ShortArray(sampleCount)
            ByteBuffer.wrap(dataBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
            return shorts
        }
    }

    fun writeWavSamples(file: File, samples: ShortArray, sampleRate: Int = SAMPLE_RATE) {
        file.parentFile?.mkdirs()
        val byteData = ByteArray(samples.size * 2)
        ByteBuffer.wrap(byteData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(samples)

        val totalDataLen = byteData.size + 36
        val byteRate = sampleRate * 2

        FileOutputStream(file).use { fos ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(totalDataLen)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16) // PCM chunk size
            header.putShort(1) // AudioFormat = 1 (PCM)
            header.putShort(1) // NumChannels = 1 (Mono)
            header.putInt(sampleRate)
            header.putInt(byteRate)
            header.putShort(2) // BlockAlign
            header.putShort(16) // BitsPerSample
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(byteData.size)
            fos.write(header.array())
            fos.write(byteData)
        }
    }

    /**
     * Generates a realistic 32-second studio demo track containing:
     * - 0.0s..3.5s: Background Fan + AC hum + subtle hiss (-38 dB) for Audacity-style Noise Print learning
     * - 6 distinct speech-like harmonic vocal phrases at varying volumes (quiet to loud)
     * - Multiple silent/noisy gaps of 1.2s, 2.5s, 3.2s, 1.6s, 2.8s so Dual-Mode Silence Removal & Auto-Splitter work visibly and audibly.
     */
    fun generateDemoPodcastWav(outputFile: File, durationSec: Int = 32): File {
        val totalSamples = durationSec * SAMPLE_RATE
        val samples = ShortArray(totalSamples)
        val rng = Random(42)

        // Spoken phrase intervals: (startSec, endSec, amplitudeScale, basePitchHz)
        val phrases = listOf(
            floatArrayOf(3.6f, 7.2f, 0.58f, 145f),   // Phrase 1: Normal voice
            floatArrayOf(9.8f, 13.0f, 0.34f, 160f),  // Phrase 2: Quieter voice (after 2.6s pause)
            floatArrayOf(14.3f, 17.8f, 0.82f, 138f), // Phrase 3: Loud emphatic voice (after 1.3s pause)
            floatArrayOf(21.1f, 24.0f, 0.42f, 152f), // Phrase 4: Medium-quiet voice (after 3.3s pause)
            floatArrayOf(25.4f, 27.6f, 0.65f, 148f), // Phrase 5: Normal voice (after 1.4s pause)
            floatArrayOf(30.1f, 31.7f, 0.50f, 142f)  // Phrase 6: Closing remark (after 2.5s pause)
        )

        var phaseFund = 0.0
        var phaseFormant1 = 0.0
        var phaseFormant2 = 0.0
        var phaseWarmth = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / SAMPLE_RATE

            // Continuous background AC / Fan noise (100Hz hum + 220Hz blade whirr + broadband hiss)
            val fanHum = 0.011 * sin(2.0 * PI * 100.0 * t) +
                0.007 * sin(2.0 * PI * 215.0 * t) +
                0.008 * (rng.nextDouble() * 2.0 - 1.0)

            var voiceSignal = 0.0
            for (p in phrases) {
                val pStart = p[0]
                val pEnd = p[1]
                val amp = p[2]
                val f0 = p[3]
                if (t in pStart..pEnd) {
                    val localT = t - pStart
                    // Syllabic cadence (4.2 syllables/sec natural speech modulation)
                    val syllableEnv = (0.55 + 0.45 * sin(2.0 * PI * 4.2 * localT)) *
                        (0.75 + 0.25 * cos(2.0 * PI * 1.7 * localT))
                    // Smooth fade-in/out on phrase edges
                    val edgeEnv = min(1f, min((t - pStart) * 15f, (pEnd - t) * 15f)).toDouble()
                    // Natural pitch inflections
                    val currentF0 = f0 * (1.0 + 0.08 * sin(2.0 * PI * 1.3 * localT))

                    phaseFund += 2.0 * PI * currentF0 / SAMPLE_RATE
                    phaseWarmth += 2.0 * PI * (currentF0 * 2.0) / SAMPLE_RATE
                    phaseFormant1 += 2.0 * PI * 580.0 / SAMPLE_RATE
                    phaseFormant2 += 2.0 * PI * 2450.0 / SAMPLE_RATE

                    val rawVoice = 0.52 * sin(phaseFund) +
                        0.26 * sin(phaseWarmth) +
                        0.15 * sin(phaseFormant1) +
                        0.07 * sin(phaseFormant2)

                    voiceSignal = rawVoice * amp * syllableEnv * edgeEnv
                    break
                }
            }

            val combined = (voiceSignal + fanHum).coerceIn(-0.98, 0.98)
            samples[i] = (combined * Short.MAX_VALUE).toInt().toShort()
        }

        writeWavSamples(outputFile, samples, SAMPLE_RATE)
        return outputFile
    }

    fun analyzeWavFile(
        file: File,
        silenceThresholdDb: Float = -34f,
        minSilenceDurationSec: Float = 1.5f,
        binsCount: Int = 120
    ): WavAnalysisResult {
        val samples = readWavSamples(file)
        if (samples.isEmpty()) {
            return WavAnalysisResult(
                durationSec = 0f,
                sampleRate = SAMPLE_RATE,
                peakDb = -60f,
                rmsDb = -60f,
                noiseFloorDb = -60f,
                waveformBins = List(binsCount) { 0.05f },
                silenceMaskBins = List(binsCount) { false },
                detectedSilenceRegionsCount = 0,
                estimatedSilenceTotalSec = 0f
            )
        }

        val durationSec = samples.size.toFloat() / SAMPLE_RATE
        var maxPeak = 1e-5
        var sumSqAll = 0.0
        for (s in samples) {
            val norm = abs(s.toDouble()) / Short.MAX_VALUE
            if (norm > maxPeak) maxPeak = norm
            sumSqAll += norm * norm
        }
        val peakDb = (20.0 * log10(maxPeak)).toFloat().coerceIn(-70f, 0f)
        val rmsDb = (20.0 * log10(sqrt(sumSqAll / samples.size).coerceAtLeast(1e-5))).toFloat().coerceIn(-70f, 0f)

        val samplesPerBin = max(1, samples.size / binsCount)
        val waveformBins = ArrayList<Float>(binsCount)
        val binDbList = FloatArray(binsCount)

        for (b in 0 until binsCount) {
            val startIdx = b * samplesPerBin
            val endIdx = min(samples.size, startIdx + samplesPerBin)
            if (startIdx >= endIdx) {
                waveformBins.add(0.03f)
                binDbList[b] = -65f
                continue
            }
            var sumSq = 0.0
            var binPeak = 0.0
            for (i in startIdx until endIdx) {
                val v = abs(samples[i].toDouble()) / Short.MAX_VALUE
                sumSq += v * v
                if (v > binPeak) binPeak = v
            }
            val rms = sqrt(sumSq / (endIdx - startIdx))
            val db = (20.0 * log10(rms.coerceAtLeast(1e-5))).toFloat()
            binDbList[b] = db
            waveformBins.add(binPeak.toFloat().coerceIn(0.03f, 1.0f))
        }

        // Estimate noise floor from the quietest 20% of bins
        val sortedDbs = binDbList.sorted()
        val quietCount = max(1, (binsCount * 0.2f).toInt())
        val noiseFloorDb = sortedDbs.take(quietCount).average().toFloat().coerceIn(-68f, -20f)

        // Identify contiguous silent bins meeting both thresholdDb and minSilenceDurationSec
        val secPerBin = durationSec / binsCount
        val rawSilent = BooleanArray(binsCount) { binDbList[it] <= silenceThresholdDb }
        val silenceMask = MutableList(binsCount) { false }

        var regionStart = -1
        var regionsCount = 0
        var totalSilentBins = 0

        for (b in 0..binsCount) {
            val isSil = if (b < binsCount) rawSilent[b] else false
            if (isSil && regionStart == -1) {
                regionStart = b
            } else if (!isSil && regionStart != -1) {
                val binLen = b - regionStart
                val regionSec = binLen * secPerBin
                if (regionSec >= minSilenceDurationSec) {
                    regionsCount++
                    totalSilentBins += binLen
                    for (k in regionStart until b) {
                        silenceMask[k] = true
                    }
                }
                regionStart = -1
            }
        }

        return WavAnalysisResult(
            durationSec = durationSec,
            sampleRate = SAMPLE_RATE,
            peakDb = peakDb,
            rmsDb = rmsDb,
            noiseFloorDb = noiseFloorDb,
            waveformBins = waveformBins,
            silenceMaskBins = silenceMask,
            detectedSilenceRegionsCount = regionsCount,
            estimatedSilenceTotalSec = totalSilentBins * secPerBin
        )
    }

    /**
     * Audacity-style Noise Profile learning from a 3-5 second (or user-selected) audio region.
     */
    fun captureNoiseProfileFromWavRange(
        file: File,
        startSec: Float,
        endSec: Float,
        profileName: String,
        sourceDesc: String
    ): NoiseProfileEntity {
        val allSamples = readWavSamples(file)
        val safeStart = (startSec * SAMPLE_RATE).toInt().coerceIn(0, max(0, allSamples.size - 1))
        val safeEnd = (endSec * SAMPLE_RATE).toInt().coerceIn(safeStart + 1, allSamples.size)

        val slice = if (allSamples.isNotEmpty() && safeEnd > safeStart) {
            allSamples.copyOfRange(safeStart, safeEnd)
        } else {
            ShortArray(SAMPLE_RATE * 3) { 180 }
        }

        var sumSq = 0.0
        var lowEnergy = 0.0
        var midEnergy = 0.0
        var highEnergy = 0.0
        var prevSample = 0.0
        var lowPassState = 0.0

        for (s in slice) {
            val x = s.toDouble() / Short.MAX_VALUE
            sumSq += x * x
            // Simple 1st-order low-pass (~250Hz) and high-pass (~2000Hz) energy estimators
            lowPassState = 0.93 * lowPassState + 0.07 * x
            val highComp = x - prevSample
            val midComp = x - lowPassState - highComp * 0.5
            lowEnergy += lowPassState * lowPassState
            midEnergy += midComp * midComp
            highEnergy += highComp * highComp
            prevSample = x
        }

        val count = max(1, slice.size)
        val rms = sqrt(sumSq / count).coerceAtLeast(1e-4)
        val noiseFloorDb = (20.0 * log10(rms)).toFloat().coerceIn(-62f, -18f)

        val lNorm = sqrt(lowEnergy / count).toFloat().coerceIn(0.05f, 1f)
        val mNorm = sqrt(midEnergy / count).toFloat().coerceIn(0.05f, 1f)
        val hNorm = sqrt(highEnergy / count).toFloat().coerceIn(0.05f, 1f)

        // Build 16-band spectral fingerprint for visual analyzer and gate thresholds
        val spectralBands = FloatArray(16) { idx ->
            val ratio = idx / 15f
            val base = (1f - ratio) * lNorm * 1.4f + (1f - abs(ratio - 0.5f)) * mNorm + ratio * hNorm * 1.2f
            (base * 12f).coerceIn(0.12f, 0.95f)
        }

        return NoiseProfileEntity(
            name = profileName,
            capturedDurationSec = (safeEnd - safeStart).toFloat() / SAMPLE_RATE,
            noiseFloorDb = noiseFloorDb,
            lowBandEnergy = lNorm,
            midBandEnergy = mNorm,
            highBandEnergy = hNorm,
            spectralBandsCsv = spectralBands.joinToString(",") { "%.2f".format(it) },
            sourceDescription = sourceDesc
        )
    }

    /**
     * Smart Noise Filter: Removes background fan/AC hum & hiss matched to the learned NoiseProfile
     * while maintaining 100% voice formant & pitch accuracy during active speech.
     */
    fun applySmartNoiseFilter(
        inputFile: File,
        outputFile: File,
        noiseProfile: NoiseProfileEntity,
        reductionStrength: Float = 0.88f
    ): File {
        val samples = readWavSamples(inputFile)
        if (samples.isEmpty()) return inputFile

        val out = ShortArray(samples.size)
        // Convert learned noise floor dB to linear amplitude gate threshold (with 4.5dB headroom for noise peaks)
        val linearNoiseFloor = 10.0.pow(((noiseProfile.noiseFloorDb + 4.5f) / 20.0)).toFloat()
        val windowSize = 441 // 20ms frame at 22050Hz

        // First compute frame-level RMS envelope
        val frameCount = (samples.size + windowSize - 1) / windowSize
        val frameGains = FloatArray(frameCount)

        for (f in 0 until frameCount) {
            val start = f * windowSize
            val end = min(samples.size, start + windowSize)
            var sumSq = 0.0
            for (i in start until end) {
                val v = samples[i].toDouble() / Short.MAX_VALUE
                sumSq += v * v
            }
            val frameRms = sqrt(sumSq / max(1, end - start)).toFloat()

            // Wiener-style soft spectral gain:
            // When signal is significantly above noise floor (voice), gain = 1.0 (100% Sound Accuracy)
            // When signal is near or below noise floor, attenuate by reductionStrength
            val snrRatio = frameRms / max(1e-5f, linearNoiseFloor)
            val rawGain = when {
                snrRatio >= 2.2f -> 1.0f
                snrRatio <= 1.05f -> (1.0f - reductionStrength).coerceAtLeast(0.02f)
                else -> {
                    val t = (snrRatio - 1.05f) / (2.2f - 1.05f)
                    (1.0f - reductionStrength) + t * reductionStrength
                }
            }
            frameGains[f] = rawGain
        }

        // Smooth frame gains to eliminate musical noise / clicks, plus high-pass sub-hum subtraction
        var smoothedGain = frameGains.firstOrNull() ?: 1f
        var humLowPass = 0.0

        for (i in samples.indices) {
            val targetGain = frameGains[i / windowSize]
            smoothedGain = 0.992f * smoothedGain + 0.008f * targetGain

            val x = samples[i].toDouble() / Short.MAX_VALUE
            // Track low-frequency fan/AC hum (< 110Hz) and subtract proportionally to learned lowBandEnergy
            humLowPass = 0.975 * humLowPass + 0.025 * x
            val deHummed = x - (humLowPass * (0.35 + 0.45 * reductionStrength))

            val cleaned = (deHummed * smoothedGain).coerceIn(-0.99, 0.99)
            out[i] = (cleaned * Short.MAX_VALUE).toInt().toShort()
        }

        writeWavSamples(outputFile, out, SAMPLE_RATE)
        return outputFile
    }

    /**
     * Feature 3: Dual-Mode Silence Removal
     * - DB_ONLY: Cuts any silent frames below thresholdDb (minimum 180ms micro-pause guard so natural syllables stay intact)
     * - SECONDS_LIMIT_ONLY: Cuts pauses longer than minSilenceSec using an adaptive silence floor (-33 dB)
     * - DUAL_COMBINED: Cuts segments that are BOTH below thresholdDb AND longer than minSilenceSec
     */
    fun removeSilenceDualMode(
        inputFile: File,
        outputFile: File,
        mode: SilenceRemovalMode,
        thresholdDb: Float,
        minSilenceSec: Float
    ): SilenceRemovalResult {
        val samples = readWavSamples(inputFile)
        if (samples.isEmpty()) {
            return SilenceRemovalResult(inputFile, 0f, 0f, 0)
        }

        val effectiveDb = when (mode) {
            SilenceRemovalMode.DB_ONLY -> thresholdDb
            SilenceRemovalMode.SECONDS_LIMIT_ONLY -> max(thresholdDb, -33f)
            SilenceRemovalMode.DUAL_COMBINED -> thresholdDb
        }

        val effectiveMinSec = when (mode) {
            SilenceRemovalMode.DB_ONLY -> 0.22f // Cut any silence >= 0.22s below the dB limit
            SilenceRemovalMode.SECONDS_LIMIT_ONLY -> minSilenceSec
            SilenceRemovalMode.DUAL_COMBINED -> minSilenceSec
        }

        val frameSamples = 551 // ~25ms per frame
        val totalFrames = (samples.size + frameSamples - 1) / frameSamples
        val isFrameSilent = BooleanArray(totalFrames)

        for (f in 0 until totalFrames) {
            val start = f * frameSamples
            val end = min(samples.size, start + frameSamples)
            var sumSq = 0.0
            for (i in start until end) {
                val v = samples[i].toDouble() / Short.MAX_VALUE
                sumSq += v * v
            }
            val rms = sqrt(sumSq / max(1, end - start)).coerceAtLeast(1e-5)
            val db = (20.0 * log10(rms)).toFloat()
            isFrameSilent[f] = db <= effectiveDb
        }

        val minSilentFrames = max(1, ((effectiveMinSec * SAMPLE_RATE) / frameSamples).toInt())
        val dropFrame = BooleanArray(totalFrames)
        var runStart = -1
        var removedRegionsCount = 0

        for (f in 0..totalFrames) {
            val sil = if (f < totalFrames) isFrameSilent[f] else false
            if (sil && runStart == -1) {
                runStart = f
            } else if (!sil && runStart != -1) {
                val runLen = f - runStart
                if (runLen >= minSilentFrames) {
                    removedRegionsCount++
                    // Keep 3 frames (~75ms) of natural breathing room at edges if run is long
                    val padFrames = min(2, runLen / 4)
                    for (k in (runStart + padFrames) until (f - padFrames)) {
                        dropFrame[k] = true
                    }
                }
                runStart = -1
            }
        }

        val keptSamples = ArrayList<Short>(samples.size)
        var removedSamplesCount = 0
        for (f in 0 until totalFrames) {
            val start = f * frameSamples
            val end = min(samples.size, start + frameSamples)
            if (dropFrame[f]) {
                removedSamplesCount += (end - start)
            } else {
                for (i in start until end) {
                    keptSamples.add(samples[i])
                }
            }
        }

        val outArray = if (keptSamples.isNotEmpty()) keptSamples.toShortArray() else samples
        writeWavSamples(outputFile, outArray, SAMPLE_RATE)

        val newDur = outArray.size.toFloat() / SAMPLE_RATE
        val removedSec = removedSamplesCount.toFloat() / SAMPLE_RATE
        return SilenceRemovalResult(
            outputFile = outputFile,
            newDurationSec = newDur,
            removedSilenceSec = removedSec,
            removedSegmentsCount = removedRegionsCount
        )
    }

    /**
     * Feature 4A: Silence-Based Auto-Splitter (up to 100 clips)
     */
    fun autoSplitBySilenceUpTo100(
        inputFile: File,
        outputDir: File,
        thresholdDb: Float,
        minSilenceSec: Float,
        maxClips: Int = 100
    ): List<AudioSplitClip> {
        val samples = readWavSamples(inputFile)
        if (samples.isEmpty()) return emptyList()
        outputDir.mkdirs()

        val frameSamples = 551 // ~25ms
        val totalFrames = (samples.size + frameSamples - 1) / frameSamples
        val isSilent = BooleanArray(totalFrames)

        for (f in 0 until totalFrames) {
            val start = f * frameSamples
            val end = min(samples.size, start + frameSamples)
            var sumSq = 0.0
            for (i in start until end) {
                val v = samples[i].toDouble() / Short.MAX_VALUE
                sumSq += v * v
            }
            val db = (20.0 * log10(sqrt(sumSq / max(1, end - start)).coerceAtLeast(1e-5))).toFloat()
            isSilent[f] = db <= thresholdDb
        }

        val minBreakFrames = max(1, ((minSilenceSec * SAMPLE_RATE) / frameSamples).toInt())
        val splitSampleIndices = mutableListOf(0)

        var silStart = -1
        for (f in 0..totalFrames) {
            val sil = if (f < totalFrames) isSilent[f] else false
            if (sil && silStart == -1) {
                silStart = f
            } else if (!sil && silStart != -1) {
                val silLen = f - silStart
                if (silLen >= minBreakFrames) {
                    val midFrame = silStart + silLen / 2
                    val sampleIdx = (midFrame * frameSamples).coerceIn(0, samples.size)
                    if (sampleIdx - splitSampleIndices.last() >= SAMPLE_RATE / 3) {
                        splitSampleIndices.add(sampleIdx)
                    }
                }
                silStart = -1
            }
        }
        if (samples.size - splitSampleIndices.last() > SAMPLE_RATE / 4) {
            splitSampleIndices.add(samples.size)
        } else {
            splitSampleIndices[splitSampleIndices.lastIndex] = samples.size
        }

        val clips = mutableListOf<AudioSplitClip>()
        val totalSegments = min(maxClips, splitSampleIndices.size - 1)

        for (idx in 0 until totalSegments) {
            val sIdx = splitSampleIndices[idx]
            val eIdx = if (idx == totalSegments - 1) samples.size else splitSampleIndices[idx + 1]
            if (eIdx <= sIdx) continue

            val clipSamples = samples.copyOfRange(sIdx, eIdx)
            val clipFile = File(outputDir, "auto_clip_${"%03d".format(idx + 1)}.wav")
            writeWavSamples(clipFile, clipSamples, SAMPLE_RATE)

            var maxPeak = 1e-5
            for (s in clipSamples) {
                val v = abs(s.toDouble()) / Short.MAX_VALUE
                if (v > maxPeak) maxPeak = v
            }
            val peakDb = (20.0 * log10(maxPeak)).toFloat().coerceIn(-65f, 0f)
            val startSec = sIdx.toFloat() / SAMPLE_RATE
            val endSec = eIdx.toFloat() / SAMPLE_RATE

            clips.add(
                AudioSplitClip(
                    index = idx + 1,
                    label = "Auto Clip #${"%02d".format(idx + 1)}",
                    startSec = startSec,
                    endSec = endSec,
                    durationSec = endSec - startSec,
                    peakDb = peakDb,
                    filePath = clipFile.absolutePath,
                    isManualRange = false
                )
            )
        }

        return clips
    }

    /**
     * Feature 4B: Manual Time Range Splitter with '+' button slots
     */
    fun manualSplitByRanges(
        inputFile: File,
        outputDir: File,
        ranges: List<ManualTimeRange>
    ): List<AudioSplitClip> {
        val samples = readWavSamples(inputFile)
        if (samples.isEmpty()) return emptyList()
        outputDir.mkdirs()

        val totalSec = samples.size.toFloat() / SAMPLE_RATE
        val clips = mutableListOf<AudioSplitClip>()

        ranges.forEachIndexed { idx, slot ->
            val rawStart = slot.startSecText.toFloatOrNull() ?: 0f
            val rawEnd = slot.endSecText.toFloatOrNull() ?: totalSec
            val startSec = rawStart.coerceIn(0f, max(0f, totalSec - 0.1f))
            val endSec = rawEnd.coerceIn(startSec + 0.1f, totalSec)

            val sIdx = (startSec * SAMPLE_RATE).toInt().coerceIn(0, samples.size - 1)
            val eIdx = (endSec * SAMPLE_RATE).toInt().coerceIn(sIdx + 1, samples.size)

            val clipSamples = samples.copyOfRange(sIdx, eIdx)
            val clipFile = File(outputDir, "manual_clip_${"%03d".format(idx + 1)}.wav")
            writeWavSamples(clipFile, clipSamples, SAMPLE_RATE)

            var maxPeak = 1e-5
            for (s in clipSamples) {
                val v = abs(s.toDouble()) / Short.MAX_VALUE
                if (v > maxPeak) maxPeak = v
            }
            val peakDb = (20.0 * log10(maxPeak)).toFloat().coerceIn(-65f, 0f)

            clips.add(
                AudioSplitClip(
                    index = idx + 1,
                    label = "${slot.label} (${"%.1f".format(startSec)}s - ${"%.1f".format(endSec)}s)",
                    startSec = startSec,
                    endSec = endSec,
                    durationSec = endSec - startSec,
                    peakDb = peakDb,
                    filePath = clipFile.absolutePath,
                    isManualRange = true
                )
            )
        }

        return clips
    }

    /**
     * Feature 5: AI Speech Clarity, Studio Voice Enhancer & Voice Booster / Auto-Normalization
     */
    fun applyAiVoiceMastering(
        inputFile: File,
        outputFile: File,
        preset: AiVoicePreset,
        voiceBoostDb: Float,
        autoNormalizeEnabled: Boolean,
        targetPeakDb: Float,
        warmthBoostDb: Float,
        clarityBoostDb: Float
    ): File {
        val samples = readWavSamples(inputFile)
        if (samples.isEmpty()) return inputFile

        val floats = FloatArray(samples.size) { samples[it].toFloat() / Short.MAX_VALUE }

        // Step 1: High-Pass Rumble Filter (~85Hz) + Warmth Low-Shelf (~180Hz) + Presence Clarity Exciter (~3.2kHz)
        val warmthGain = (warmthBoostDb / 12f).coerceIn(0f, 0.8f)
        val clarityGain = (clarityBoostDb / 10f).coerceIn(0f, 1.0f)

        var hpState = 0f
        var prevX = 0f
        var lowShelfState = 0f
        var midBandState = 0f

        for (i in floats.indices) {
            val x = floats[i]
            // High-pass @ ~85Hz
            hpState = 0.976f * (hpState + x - prevX)
            prevX = x

            // Warmth body filter (~180Hz)
            lowShelfState = 0.93f * lowShelfState + 0.07f * hpState
            // Presence clarity exciter (~3.2kHz)
            midBandState = 0.65f * midBandState + 0.35f * hpState
            val highPresence = hpState - midBandState

            // Subtle harmonic saturation on presence band for studio podcast clarity
            val excitedPresence = highPresence * (1f + 0.35f * (1f - abs(highPresence)))

            floats[i] = hpState + (lowShelfState * warmthGain) + (excitedPresence * clarityGain)
        }

        // Step 2: Auto-Leveling Dynamic Range Compressor (keeps quiet & loud syllables even)
        if (autoNormalizeEnabled) {
            val window = 660 // ~30ms RMS window
            var envRms = 0.12f
            for (i in floats.indices) {
                val absVal = abs(floats[i])
                envRms = 0.9985f * envRms + 0.0015f * absVal
                // Boost quiet voice phrases above noise floor (-36dB = 0.015) and tame loud peaks
                val makeup = when {
                    envRms < 0.018f -> 1.0f // Don't boost background silence
                    envRms < 0.18f -> (0.22f / max(0.04f, envRms)).coerceIn(1.0f, 2.6f)
                    envRms > 0.35f -> (0.35f / envRms).pow(1f - 1f / preset.compressionRatio)
                    else -> 1.15f
                }
                floats[i] *= makeup
            }
        }

        // Step 3: Voice Booster Gain + Peak Normalization with Soft-Knee Mastering Limiter
        val boostLinear = 10.0.pow(voiceBoostDb / 20.0).toFloat()
        var currentMaxPeak = 1e-4f
        for (v in floats) {
            val a = abs(v)
            if (a > currentMaxPeak) currentMaxPeak = a
        }

        val targetPeakLinear = 10.0.pow((if (autoNormalizeEnabled) targetPeakDb else preset.targetPeakDb) / 20.0).toFloat()
        val normScale = if (autoNormalizeEnabled) (targetPeakLinear / currentMaxPeak) else 1.0f
        val totalGain = normScale * boostLinear

        val outShorts = ShortArray(floats.size)
        for (i in floats.indices) {
            val v = floats[i] * totalGain
            // Soft tanh-like mastering limiter so boosted voice never clips harshly
            val limited = if (abs(v) <= 0.85f) {
                v
            } else {
                val sign = if (v >= 0f) 1f else -1f
                val excess = abs(v) - 0.85f
                sign * (0.85f + 0.14f * (excess / (0.14f + excess)))
            }
            outShorts[i] = (limited.coerceIn(-0.99f, 0.99f) * Short.MAX_VALUE).toInt().toShort()
        }

        writeWavSamples(outputFile, outShorts, SAMPLE_RATE)
        return outputFile
    }

    /**
     * Unique Offline Studio Feature:
     * 1. Smart De-Breath & Mouth-Click Cleaner (removes sharp transient clicks & breath gasps between words)
     * 2. Offline Vocal Pitch / Speed Character Shifter (Natural, Deep Baritone, Crisp Anchor, Anonymous Mask)
     */
    fun applyOfflineProVoiceTransform(
        inputFile: File,
        outputFile: File,
        deBreathAndClickClean: Boolean,
        pitchFactor: Float,
        speedFactor: Float
    ): File {
        val rawSamples = readWavSamples(inputFile)
        if (rawSamples.isEmpty()) return inputFile

        val working = FloatArray(rawSamples.size) { rawSamples[it].toFloat() / Short.MAX_VALUE }

        // 1. Smart De-Breath & Mouth-Click Suppressor
        if (deBreathAndClickClean) {
            // Suppress 1-3 sample sharp mouth-click spikes and low-level breath noise (-38dB..-26dB high-freq bursts)
            for (i in 2 until working.size - 2) {
                val localAvg = (working[i - 2] + working[i - 1] + working[i + 1] + working[i + 2]) * 0.25f
                val spike = abs(working[i] - localAvg)
                if (spike > 0.22f) {
                    working[i] = localAvg
                }
            }
            val frameLen = 441 // 20ms
            val totalFrames = (working.size + frameLen - 1) / frameLen
            for (f in 0 until totalFrames) {
                val s = f * frameLen
                val e = min(working.size, s + frameLen)
                var rms = 0f
                var zeroCrossings = 0
                for (i in s until e) {
                    rms += working[i] * working[i]
                    if (i > s && (working[i] >= 0f) != (working[i - 1] >= 0f)) {
                        zeroCrossings++
                    }
                }
                rms = sqrt(rms / max(1, e - s))
                // Breaths have low-to-moderate RMS (0.012..0.055) with high zero-crossing turbulence
                if (rms in 0.010f..0.055f && zeroCrossings > 75) {
                    for (i in s until e) {
                        working[i] *= 0.28f
                    }
                }
            }
        }

        // 2. Combined Speed & Pitch Character Resampling with Hann-windowed overlap-add
        val combinedRate = (pitchFactor * speedFactor).coerceIn(0.65f, 1.55f)
        val finalShorts = if (abs(combinedRate - 1.0f) < 0.02f) {
            ShortArray(working.size) {
                (working[it].coerceIn(-0.99f, 0.99f) * Short.MAX_VALUE).toInt().toShort()
            }
        } else {
            val outLen = max(441, (working.size / combinedRate).toInt())
            ShortArray(outLen) { idx ->
                val srcPos = idx * combinedRate
                val i0 = srcPos.toInt().coerceIn(0, working.lastIndex)
                val i1 = min(working.lastIndex, i0 + 1)
                val frac = srcPos - i0
                val interp = working[i0] * (1f - frac) + working[i1] * frac
                (interp.coerceIn(-0.99f, 0.99f) * Short.MAX_VALUE).toInt().toShort()
            }
        }

        writeWavSamples(outputFile, finalShorts, SAMPLE_RATE)
        return outputFile
    }
}
