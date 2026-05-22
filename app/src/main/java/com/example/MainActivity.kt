package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels // Added this just in case
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    private lateinit var networkObserver: NetworkConnectivityObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        MobileAds.initialize(this) {}
        AdHelper.loadRewardedAd(this)
        AdHelper.loadInterstitialAd(this)
        
        networkObserver = NetworkConnectivityObserver(this)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isConnected by networkObserver.isConnected.collectAsStateWithLifecycle()
                    
                    if (!isConnected) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.WifiOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No Internet Connection", style = MaterialTheme.typography.headlineMedium)
                                Text("Please connect to the internet to use the app.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        val viewModel: NoteViewModel = viewModel()
                        NoteSyncApp(viewModel, this@MainActivity)
                    }
                }
            }
        }
    }
}

@Composable
fun NoteSyncApp(viewModel: NoteViewModel, activity: ComponentActivity) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "note_list") {
        composable("note_list") {
            NoteListScreen(viewModel, activity, onNavigateToNote = { noteId ->
                if (noteId == null) {
                    AdHelper.showRewardedAd(activity) {
                        viewModel.selectNote(null)
                        navController.navigate("note_detail")
                    }
                } else {
                    viewModel.selectNote(noteId)
                    navController.navigate("note_detail")
                }
            })
        }
        composable("note_detail") {
            NoteDetailScreen(viewModel, activity, onNavigateBack = {
                navController.popBackStack()
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(viewModel: NoteViewModel, activity: ComponentActivity, onNavigateToNote: (Int?) -> Unit) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Note", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.simulateSync() }, modifier = Modifier.testTag("sync_button")) {
                        Icon(Icons.Filled.Sync, contentDescription = "Sync Now")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToNote(null) }, modifier = Modifier.testTag("add_note_fab")) {
                Icon(Icons.Filled.Add, contentDescription = "New Note")
            }
        }
    ) { paddingValues ->
        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No notes yet. Tap + to start organizing.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(note, onClick = { onNavigateToNote(note.id) }, onDelete = { viewModel.deleteNote(note.id) })
                }
            }
        }
    }
}

@Composable
fun NoteCard(note: Note, onClick: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("note_card_${note.id}"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = note.title.ifEmpty { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (note.isSynced) {
                    Icon(Icons.Filled.CloudDone, contentDescription = "Synced", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Filled.CloudOff, contentDescription = "Not Synced", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(note.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(viewModel: NoteViewModel, activity: ComponentActivity, onNavigateBack: () -> Unit) {
    val currentNote by viewModel.currentNote.collectAsStateWithLifecycle()
    val versions by viewModel.currentNoteVersions.collectAsStateWithLifecycle()
    val comments by viewModel.currentNoteComments.collectAsStateWithLifecycle()
    
    var title by remember(currentNote) { mutableStateOf(currentNote?.title ?: "") }
    var content by remember(currentNote) { mutableStateOf(currentNote?.content ?: "") }
    
    var showVersions by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    
    val saveAndPop = {
        if (title.isNotEmpty() || content.isNotEmpty()) {
            viewModel.saveNote(currentNote?.id, title, content)
        }
        onNavigateBack()
    }
    
    BackHandler {
        saveAndPop()
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            uri?.let {
                PdfHelper.generatePdf(activity, title, content, it)
            }
        }
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentNote == null) "New Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = saveAndPop, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        AdHelper.showInterstitialAd(activity) {
                            if (title.isNotEmpty() || content.isNotEmpty()) {
                                viewModel.saveNote(currentNote?.id, title, content)
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Save, contentDescription = "Save")
                    }
                    IconButton(onClick = {
                        AdHelper.showInterstitialAd(activity) {
                            createDocumentLauncher.launch("${title.ifEmpty { "Note" }}.pdf")
                        }
                    }) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export PDF")
                    }
                    if (currentNote != null) {
                        IconButton(onClick = { showComments = !showComments; showVersions = false }, modifier = Modifier.testTag("comments_button")) {
                            Icon(if (showComments) Icons.Filled.ChatBubble else Icons.Filled.ChatBubbleOutline, contentDescription = "Comments")
                        }
                        IconButton(onClick = { showVersions = !showVersions; showComments = false }, modifier = Modifier.testTag("versions_button")) {
                            Icon(Icons.Filled.History, contentDescription = "Versions")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Main Editor
            Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Note Title", style = MaterialTheme.typography.headlineSmall) },
                    textStyle = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth().testTag("title_input"),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                    maxLines = 1
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Start typing your ideas... (Use markdown-like hints for formatting)") },
                    modifier = Modifier.fillMaxWidth().weight(1f).testTag("content_input"),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                )
            }
            
            // Side Panel for Comments or Versions
            if (showComments) {
                VerticalDivider(modifier = Modifier.fillMaxHeight())
                Column(modifier = Modifier.weight(0.7f).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).padding(16.dp)) {
                    Text("Comments (${comments.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(comments) { comment ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(comment.author, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("Add comment...") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                if (newCommentText.isNotBlank()) {
                                    viewModel.addComment(newCommentText)
                                    newCommentText = ""
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                            }
                        }
                    )
                }
            } else if (showVersions) {
                VerticalDivider(modifier = Modifier.fillMaxHeight())
                Column(modifier = Modifier.weight(0.7f).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).padding(16.dp)) {
                    Text("Version History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(versions) { version ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US).format(Date(version.timestamp)), style = MaterialTheme.typography.labelSmall)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(version.content, style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
