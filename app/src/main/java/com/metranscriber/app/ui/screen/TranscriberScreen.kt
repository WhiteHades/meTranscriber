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
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
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
import com.metranscriber.app.theme.*
import com.metranscriber.app.ui.viewmodel.TranscriberViewModel
import com.metranscriber.app.ui.viewmodel.RecordingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

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
    containerColor = MaterialTheme.colorScheme.background,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "MeTranscriber",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        actions = {
          IconButton(onClick = {
            Toast.makeText(context, "MeTranscriber v1.0.0 - Premium Offline STT", Toast.LENGTH_SHORT).show()
          }) {
            Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary)
          }
        }
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        modifier = Modifier.border(0.5.dp, MaterialTheme.colorScheme.surface.copy(alpha = 0.2f), RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp))
      ) {
        NavigationBarItem(
          selected = currentTab == 0,
          onClick = { currentTab = 0 },
          label = { Text("Transcribe", fontWeight = FontWeight.SemiBold) },
          icon = { Icon(Icons.Default.Mic, contentDescription = "Transcribe") }
        )
        NavigationBarItem(
          selected = currentTab == 1,
          onClick = {
            currentTab = 1
            viewModel.loadSessions()
          },
          label = { Text("History", fontWeight = FontWeight.SemiBold) },
          icon = { Icon(Icons.Default.History, contentDescription = "History") }
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              MaterialTheme.colorScheme.background,
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
          )
        )
        .padding(innerPadding)
    ) {
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

      // Detailed Bottom Sheet Dialog
      if (selectedSession != null) {
        SessionDetailsDialog(
          session = selectedSession!!,
          segments = selectedSessionSegments,
          notes = selectedSessionNotes,
          onNotesChange = { viewModel.updateNotes(it) },
          onDismiss = { viewModel.selectSession(null) },
          onExportTxt = {
            val content = viewModel.exportSessionAsTxt(selectedSession!!)
            shareText(context, content, "text/plain")
          },
          onExportJson = {
            val content = viewModel.exportSessionAsJson(selectedSession!!, selectedSessionSegments)
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
  val haptic = LocalHapticFeedback.current
  val engineStatus = if (activeEngine.isModelDownloaded) "Ready" else "Model check on start"
  val engineStatusColor = if (activeEngine.isModelDownloaded) AccentCyan else MaterialTheme.colorScheme.tertiary

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
      modifier = Modifier
        .fillMaxWidth()
        .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Active Engine",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
          )
          Text(
            text = activeEngine.displayName,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
        AssistChip(
          onClick = {},
          enabled = false,
          label = {
            Text(
              text = engineStatus,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          },
          leadingIcon = {
            Icon(
              imageVector = if (activeEngine.isModelDownloaded) Icons.Default.CheckCircle else Icons.Default.Storage,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
          },
          colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = engineStatusColor.copy(alpha = 0.12f),
            disabledLabelColor = engineStatusColor,
            disabledLeadingIconContentColor = engineStatusColor
          ),
          border = AssistChipDefaults.assistChipBorder(
            enabled = false,
            borderColor = engineStatusColor.copy(alpha = 0.28f),
            disabledBorderColor = engineStatusColor.copy(alpha = 0.28f)
          )
        )
      }
    }

    // Scrollable Transcription Feed or Empty State
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(vertical = 24.dp),
      contentAlignment = Alignment.Center
    ) {
      if (liveSegments.isEmpty() && recordingState == RecordingState.IDLE) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(24.dp)
        ) {
          Icon(
            Icons.Default.Mic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            modifier = Modifier.size(64.dp)
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "Ready to Transcribe",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Tap the microphone below to start live, offline speech recognition.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          reverseLayout = true
        ) {
          // Display newest transcription text at the bottom
          val reversedList = liveSegments.reversed()
          items(reversedList) { segment ->
            Card(
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = segment.speaker ?: "Speaker 1",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                  )
                  Text(
                    text = "${segment.timestampMs / 1000}s",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = segment.text,
                  fontSize = 15.sp,
                  fontFamily = FontFamily.Monospace,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }
      }
    }

    // Dynamic Waveform and Recorder controls at bottom
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth()
    ) {
      AnimatedVisibility(
        visible = isImporting,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Importing WAV audio...",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      AnimatedVisibility(
        visible = recordingState == RecordingState.RECORDING,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          val waveformColor = MaterialTheme.colorScheme.primary
          Canvas(
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
              .padding(horizontal = 16.dp)
          ) {
            val barWidth = 6.dp.toPx()
            val gap = 4.dp.toPx()
            val amplitudeList = waveformAmplitudes
            val totalBars = (size.width / (barWidth + gap)).toInt()

            val activeList = if (amplitudeList.size > totalBars) {
              amplitudeList.takeLast(totalBars)
            } else {
              amplitudeList
            }

            val startX = (size.width - (activeList.size * (barWidth + gap))) / 2f

            activeList.forEachIndexed { index, amp ->
              val barHeight = size.height * amp
              val x = startX + index * (barWidth + gap)
              val y = (size.height - barHeight) / 2f
              drawRoundRect(
                brush = SolidColor(waveformColor),
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
              )
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = timerText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = RecordingRed
          )
          Spacer(modifier = Modifier.height(16.dp))
        }
      }

      OutlinedButton(
        onClick = onImportClick,
        enabled = recordingState == RecordingState.IDLE && !isImporting,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.padding(bottom = 12.dp)
      ) {
        Icon(
          imageVector = Icons.Default.UploadFile,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (isImporting) "Importing" else "Import WAV")
      }

      // Mic Pulse Animation
      val infiniteTransition = rememberInfiniteTransition(label = "pulse")
      val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (recordingState == RecordingState.RECORDING) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
          animation = twistyTween(1000),
          repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
      )

      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(96.dp)
      ) {
        if (recordingState == RecordingState.RECORDING) {
          Box(
            modifier = Modifier
              .size(76.dp)
              .scale(scale)
              .background(RecordingRed.copy(alpha = 0.15f), CircleShape)
          )
        }

        Box(
          modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
              Brush.linearGradient(
                colors = if (recordingState == RecordingState.RECORDING) {
                  listOf(RecordingRed, RecordingRed.copy(alpha = 0.8f))
                } else {
                  listOf(DeepIndigo, DeepIndigo.copy(alpha = 0.8f))
                }
              )
            )
            .clickable(enabled = !isImporting) {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onRecordToggle()
            },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (recordingState == RecordingState.RECORDING) Icons.Default.Stop else Icons.Default.Mic,
            contentDescription = when {
              isImporting -> "Import in progress"
              recordingState == RecordingState.RECORDING -> "Stop recording"
              else -> "Start recording"
            },
            tint = Color.White,
            modifier = Modifier.size(32.dp)
          )
        }
      }
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
      .padding(24.dp)
  ) {
    // Sleek search input
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchQueryChange,
      placeholder = { Text("Search sessions, notes or keywords...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp),
      shape = RoundedCornerShape(12.dp),
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
        focusedContainerColor = Color.White.copy(alpha = 0.05f),
        unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
      )
    )

    if (sessionsList.isEmpty()) {
      Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            modifier = Modifier.size(48.dp)
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "No Sessions Found",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth()
      ) {
        items(sessionsList) { session ->
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 6.dp)
              .clickable { onSessionClick(session) }
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = session.title,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onDeleteSession(session) }) {
                  Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = RecordingRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = session.rawText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
              Spacer(modifier = Modifier.height(12.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                val formattedTime = remember(session.createdAt) {
                  java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(session.createdAt)
                }
                Text(
                  text = formattedTime,
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Row {
                  Text(
                    text = "${session.durationMs / 1000}s",
                    fontSize = 11.sp,
                    color = AccentCyan,
                    modifier = Modifier
                      .background(AccentCyan.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "${session.wordCount} words",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                      .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
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
      .fillMaxHeight(0.85f),
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
      }
    },
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = session.title,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { isExporting = true }) {
          Icon(Icons.Default.Share, contentDescription = "Export Options", tint = MaterialTheme.colorScheme.primary)
        }
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxSize()) {
        Text(
          text = "Full Transcript",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = AccentCyan,
          modifier = Modifier.padding(bottom = 6.dp)
        )

        // Read-only transcript feed or block
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(bottom = 16.dp)
        ) {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(12.dp)
          ) {
            if (segments.isEmpty()) {
              item {
                Text(
                  text = session.rawText,
                  fontSize = 14.sp,
                  lineHeight = 20.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            } else {
              items(segments) { segment ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = segment.speaker ?: "Speaker 1",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = AccentCyan
                    )
                    Text(
                      text = "${segment.timestampMs / 1000}s",
                      fontSize = 10.sp,
                      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                  }
                  Text(
                    text = segment.text,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp)
                  )
                }
              }
            }
          }
        }

        // Editable Session Notes
        Text(
          text = "Session Notes (auto-saved)",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = AccentCyan,
          modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
          value = notes,
          onValueChange = onNotesChange,
          placeholder = { Text("Take notes, add summaries or action items...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
          modifier = Modifier
            .fillMaxWidth()
            .height(112.dp),
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
          )
        )
      }
    }
  )

  // Export choice overlay dialog
  if (isExporting) {
    AlertDialog(
      onDismissRequest = { isExporting = false },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { isExporting = false }) {
          Text("Cancel", color = MaterialTheme.colorScheme.primary)
        }
      },
      title = { Text("Export Transcription", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          ListItem(
            headlineContent = { Text("Plain Text (.txt)", fontWeight = FontWeight.SemiBold) },
            leadingContent = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = AccentCyan) },
            modifier = Modifier.clickable {
              isExporting = false
              onExportTxt()
            }
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
          ListItem(
            headlineContent = { Text("Structured Data (.json)", fontWeight = FontWeight.SemiBold) },
            leadingContent = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.clickable {
              isExporting = false
              onExportJson()
            }
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
          ListItem(
            headlineContent = { Text("Subtitles (.srt)", fontWeight = FontWeight.SemiBold) },
            leadingContent = { Icon(Icons.Default.Check, contentDescription = null, tint = RecordingRed) },
            modifier = Modifier.clickable {
              isExporting = false
              onExportSrt()
            }
          )
        }
      }
    )
  }
}

private fun shareText(context: Context, text: String, mimeType: String) {
  val intent = Intent(Intent.ACTION_SEND).apply {
    type = mimeType
    putExtra(Intent.EXTRA_TEXT, text)
  }
  context.startActivity(Intent.createChooser(intent, "Share Transcription Export"))
}
