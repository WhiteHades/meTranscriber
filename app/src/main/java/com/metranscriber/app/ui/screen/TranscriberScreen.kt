package com.metranscriber.app.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.metranscriber.app.domain.model.TranscriptSegment
import com.metranscriber.app.domain.model.TranscriptionSession
import com.metranscriber.app.engine.TranscriberEngine
import com.metranscriber.app.theme.AccentCyan
import com.metranscriber.app.theme.CardSurface
import com.metranscriber.app.theme.DeepIndigo
import com.metranscriber.app.theme.DeepIndigoContainer
import com.metranscriber.app.theme.GoldenSun
import com.metranscriber.app.theme.GradientPrimary
import com.metranscriber.app.theme.GradientRecording
import com.metranscriber.app.theme.LightText
import com.metranscriber.app.theme.MutedText
import com.metranscriber.app.theme.PanelSurface
import com.metranscriber.app.theme.PanelSurfaceHigh
import com.metranscriber.app.theme.PremiumBackground
import com.metranscriber.app.theme.RecordingRed
import com.metranscriber.app.theme.SignalLime
import com.metranscriber.app.theme.SignalOrange
import com.metranscriber.app.theme.SignalStroke
import com.metranscriber.app.ui.viewmodel.RecordingState
import com.metranscriber.app.ui.viewmodel.TranscriberViewModel
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_WAV_IMPORT_BYTES = 25 * 1024 * 1024

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriberScreen(
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val viewModel: TranscriberViewModel = viewModel {
    TranscriberViewModel.createFactory(context)
  }

  val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
  val timerText by viewModel.timerText.collectAsStateWithLifecycle()
  val waveformAmplitudes by viewModel.waveformAmplitudes.collectAsStateWithLifecycle()
  val liveSegments by viewModel.liveSegments.collectAsStateWithLifecycle()
  val sessionsList by viewModel.sessionsList.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedSession by viewModel.selectedSession.collectAsStateWithLifecycle()
  val selectedSessionSegments by viewModel.selectedSessionSegments.collectAsStateWithLifecycle()
  val selectedSessionNotes by viewModel.selectedSessionNotes.collectAsStateWithLifecycle()
  val activeEngine by viewModel.activeEngine.collectAsStateWithLifecycle()
  val recordingError by viewModel.recordingError.collectAsStateWithLifecycle()
  val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()

  var currentTab by rememberSaveable { mutableIntStateOf(0) }
  var hasMicPermission by remember {
    mutableStateOf(context.hasRecordAudioPermission())
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { isGranted ->
      hasMicPermission = isGranted
      if (isGranted) {
        viewModel.startRecording()
      } else {
        Toast.makeText(context, "Microphone permission required for transcription", Toast.LENGTH_LONG).show()
      }
    }
  )

  val wavImportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
    onResult = { uri ->
      if (uri == null) return@rememberLauncherForActivityResult
      scope.launch {
        try {
          val bytes = withContext(Dispatchers.IO) {
            context.readSelectedAudioBytes(uri)
          }
          if (bytes == null) {
            snackbarHostState.showSnackbar("Could not read selected audio file")
          } else {
            viewModel.importWavFile(uri.lastPathSegment ?: "selected audio", bytes)
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          snackbarHostState.showSnackbar(e.message ?: "Could not read selected audio file")
        }
      }
    }
  )

  LaunchedEffect(recordingError) {
    recordingError?.let { message ->
      snackbarHostState.showSnackbar(
        message = message,
        actionLabel = "Dismiss",
        duration = SnackbarDuration.Long
      )
      viewModel.clearRecordingError()
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = PremiumBackground,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      SignalTopBar(
        activeTab = currentTab,
        onInfoClick = {
          Toast.makeText(context, "MeTranscriber v1.0.0, offline speech cockpit", Toast.LENGTH_SHORT).show()
        }
      )
    },
    bottomBar = {
      SignalNavigationBar(
        currentTab = currentTab,
        onTabSelected = { index ->
          currentTab = index
          if (index == 1) viewModel.loadSessions()
        }
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      SignalBackdrop(modifier = Modifier.matchParentSize())

      if (currentTab == 0) {
        TranscribeTab(
          recordingState = recordingState,
          timerText = timerText,
          waveformAmplitudes = waveformAmplitudes,
          liveSegments = liveSegments,
          activeEngine = activeEngine,
          isImporting = isImporting,
          onImportClick = {
            wavImportLauncher.launch(arrayOf("audio/wav", "audio/x-wav", "audio/wave", "audio/*"))
          },
          onRecordToggle = {
            if (!isImporting) {
              if (recordingState == RecordingState.IDLE) {
                hasMicPermission = context.hasRecordAudioPermission()
                if (hasMicPermission) {
                  viewModel.startRecording()
                } else {
                  permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
              } else {
                viewModel.stopRecording()
              }
            }
          }
        )
      } else {
        HistoryTab(
          sessionsList = sessionsList,
          searchQuery = searchQuery,
          onSearchQueryChange = { viewModel.setSearchQuery(it) },
          onSessionClick = { viewModel.selectSession(it) },
          onDeleteSession = { viewModel.deleteSession(it.id) }
        )
      }

      selectedSession?.let { session ->
        SessionDetailsDialog(
          session = session,
          segments = selectedSessionSegments,
          notes = selectedSessionNotes,
          onNotesChange = { viewModel.updateNotes(it) },
          onDismiss = { viewModel.selectSession(null) },
          onExportTxt = {
            val content = viewModel.exportSessionAsTxt(session)
            shareText(context, content, "text/plain")
          },
          onExportJson = {
            val content = viewModel.exportSessionAsJson(session, selectedSessionSegments)
            shareText(context, content, "application/json")
          },
          onExportSrt = {
            val content = viewModel.exportSessionAsSrt(selectedSessionSegments)
            shareText(context, content, "text/plain")
          }
        )
      }
    }
  }
}

private fun Context.readSelectedAudioBytes(uri: Uri): ByteArray? =
  contentResolver.openInputStream(uri)?.use { it.readBytesWithLimit(MAX_WAV_IMPORT_BYTES) }

private fun Context.hasRecordAudioPermission(): Boolean =
  ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

private fun InputStream.readBytesWithLimit(maxBytes: Int): ByteArray {
  val output = ByteArrayOutputStream()
  val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
  var totalBytes = 0
  while (true) {
    val read = read(buffer)
    if (read == -1) break
    totalBytes += read
    require(totalBytes <= maxBytes) { "Selected WAV file is too large. Choose a file under 25 MB." }
    output.write(buffer, 0, read)
  }
  return output.toByteArray()
}

@Composable
private fun SignalTopBar(
  activeTab: Int,
  onInfoClick: () -> Unit
) {
  Surface(
    color = PremiumBackground.copy(alpha = 0.98f),
    contentColor = LightText,
    shadowElevation = 0.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 18.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(GradientPrimary))
            .border(1.dp, AccentCyan.copy(alpha = 0.28f), RoundedCornerShape(16.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.GraphicEq, contentDescription = null, tint = LightText, modifier = Modifier.size(25.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "MeTranscriber",
            style = MaterialTheme.typography.titleLarge,
            color = LightText,
            maxLines = 1
          )
          Text(
            text = if (activeTab == 0) "Live signal capture" else "Transcript archive",
            style = MaterialTheme.typography.labelSmall,
            color = MutedText
          )
        }
      }

      IconButton(onClick = onInfoClick) {
        Icon(Icons.Default.Info, contentDescription = "Info", tint = AccentCyan)
      }
    }
  }
}

@Composable
private fun SignalNavigationBar(
  currentTab: Int,
  onTabSelected: (Int) -> Unit
) {
  Surface(
    color = PremiumBackground.copy(alpha = 0.96f),
    contentColor = LightText,
    border = BorderStroke(1.dp, SignalStroke.copy(alpha = 0.5f)),
    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    NavigationBar(
      containerColor = Color.Transparent,
      tonalElevation = 0.dp,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
      NavigationBarItem(
        selected = currentTab == 0,
        onClick = { onTabSelected(0) },
        label = { Text("Transcribe", fontWeight = FontWeight.Bold) },
        icon = { Icon(Icons.Default.Mic, contentDescription = "Transcribe") },
        colors = signalNavColors()
      )
      NavigationBarItem(
        selected = currentTab == 1,
        onClick = { onTabSelected(1) },
        label = { Text("History", fontWeight = FontWeight.Bold) },
        icon = { Icon(Icons.Default.History, contentDescription = "History") },
        colors = signalNavColors()
      )
    }
  }
}

@Composable
private fun signalNavColors() =
  NavigationBarItemDefaults.colors(
    selectedIconColor = LightText,
    selectedTextColor = LightText,
    indicatorColor = DeepIndigo.copy(alpha = 0.58f),
    unselectedIconColor = MutedText,
    unselectedTextColor = MutedText
  )

@Composable
private fun SignalBackdrop(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .background(
        Brush.radialGradient(
          colors = listOf(DeepIndigoContainer.copy(alpha = 0.95f), PremiumBackground),
          center = Offset(120f, 0f),
          radius = 760f
        )
      )
      .background(
        Brush.verticalGradient(
          colors = listOf(Color.Transparent, PremiumBackground.copy(alpha = 0.2f), PremiumBackground)
        )
      )
  ) {
    Canvas(modifier = Modifier.matchParentSize()) {
      val step = 38.dp.toPx()
      var x = 0f
      while (x < size.width) {
        drawLine(
          color = SignalStroke.copy(alpha = 0.16f),
          start = Offset(x, 0f),
          end = Offset(x, size.height),
          strokeWidth = 1f
        )
        x += step
      }
      var y = 0f
      while (y < size.height) {
        drawLine(
          color = SignalStroke.copy(alpha = 0.1f),
          start = Offset(0f, y),
          end = Offset(size.width, y),
          strokeWidth = 1f
        )
        y += step
      }
      drawCircle(
        color = AccentCyan.copy(alpha = 0.08f),
        radius = size.minDimension * 0.52f,
        center = Offset(size.width * 0.86f, size.height * 0.1f),
        style = Stroke(width = 1.5.dp.toPx())
      )
    }
  }
}

@Composable
fun TranscribeTab(
  recordingState: RecordingState,
  timerText: String,
  waveformAmplitudes: List<Float>,
  liveSegments: List<TranscriptSegment>,
  activeEngine: TranscriberEngine,
  isImporting: Boolean,
  onImportClick: () -> Unit,
  onRecordToggle: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 18.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    CaptureHeader(activeEngine = activeEngine, recordingState = recordingState, timerText = timerText)
    LiveTranscriptPanel(
      liveSegments = liveSegments,
      recordingState = recordingState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    )
    ControlDeck(
      recordingState = recordingState,
      timerText = timerText,
      waveformAmplitudes = waveformAmplitudes,
      isImporting = isImporting,
      onImportClick = onImportClick,
      onRecordToggle = onRecordToggle
    )
  }
}

@Composable
private fun CaptureHeader(
  activeEngine: TranscriberEngine,
  recordingState: RecordingState,
  timerText: String
) {
  val isReady = activeEngine.isModelDownloaded
  val status = when {
    recordingState == RecordingState.RECORDING -> "ARMED"
    isReady -> "READY"
    else -> "MODEL CHECK"
  }
  val statusColor = when {
    recordingState == RecordingState.RECORDING -> RecordingRed
    isReady -> SignalLime
    else -> GoldenSun
  }

  Surface(
    shape = RoundedCornerShape(28.dp),
    color = PanelSurface.copy(alpha = 0.94f),
    border = BorderStroke(1.dp, SignalStroke.copy(alpha = 0.7f)),
    shadowElevation = 6.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(18.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("CAPTURE SYSTEM", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = activeEngine.displayName,
          style = MaterialTheme.typography.titleLarge,
          color = LightText,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = if (recordingState == RecordingState.RECORDING) "Recording locally for $timerText" else "Private offline transcription, ready on device",
          style = MaterialTheme.typography.bodyMedium,
          color = MutedText,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      StatusPill(status = status, statusColor = statusColor, isReady = isReady)
    }
  }
}

@Composable
private fun StatusPill(
  status: String,
  statusColor: Color,
  isReady: Boolean
) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(statusColor.copy(alpha = 0.14f))
      .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
      .padding(horizontal = 12.dp, vertical = 9.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Storage,
      contentDescription = null,
      tint = statusColor,
      modifier = Modifier.size(17.dp)
    )
    Spacer(modifier = Modifier.width(7.dp))
    Text(text = status, style = MaterialTheme.typography.labelSmall, color = statusColor)
  }
}

@Composable
private fun LiveTranscriptPanel(
  liveSegments: List<TranscriptSegment>,
  recordingState: RecordingState,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(32.dp),
    color = CardSurface.copy(alpha = 0.82f),
    border = BorderStroke(1.dp, SignalStroke.copy(alpha = 0.62f)),
    modifier = modifier
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(PanelSurfaceHigh.copy(alpha = 0.74f), CardSurface.copy(alpha = 0.92f))
          )
        )
        .padding(16.dp)
    ) {
      if (liveSegments.isEmpty()) {
        EmptyCaptureState(recordingState = recordingState)
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          reverseLayout = true,
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(liveSegments.reversed()) { segment ->
            TranscriptBurst(segment = segment)
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyCaptureState(recordingState: RecordingState) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(132.dp)) {
      Canvas(modifier = Modifier.matchParentSize()) {
        drawCircle(DeepIndigo.copy(alpha = 0.3f), radius = size.minDimension * 0.48f)
        drawCircle(AccentCyan.copy(alpha = 0.22f), radius = size.minDimension * 0.36f, style = Stroke(2.dp.toPx()))
        drawCircle(RecordingRed.copy(alpha = 0.18f), radius = size.minDimension * 0.22f, style = Stroke(2.dp.toPx()))
      }
      Icon(
        imageVector = if (recordingState == RecordingState.RECORDING) Icons.Default.GraphicEq else Icons.Default.Mic,
        contentDescription = null,
        tint = LightText,
        modifier = Modifier.size(54.dp)
      )
    }
    Spacer(modifier = Modifier.height(18.dp))
    Text(
      text = if (recordingState == RecordingState.RECORDING) "Listening for the first words" else "Ready to Transcribe",
      style = MaterialTheme.typography.headlineMedium,
      color = LightText,
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text = "Start a live capture or import a WAV file. Audio stays local while the transcript takes shape.",
      style = MaterialTheme.typography.bodyMedium,
      color = MutedText,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth(0.86f)
    )
  }
}

@Composable
private fun TranscriptBurst(segment: TranscriptSegment) {
  Surface(
    shape = RoundedCornerShape(22.dp),
    color = PanelSurface.copy(alpha = 0.9f),
    border = BorderStroke(1.dp, SignalStroke.copy(alpha = 0.52f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.Top
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = formatTimestamp(segment.timestampMs),
          style = MaterialTheme.typography.labelSmall,
          color = AccentCyan
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
          modifier = Modifier
            .width(2.dp)
            .height(34.dp)
            .background(Brush.verticalGradient(listOf(AccentCyan, Color.Transparent)))
        )
      }
      Spacer(modifier = Modifier.width(13.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = segment.speaker ?: "Speaker 1",
          style = MaterialTheme.typography.labelSmall,
          color = GoldenSun
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
          text = segment.text,
          style = MaterialTheme.typography.bodyLarge,
          fontFamily = FontFamily.Monospace,
          color = LightText
        )
      }
    }
  }
}

@Composable
private fun ControlDeck(
  recordingState: RecordingState,
  timerText: String,
  waveformAmplitudes: List<Float>,
  isImporting: Boolean,
  onImportClick: () -> Unit,
  onRecordToggle: () -> Unit
) {
  val haptic = LocalHapticFeedback.current
  val isRecording = recordingState == RecordingState.RECORDING

  Surface(
    shape = RoundedCornerShape(32.dp),
    color = PanelSurfaceHigh.copy(alpha = 0.96f),
    border = BorderStroke(1.dp, if (isRecording) RecordingRed.copy(alpha = 0.55f) else SignalStroke.copy(alpha = 0.72f)),
    shadowElevation = 10.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      AnimatedVisibility(
        visible = isImporting,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = AccentCyan,
            trackColor = SignalStroke.copy(alpha = 0.35f)
          )
          Spacer(modifier = Modifier.height(7.dp))
          Text("Importing WAV audio", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("SIGNAL", style = MaterialTheme.typography.labelSmall, color = MutedText)
          Text(
            text = if (isRecording) timerText else "00:00",
            style = MaterialTheme.typography.displaySmall,
            color = if (isRecording) RecordingRed else LightText
          )
        }
        SignalBadge(text = if (isRecording) "LIVE" else "STANDBY", color = if (isRecording) RecordingRed else SignalLime)
      }

      SignalWaveform(
        amplitudes = waveformAmplitudes,
        isRecording = isRecording,
        modifier = Modifier
          .fillMaxWidth()
          .height(72.dp)
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = onImportClick,
          enabled = recordingState == RecordingState.IDLE && !isImporting,
          shape = RoundedCornerShape(18.dp),
          border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.42f)),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AccentCyan,
            disabledContentColor = MutedText.copy(alpha = 0.55f)
          ),
          modifier = Modifier.height(54.dp)
        ) {
          Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(19.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (isImporting) "Importing" else "Import WAV", fontWeight = FontWeight.Bold)
        }

        RecordingHaloButton(
          isRecording = isRecording,
          enabled = !isImporting,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onRecordToggle()
          }
        )
      }
    }
  }
}

@Composable
private fun SignalBadge(text: String, color: Color) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelSmall,
    color = color,
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(color.copy(alpha = 0.13f))
      .border(1.dp, color.copy(alpha = 0.42f), RoundedCornerShape(999.dp))
      .padding(horizontal = 12.dp, vertical = 7.dp)
  )
}

@Composable
private fun SignalWaveform(
  amplitudes: List<Float>,
  isRecording: Boolean,
  modifier: Modifier = Modifier
) {
  val idleBars = remember {
    listOf(0.18f, 0.38f, 0.22f, 0.55f, 0.32f, 0.72f, 0.26f, 0.5f, 0.2f, 0.43f, 0.28f, 0.64f)
  }

  Canvas(modifier = modifier) {
    val barWidth = 5.dp.toPx()
    val gap = 5.dp.toPx()
    val totalBars = (size.width / (barWidth + gap)).toInt().coerceAtLeast(1)
    val source = if (amplitudes.isNotEmpty()) amplitudes else idleBars
    val activeList = if (source.size > totalBars) source.takeLast(totalBars) else List(totalBars) { source[it % source.size] }
    val startX = (size.width - (activeList.size * (barWidth + gap))) / 2f
    val centerY = size.height / 2f

    drawLine(
      color = SignalStroke.copy(alpha = 0.7f),
      start = Offset(0f, centerY),
      end = Offset(size.width, centerY),
      strokeWidth = 1.dp.toPx()
    )

    activeList.forEachIndexed { index, amp ->
      val energy = amp.coerceIn(0.08f, 1f)
      val barHeight = size.height * energy * if (isRecording) 0.96f else 0.52f
      val x = startX + index * (barWidth + gap)
      val y = (size.height - barHeight) / 2f
      drawRoundRect(
        brush = if (isRecording) Brush.verticalGradient(GradientRecording) else SolidColor(AccentCyan.copy(alpha = 0.76f)),
        topLeft = Offset(x, y),
        size = Size(barWidth, barHeight),
        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
      )
    }
  }
}

@Composable
private fun RecordingHaloButton(
  isRecording: Boolean,
  enabled: Boolean,
  onClick: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "record-pulse")
  val scale by infiniteTransition.animateFloat(
    initialValue = 0.92f,
    targetValue = if (isRecording) 1.18f else 1f,
    animationSpec = infiniteRepeatable(
      animation = twistyTween(920),
      repeatMode = RepeatMode.Reverse
    ),
    label = "record-scale"
  )

  Box(contentAlignment = Alignment.Center, modifier = Modifier.size(94.dp)) {
    if (isRecording) {
      Box(
        modifier = Modifier
          .size(74.dp)
          .scale(scale)
          .clip(CircleShape)
          .background(RecordingRed.copy(alpha = 0.18f))
      )
    }
    Box(
      modifier = Modifier
        .size(70.dp)
        .clip(CircleShape)
        .background(Brush.linearGradient(if (isRecording) GradientRecording else GradientPrimary))
        .border(1.dp, LightText.copy(alpha = 0.24f), CircleShape)
        .clickable(enabled = enabled, onClick = onClick),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
        contentDescription = if (isRecording) "Stop recording" else "Start recording",
        tint = LightText,
        modifier = Modifier.size(32.dp)
      )
    }
  }
}

private fun twistyTween(duration: Int): TweenSpec<Float> = tween(
  durationMillis = duration,
  easing = FastOutSlowInEasing
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTab(
  sessionsList: List<TranscriptionSession>,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  onSessionClick: (TranscriptionSession) -> Unit,
  onDeleteSession: (TranscriptionSession) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 18.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Column {
      Text("ARCHIVE", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
      Text("Captured signals", style = MaterialTheme.typography.headlineMedium, color = LightText)
      Text(
        text = "Search, reopen, annotate, and export every saved transcript.",
        style = MaterialTheme.typography.bodyMedium,
        color = MutedText
      )
    }

    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchQueryChange,
      placeholder = { Text("Search sessions, notes or keywords", color = MutedText.copy(alpha = 0.72f)) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = AccentCyan) },
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      singleLine = true,
      colors = signalTextFieldColors()
    )

    if (sessionsList.isEmpty()) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.History, contentDescription = null, tint = AccentCyan.copy(alpha = 0.48f), modifier = Modifier.size(54.dp))
          Spacer(modifier = Modifier.height(16.dp))
          Text("No Sessions Found", style = MaterialTheme.typography.titleMedium, color = MutedText)
          Spacer(modifier = Modifier.height(6.dp))
          Text("Record or import audio to build the archive.", style = MaterialTheme.typography.bodyMedium, color = MutedText.copy(alpha = 0.72f))
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        itemsIndexed(sessionsList) { index, session ->
          SessionArchiveRow(
            index = index,
            session = session,
            onClick = { onSessionClick(session) },
            onDelete = { onDeleteSession(session) }
          )
        }
      }
    }
  }
}

@Composable
private fun signalTextFieldColors() =
  OutlinedTextFieldDefaults.colors(
    focusedTextColor = LightText,
    unfocusedTextColor = LightText,
    focusedBorderColor = AccentCyan,
    unfocusedBorderColor = SignalStroke.copy(alpha = 0.75f),
    focusedContainerColor = PanelSurface.copy(alpha = 0.9f),
    unfocusedContainerColor = PanelSurface.copy(alpha = 0.78f),
    cursorColor = AccentCyan,
    focusedPlaceholderColor = MutedText,
    unfocusedPlaceholderColor = MutedText
  )

@Composable
private fun SessionArchiveRow(
  index: Int,
  session: TranscriptionSession,
  onClick: () -> Unit,
  onDelete: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(26.dp),
    color = PanelSurface.copy(alpha = 0.94f),
    border = BorderStroke(1.dp, SignalStroke.copy(alpha = 0.58f)),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(15.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(52.dp)
          .clip(RoundedCornerShape(18.dp))
          .background(DeepIndigo.copy(alpha = 0.26f))
          .border(1.dp, AccentCyan.copy(alpha = 0.25f), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = (index + 1).toString().padStart(2, '0'),
          style = MaterialTheme.typography.labelLarge,
          color = AccentCyan
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = session.title,
            style = MaterialTheme.typography.titleMedium,
            color = LightText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )
          IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RecordingRed.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
          }
        }
        Text(
          text = session.rawText.ifBlank { "No transcript text saved" },
          style = MaterialTheme.typography.bodyMedium,
          color = MutedText,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          MetadataChip(text = formatSessionTime(session.createdAt), color = MutedText)
          MetadataChip(text = formatDuration(session.durationMs), color = AccentCyan)
          MetadataChip(text = "${session.wordCount} words", color = GoldenSun)
        }
      }
    }
  }
}

@Composable
private fun MetadataChip(text: String, color: Color) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelSmall,
    color = color,
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(color.copy(alpha = 0.1f))
      .padding(horizontal = 8.dp, vertical = 4.dp)
  )
}

@Composable
fun SessionDetailsDialog(
  session: TranscriptionSession,
  segments: List<TranscriptSegment>,
  notes: String,
  onNotesChange: (String) -> Unit,
  onDismiss: () -> Unit,
  onExportTxt: () -> Unit,
  onExportJson: () -> Unit,
  onExportSrt: () -> Unit
) {
  var isExporting by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth(0.95f)
      .fillMaxHeight(0.86f),
    containerColor = PanelSurfaceHigh,
    titleContentColor = LightText,
    textContentColor = LightText,
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close", color = AccentCyan, fontWeight = FontWeight.Bold)
      }
    },
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("TRANSCRIPT", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
          Text(
            text = session.title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        IconButton(onClick = { isExporting = true }) {
          Icon(Icons.Default.Share, contentDescription = "Export Options", tint = GoldenSun)
        }
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          MetadataChip(text = formatDuration(session.durationMs), color = AccentCyan)
          MetadataChip(text = "${session.wordCount} words", color = GoldenSun)
          MetadataChip(text = session.engineUsed, color = SignalLime)
        }

        Surface(
          shape = RoundedCornerShape(24.dp),
          color = CardSurface.copy(alpha = 0.72f),
          border = BorderStroke(1.dp, SignalStroke.copy(alpha = 0.58f)),
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            if (segments.isEmpty()) {
              item {
                Text(
                  text = session.rawText,
                  style = MaterialTheme.typography.bodyLarge,
                  lineHeight = 24.sp,
                  color = LightText
                )
              }
            } else {
              items(segments) { segment ->
                TranscriptBurst(segment = segment)
              }
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Session notes, auto-saved", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
          OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            placeholder = { Text("Add summary, action items or cleanup notes", color = MutedText.copy(alpha = 0.72f)) },
            modifier = Modifier
              .fillMaxWidth()
              .height(112.dp),
            shape = RoundedCornerShape(18.dp),
            colors = signalTextFieldColors()
          )
        }
      }
    }
  )

  if (isExporting) {
    ExportDialog(
      onDismiss = { isExporting = false },
      onExportTxt = {
        isExporting = false
        onExportTxt()
      },
      onExportJson = {
        isExporting = false
        onExportJson()
      },
      onExportSrt = {
        isExporting = false
        onExportSrt()
      }
    )
  }
}

@Composable
private fun ExportDialog(
  onDismiss: () -> Unit,
  onExportTxt: () -> Unit,
  onExportJson: () -> Unit,
  onExportSrt: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PanelSurfaceHigh,
    titleContentColor = LightText,
    textContentColor = LightText,
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = AccentCyan)
      }
    },
    title = { Text("Export Transcription", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        ExportListItem(
          title = "Plain Text (.txt)",
          icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = AccentCyan) },
          onClick = onExportTxt
        )
        HorizontalDivider(color = SignalStroke.copy(alpha = 0.5f))
        ExportListItem(
          title = "Structured Data (.json)",
          icon = { Icon(Icons.Default.Info, contentDescription = null, tint = GoldenSun) },
          onClick = onExportJson
        )
        HorizontalDivider(color = SignalStroke.copy(alpha = 0.5f))
        ExportListItem(
          title = "Subtitles (.srt)",
          icon = { Icon(Icons.Default.Check, contentDescription = null, tint = RecordingRed) },
          onClick = onExportSrt
        )
      }
    }
  )
}

@Composable
private fun ExportListItem(
  title: String,
  icon: @Composable () -> Unit,
  onClick: () -> Unit
) {
  ListItem(
    headlineContent = { Text(title, fontWeight = FontWeight.Bold, color = LightText) },
    leadingContent = icon,
    trailingContent = { Icon(Icons.Default.MoreVert, contentDescription = null, tint = MutedText) },
    colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
    modifier = Modifier.clickable(onClick = onClick)
  )
}

private fun formatTimestamp(timestampMs: Long): String {
  val totalSeconds = timestampMs / 1000
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return "%02d:%02d".format(minutes, seconds)
}

private fun formatDuration(durationMs: Long): String = formatTimestamp(durationMs)

private fun formatSessionTime(createdAt: Long): String =
  SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(createdAt))

private fun shareText(context: Context, text: String, mimeType: String) {
  val intent = Intent(Intent.ACTION_SEND).apply {
    type = mimeType
    putExtra(Intent.EXTRA_TEXT, text)
  }
  context.startActivity(Intent.createChooser(intent, "Share Transcription Export"))
}
