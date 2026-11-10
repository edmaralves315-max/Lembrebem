package org.lembrebem.clinical

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lembrebem.R

@Composable
fun ClinicalScreen(onBack: () -> Unit, canReview: Boolean = false) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val storage = remember { ClinicalStorage(context) }
    val recorder = remember { ConsentRecorder(context, storage) }
    var text by remember { mutableStateOf(runCatching { storage.workingText() }.getOrDefault("")) }
    var consent by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }
    var draft by remember { mutableStateOf<ConsultationDraft?>(null) }
    var deleteDialog by remember { mutableStateOf(false) }
    var audioId by remember { mutableStateOf<String?>(storage.audioIds().lastOrNull()) }
    var retention by remember { mutableStateOf(context.getSharedPreferences("clinical", 0).getInt("retention", 30)) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playbackFile by remember { mutableStateOf<File?>(null) }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        message = if (granted) R.string.clinical_mic_ready else R.string.clinical_mic_denied
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            runCatching {
                val original = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val bytes = input.readBytesLimited(12 * 1024 * 1024)
                        storage.save("document-${java.util.UUID.randomUUID()}.image.enc", bytes)
                    }
                }
                check(original != null)
                MlKitDocumentExtractionEngine(context).extract(uri)
            }.onSuccess { result ->
                if (result.isBlank()) message = R.string.clinical_unreadable
                else { text = result; draft = null; message = R.string.clinical_ocr_review }
            }.onFailure { message = R.string.clinical_unreadable }
            busy = false
        }
    }
    val importText = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            runCatching { withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytesLimited(1024 * 1024).toString(Charsets.UTF_8) }
            }}.onSuccess { imported -> if (imported != null) { text = imported; draft = null } }
                .onFailure { message = R.string.clinical_error }
            busy = false
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && recording) {
                runCatching { recorder.stop() }.onSuccess { audioId = it }.onFailure { message = R.string.clinical_error }
                recording = false
                message = R.string.clinical_recording_stopped
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            recorder.discard()
            player?.release(); playbackFile?.delete()
        }
    }
    LaunchedEffect(retention) { withContext(Dispatchers.IO) { storage.pruneAudio(retention) } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.clinical_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.clinical_local_only))
        Text(stringResource(R.string.clinical_manual_asr))
        Row {
            Checkbox(checked = consent, onCheckedChange = { if (!recording) consent = it }, enabled = !recording)
            Text(stringResource(R.string.clinical_consent), modifier = Modifier.weight(1f))
        }
        Button(modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp), enabled = !busy && (consent || recording), onClick = {
            if (recording) {
                runCatching { recorder.stop() }.onSuccess { audioId = it; message = R.string.clinical_audio_saved }
                    .onFailure { message = R.string.clinical_error }
                recording = false
            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                micPermission.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                runCatching { recorder.start(consent) }.onSuccess { recording = true; message = null }
                    .onFailure { message = R.string.clinical_error }
            }
        }) { Text(stringResource(if (recording) R.string.clinical_stop else R.string.clinical_record)) }
        if (recording) Text(stringResource(R.string.clinical_recording_visible), color = MaterialTheme.colorScheme.error)
        if (audioId != null && !recording) Button(onClick = {
            runCatching {
                player?.release(); playbackFile?.delete()
                val file = File.createTempFile("playback-", ".m4a", context.cacheDir)
                file.writeBytes(storage.read(audioId!!)); playbackFile = file
                player = MediaPlayer().apply {
                    setDataSource(file.absolutePath); prepare()
                    setOnCompletionListener { it.release(); player = null; file.delete(); playbackFile = null }
                    start()
                }
            }.onFailure { message = R.string.clinical_error }
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.clinical_play)) }
        Button(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !busy && !recording,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.clinical_photo)) }
        Button(onClick = { importText.launch(arrayOf("text/plain")) }, enabled = !busy && !recording,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.clinical_import)) }
        if (busy) CircularProgressIndicator()
        message?.let { Text(stringResource(it)) }
        OutlinedTextField(value = text, onValueChange = { if (it.length <= 100_000) { text = it; draft = null } },
            label = { Text(stringResource(R.string.clinical_source)) }, modifier = Modifier.fillMaxWidth(), minLines = 5)
        Button(onClick = {
            runCatching { storage.saveWorkingText(text); draft = EvidenceSummaryEngine().draft(text); storage.saveDraft(draft!!) }
                .onFailure { message = R.string.clinical_error }
        }, enabled = text.isNotBlank() && !busy && !recording, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(stringResource(R.string.clinical_draft))
        }
        draft?.let { current ->
            Text(stringResource(R.string.clinical_unvalidated), style = MaterialTheme.typography.titleLarge)
            current.proposals.forEach { proposal ->
                val category = when(proposal.category) {
                    DraftCategory.MEDICATION -> R.string.clinical_medication
                    DraftCategory.EXAM -> R.string.clinical_exam
                    DraftCategory.RETURN -> R.string.clinical_return
                    DraftCategory.GUIDANCE -> R.string.clinical_guidance
                }
                Text(stringResource(R.string.clinical_evidence, stringResource(category), proposal.line, proposal.literalText))
            }
            Text(stringResource(R.string.clinical_no_publish))
            if (canReview) Button(onClick = {
                runCatching { storage.saveDraft(current.copy(status = "HUMAN_REVIEWED_DRAFT")); message = R.string.clinical_review_saved }
                    .onFailure { message = R.string.clinical_error }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.clinical_review)) }
        }
        Text(stringResource(R.string.clinical_retention, retention))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(7, 30, 90).forEach { days ->
                OutlinedButton(onClick = { retention = days; context.getSharedPreferences("clinical", 0).edit().putInt("retention", days).apply() }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.clinical_days, days), fontSize = 22.sp)
                }
            }
        }
        OutlinedButton(onClick = { deleteDialog = true }, enabled = !recording, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(stringResource(R.string.clinical_delete))
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.clinical_back)) }
    }
    if (deleteDialog) AlertDialog(onDismissRequest = { deleteDialog = false }, title = { Text(stringResource(R.string.clinical_delete)) },
        text = { Text(stringResource(R.string.clinical_delete_confirm)) },
        confirmButton = { TextButton(onClick = {
            player?.release(); player = null; playbackFile?.delete(); playbackFile = null
            runCatching { storage.deleteAll() }.onSuccess { text = ""; draft = null; audioId = null }.onFailure { message = R.string.clinical_error }
            deleteDialog = false
        }) { Text(stringResource(R.string.clinical_delete)) } },
        dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text(stringResource(R.string.clinical_cancel)) } })
}

private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count < 0) return output.toByteArray()
        require(output.size() + count <= limit)
        output.write(buffer, 0, count)
    }
}
