package com.example

import com.example.audio.AudioDspEngine
import com.example.data.AiVoicePreset
import com.example.data.ManualTimeRange
import com.example.data.SilenceRemovalMode
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun audioDspEngine_noiseProfile_silenceRemoval_and_splitter_work() {
        val tempDir = Files.createTempDirectory("sonicforge_test").toFile()
        val demoWav = File(tempDir, "demo.wav")
        AudioDspEngine.generateDemoPodcastWav(demoWav, durationSec = 16)
        assertTrue(demoWav.exists() && demoWav.length() > 1000L)

        val profile = AudioDspEngine.captureNoiseProfileFromWavRange(
            file = demoWav,
            startSec = 0f,
            endSec = 3f,
            profileName = "Test Fan Noise",
            sourceDesc = "Unit Test"
        )
        assertTrue(profile.noiseFloorDb < -15f)

        val denoisedWav = File(tempDir, "denoised.wav")
        AudioDspEngine.applySmartNoiseFilter(demoWav, denoisedWav, profile, 0.85f)
        assertTrue(denoisedWav.exists() && denoisedWav.length() > 1000L)

        val trimmedWav = File(tempDir, "trimmed.wav")
        val trimRes = AudioDspEngine.removeSilenceDualMode(
            inputFile = demoWav,
            outputFile = trimmedWav,
            mode = SilenceRemovalMode.DUAL_COMBINED,
            thresholdDb = -30f,
            minSilenceSec = 1.0f
        )
        assertTrue(trimRes.outputFile.exists())

        val manualClips = AudioDspEngine.manualSplitByRanges(
            inputFile = demoWav,
            outputDir = File(tempDir, "manual_splits"),
            ranges = listOf(
                ManualTimeRange(1L, "1", "6", "Clip 1"),
                ManualTimeRange(2L, "6", "12", "Clip 2")
            )
        )
        assertTrue(manualClips.size == 2)

        val masteredWav = File(tempDir, "mastered.wav")
        AudioDspEngine.applyAiVoiceMastering(
            inputFile = demoWav,
            outputFile = masteredWav,
            preset = AiVoicePreset.CRYSTAL_PODCAST,
            voiceBoostDb = 3.0f,
            autoNormalizeEnabled = true,
            targetPeakDb = -1.0f,
            warmthBoostDb = 3.0f,
            clarityBoostDb = 4.5f
        )
        assertTrue(masteredWav.exists() && masteredWav.length() > 1000L)
        tempDir.deleteRecursively()
    }
}
