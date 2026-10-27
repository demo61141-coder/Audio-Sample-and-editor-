package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppLanguage
import com.example.data.tr
import com.example.ui.StudioTab
import com.example.ui.StudioViewModel
import com.example.ui.components.ActiveTrackTransportBar
import com.example.ui.components.SettingsAndFeatureGuideDialog
import com.example.ui.screens.EditorSplitterScreen
import com.example.ui.screens.EffectsScreen
import com.example.ui.screens.RecorderScreen
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SonicForgeStudioApp()
            }
        }
    }
}

@Composable
fun SonicForgeStudioApp(viewModel: StudioViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedTracks by viewModel.savedTracks.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lang = uiState.language

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        viewModel.toggleRecording(hasMicPermission = granted)
    }

    // Zero-permission Gallery Audio Picker (MP3, WAV, AAC, M4A, OGG)
    val galleryAudioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importMediaFromGalleryUri(uri = uri, isVideo = false)
        }
    }

    // Zero-permission Gallery Video Picker (MP4, MKV, MOV) for Video Noise & Silence Cleaning
    val galleryVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importMediaFromGalleryUri(uri = uri, isVideo = true)
        }
    }

    if (uiState.currentTab != StudioTab.RECORDER) {
        BackHandler {
            viewModel.selectTab(StudioTab.RECORDER)
        }
    }

    if (uiState.showSettingsGuideDialog) {
        SettingsAndFeatureGuideDialog(
            language = lang,
            onSelectLanguage = viewModel::setLanguage,
            onDismiss = { viewModel.toggleSettingsGuideDialog(false) }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            StudioTopHeader(
                language = lang,
                onToggleLanguage = viewModel::toggleLanguage,
                onOpenSettingsGuide = { viewModel.toggleSettingsGuideDialog(true) }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = StudioSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = uiState.currentTab == StudioTab.RECORDER,
                    onClick = { viewModel.selectTab(StudioTab.RECORDER) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == StudioTab.RECORDER) Icons.Filled.Mic else Icons.Outlined.Mic,
                            contentDescription = tr(lang, "রেকর্ডার ও ইনপুট", "Recorder & Import")
                        )
                    },
                    label = { Text(tr(lang, "১. রেকর্ড/ইনপুট", "1. Record/Import")) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBg,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = ElectricCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_recorder")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == StudioTab.EDITOR,
                    onClick = { viewModel.selectTab(StudioTab.EDITOR) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == StudioTab.EDITOR) Icons.Filled.ContentCut else Icons.Outlined.ContentCut,
                            contentDescription = tr(lang, "WAV কাট ও স্প্লিট", "WAV Cut & Split")
                        )
                    },
                    label = { Text(tr(lang, "২. WAV কাট/স্প্লিট", "2. WAV Cut/Split")) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBg,
                        selectedTextColor = AmberGold,
                        indicatorColor = AmberGold,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_editor")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == StudioTab.EFFECTS,
                    onClick = { viewModel.selectTab(StudioTab.EFFECTS) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == StudioTab.EFFECTS) Icons.Filled.Equalizer else Icons.Outlined.Equalizer,
                            contentDescription = tr(lang, "AI ভয়েস ও এক্সপোর্ট", "AI Voice & Export")
                        )
                    },
                    label = { Text(tr(lang, "৩. AI ও এক্সপোর্ট", "3. AI & Export")) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBg,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = ElectricCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_effects")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ActiveTrackTransportBar(
                language = lang,
                track = uiState.activeTrack,
                isPlaying = uiState.isPlayingMainTrack,
                playbackSec = uiState.playbackPositionSec,
                statusMessage = uiState.statusBannerMessage,
                onTogglePlay = { viewModel.toggleMainTrackPlayback() },
                onImportAudio = { galleryAudioPickerLauncher.launch("audio/*") },
                onImportVideo = { galleryVideoPickerLauncher.launch("video/*") },
                onGenerateDemo = { viewModel.generateFreshDemoTrack(isVideoDemo = false) }
            )

            Box(modifier = Modifier.weight(1f)) {
                when (uiState.currentTab) {
                    StudioTab.RECORDER -> {
                        RecorderScreen(
                            uiState = uiState,
                            savedTracks = savedTracks,
                            onSetNoiseCaptureDuration = viewModel::setNoiseCaptureTargetSec,
                            onCaptureNoisePrint = viewModel::captureAmbientNoisePrint,
                            onToggleSmartNoiseFilter = viewModel::toggleSmartNoiseFilter,
                            onNoiseStrengthChange = viewModel::setNoiseReductionStrength,
                            onToggleSilenceSkip = viewModel::toggleSilenceSkipRecording,
                            onSilenceDbChange = viewModel::updateSilenceDbThreshold,
                            onSilenceLimitSecChange = viewModel::updateSilenceDurationLimitSec,
                            onToggleRecord = {
                                if (uiState.isRecording || hasMicPermission) {
                                    viewModel.toggleRecording(hasMicPermission = hasMicPermission)
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onImportAudioFromGallery = { galleryAudioPickerLauncher.launch("audio/*") },
                            onImportVideoFromGallery = { galleryVideoPickerLauncher.launch("video/*") },
                            onGenerateVideoDemo = { viewModel.generateFreshDemoTrack(isVideoDemo = true) },
                            onSelectTrack = viewModel::selectTrack
                        )
                    }

                    StudioTab.EDITOR -> {
                        EditorSplitterScreen(
                            uiState = uiState,
                            onSilenceModeChange = viewModel::setSilenceRemovalMode,
                            onSilenceDbChange = viewModel::updateSilenceDbThreshold,
                            onSilenceLimitSecChange = viewModel::updateSilenceDurationLimitSec,
                            onUpdateSampleWindow = viewModel::updateSampleSelectionWindow,
                            onCaptureNoiseFromSelection = viewModel::captureNoiseFromWaveformSelection,
                            onOneClickNoiseRemoval = viewModel::applyOneClickNoiseRemoval,
                            onExecuteSilenceRemoval = viewModel::executeDualModeSilenceRemoval,
                            onUpdateVisualCutRange = viewModel::updateVisualCutSelection,
                            onAddVisualSelectionToCutList = viewModel::addVisualSelectionToCutRanges,
                            onRunAutoSilenceSplit = viewModel::runSilenceBasedAutoSplit,
                            onAddManualRangeSlot = viewModel::addManualTimeRangeSlot,
                            onUpdateManualRangeSlot = viewModel::updateManualTimeRangeSlot,
                            onRemoveManualRangeSlot = viewModel::removeManualTimeRangeSlot,
                            onRunManualRangeSplit = viewModel::runManualRangeSplit,
                            onSelectExportFormat = viewModel::selectExportFormat,
                            onExportSingleClipToGallery = viewModel::exportSingleClipToGallery,
                            onBatchExportAllClipsToGallery = viewModel::batchExportAllSplitClipsToGallery,
                            onToggleClipPlay = viewModel::toggleSplitClipPlayback
                        )
                    }

                    StudioTab.EFFECTS -> {
                        EffectsScreen(
                            uiState = uiState,
                            onSelectAiPreset = viewModel::selectAiVoicePreset,
                            onVoiceBoostChange = viewModel::updateVoiceBoostDb,
                            onToggleAutoNormalize = viewModel::toggleAutoNormalize,
                            onTargetPeakChange = viewModel::updateTargetPeakDb,
                            onWarmthAndClarityChange = viewModel::updateCustomWarmthAndClarity,
                            onToggleDeBreathAndClick = viewModel::toggleDeBreathAndClickClean,
                            onSelectVoiceCharacter = viewModel::selectOfflineVoiceCharacter,
                            onSpeechSpeedChange = viewModel::updateSpeechSpeedFactor,
                            onApplyMastering = viewModel::applyStudioEffectsAndMastering,
                            onSelectExportFormat = viewModel::selectExportFormat,
                            onSelectExportBitrate = viewModel::selectExportBitrate,
                            onExportTrackToGallery = viewModel::exportActiveTrackToGallery,
                            onCleanAndExportVideoToGallery = viewModel::cleanAndExportVideoToGallery
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioTopHeader(
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onOpenSettingsGuide: () -> Unit
) {
    Surface(
        color = StudioSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioSurfaceElevated)
                        .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = ElectricCyan
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "SonicForge Studio",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = tr(
                            language,
                            "১০০% অফলাইন অডিও/ভিডিও নয়েজ ও WAV স্প্লিটার",
                            "100% Offline Audio/Video Noise & WAV Splitter"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(ObsidianBg)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(50))
                        .clickable { onToggleLanguage() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("language_toggle_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "বাংলা",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (language == AppLanguage.BN) ElectricCyan else TextSecondary,
                        fontWeight = if (language == AppLanguage.BN) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = " | ",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "EN",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (language == AppLanguage.EN) ElectricCyan else TextSecondary,
                        fontWeight = if (language == AppLanguage.EN) FontWeight.Bold else FontWeight.Normal
                    )
                }

                IconButton(
                    onClick = onOpenSettingsGuide,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(StudioSurfaceElevated)
                        .border(1.dp, ElectricCyan.copy(alpha = 0.5f), CircleShape)
                        .testTag("top_settings_guide_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = tr(
                            language,
                            "সেটিংস ও ফিচার গাইড",
                            "Settings & Feature Guide"
                        ),
                        tint = ElectricCyan
                    )
                }
            }
        }
    }
}
