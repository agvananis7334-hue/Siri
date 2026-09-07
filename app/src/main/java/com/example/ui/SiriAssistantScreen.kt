package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.data.model.VoiceNote
import com.example.ui.components.ActionCard
import com.example.ui.components.SiriOrb
import com.example.ui.components.VoiceWaveform
import com.example.ui.theme.SiriAmber
import com.example.ui.theme.SiriBackground
import com.example.ui.theme.SiriBlue
import com.example.ui.theme.SiriBorder
import com.example.ui.theme.SiriCyan
import com.example.ui.theme.SiriGreen
import com.example.ui.theme.SiriMagenta
import com.example.ui.theme.SiriSurface
import com.example.ui.theme.SiriSurfaceElevated
import com.example.ui.theme.SiriSurfaceVariant
import com.example.ui.theme.SiriTextMuted
import com.example.ui.theme.SiriTextPrimary
import com.example.ui.theme.SiriTextSecondary
import com.example.ui.theme.SiriViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiriAssistantScreen(
    viewModel: SiriAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val notes by viewModel.voiceNotes.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val isThinking by viewModel.isThinking.collectAsStateWithLifecycle()
    val audioRms by viewModel.audioRms.collectAsStateWithLifecycle()
    val partialSpeech by viewModel.partialSpeech.collectAsStateWithLifecycle()
    val isOfflineOnly by viewModel.isOfflineOnly.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val speechError by viewModel.speechError.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var showNotesSheet by remember { mutableStateOf(false) }
    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordAudioPermission = isGranted
        if (isGranted) {
            viewModel.toggleListening()
        }
    }

    val listState = rememberLazyListState()

    // Auto-scroll when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SiriBackground),
        containerColor = SiriBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // 1. Siri Top Header Bar
            SiriTopBar(
                isOfflineOnly = isOfflineOnly,
                selectedLanguage = selectedLanguage,
                onToggleOffline = { viewModel.toggleOfflineMode() },
                onLanguageChange = { viewModel.setLanguage(it) },
                onOpenNotes = { showNotesSheet = true },
                onClearHistory = { viewModel.clearHistory() }
            )

            // 2. Chat Conversation List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    EmptyStateView(
                        onQuickPrompt = { viewModel.onTextInputSubmitted(it) }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                onReplaySpeech = { viewModel.speechManager.speak(message.text) },
                                onActionClick = { actionCode ->
                                    viewModel.executeQuickAction(actionCode)
                                }
                            )
                        }
                    }
                }
            }

            // 3. Status indicator & Live Speech text
            StatusAndPartialDisplay(
                isListening = isListening,
                isSpeaking = isSpeaking,
                isThinking = isThinking,
                partialSpeech = partialSpeech,
                speechError = speechError,
                onStopSpeaking = { viewModel.stopSpeaking() }
            )

            // 4. Quick Action Chips (Vivo T3 Shortcuts)
            QuickActionChipsRow(
                onActionSelected = { viewModel.executeQuickAction(it) },
                onPromptSelected = { viewModel.onTextInputSubmitted(it) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 5. Hero Siri Glowing Orb Interaction Center
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                VoiceWaveform(
                    isActive = isListening || isSpeaking,
                    audioRms = audioRms,
                    modifier = Modifier.fillMaxWidth(0.9f)
                )

                SiriOrb(
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    isThinking = isThinking,
                    audioRms = audioRms,
                    size = 100.dp,
                    onClick = {
                        if (hasRecordAudioPermission) {
                            viewModel.toggleListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
            }

            Text(
                text = when {
                    isListening -> "સાંભળી રહ્યો છું... (Listening)"
                    isSpeaking -> "બોલી રહ્યો છું... (Speaking)"
                    isThinking -> "વિચારી રહ્યો છું... (Thinking)"
                    else -> "ઓર્બ પર ટેપ કરો (Tap Siri Orb to Speak)"
                },
                color = if (isListening) SiriCyan else if (isSpeaking) SiriMagenta else SiriTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                textAlign = TextAlign.Center
            )

            // 6. Bottom Text Input Bar
            BottomInputBar(
                inputText = inputText,
                isListening = isListening,
                onTextChanged = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.onTextInputSubmitted(inputText)
                        inputText = ""
                    }
                },
                onMicClick = {
                    if (hasRecordAudioPermission) {
                        viewModel.toggleListening()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            )
        }
    }

    // Offline Voice Notes Bottom Sheet
    if (showNotesSheet) {
        NotesBottomSheet(
            notes = notes,
            onDismiss = { showNotesSheet = false },
            onDeleteNote = { viewModel.deleteNote(it) }
        )
    }
}

@Composable
private fun SiriTopBar(
    isOfflineOnly: Boolean,
    selectedLanguage: String,
    onToggleOffline: () -> Unit,
    onLanguageChange: (String) -> Unit,
    onOpenNotes: () -> Unit,
    onClearHistory: () -> Unit
) {
    Surface(
        color = SiriSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Title with Vivo T3 Branding
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(SiriCyan, SiriMagenta, SiriBlue))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Siri",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Siri AI",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = SiriTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SiriSurfaceElevated)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Vivo T3",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SiriCyan
                            )
                        }
                    }
                    Text(
                        text = if (isOfflineOnly) "🟢 Offline Engine સક્રિય" else "🌐 Hybrid Gemini AI",
                        fontSize = 11.sp,
                        color = if (isOfflineOnly) SiriGreen else SiriCyan
                    )
                }
            }

            // Right Actions: Offline Toggle, Language, Notes, Clear
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Offline / Hybrid Mode Toggle Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isOfflineOnly) SiriGreen.copy(alpha = 0.15f) else SiriCyan.copy(alpha = 0.15f))
                        .clickable { onToggleOffline() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("toggle_offline_mode_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isOfflineOnly) Icons.Default.CloudOff else Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = if (isOfflineOnly) SiriGreen else SiriCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOfflineOnly) "Offline" else "Hybrid",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOfflineOnly) SiriGreen else SiriCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Language Selector
                LanguageSelectorChip(
                    currentLang = selectedLanguage,
                    onLangChange = onLanguageChange
                )

                // Notes sheet icon
                IconButton(
                    onClick = onOpenNotes,
                    modifier = Modifier.testTag("open_notes_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Note,
                        contentDescription = "Notes",
                        tint = SiriTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Clear history icon
                IconButton(
                    onClick = onClearHistory,
                    modifier = Modifier.testTag("clear_history_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear",
                        tint = SiriTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageSelectorChip(
    currentLang: String,
    onLangChange: (String) -> Unit
) {
    val languages = listOf("gu" to "ગુજ", "hi" to "हिं", "en" to "EN")
    val nextLang = when (currentLang) {
        "gu" -> "hi"
        "hi" -> "en"
        else -> "gu"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SiriSurfaceElevated)
            .clickable { onLangChange(nextLang) }
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag("lang_toggle_btn")
    ) {
        val label = when (currentLang) {
            "gu" -> "ગુજરાતી"
            "hi" -> "हिंदी"
            else -> "English"
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = SiriViolet
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onReplaySpeech: () -> Unit,
    onActionClick: (String) -> Unit
) {
    val isUser = message.isUser

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(SiriCyan, SiriViolet))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth(if (isUser) 0.8f else 0.88f)
                    .testTag(if (isUser) "user_message_bubble" else "siri_message_bubble"),
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) SiriSurfaceElevated else SiriSurfaceVariant
                ),
                border = if (!isUser) {
                    BorderStroke(1.dp, SiriCyan.copy(alpha = 0.25f))
                } else {
                    BorderStroke(1.dp, SiriBorder)
                }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header tag for Assistant (Offline badge)
                    if (!isUser) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (message.isOffline) "⚡ ઑફલાઇન એન્જિન" else "🌐 Hybrid AI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (message.isOffline) SiriGreen else SiriCyan
                            )
                            IconButton(
                                onClick = onReplaySpeech,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Listen again",
                                    tint = SiriTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Main Text
                    Text(
                        text = message.text,
                        color = SiriTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )

                    // Interactive Action Card if attached
                    if (!message.actionType.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ActionCard(
                            actionType = message.actionType,
                            actionData = message.actionData,
                            onActionClick = onActionClick
                        )
                    }

                    // Timestamp
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp)),
                        fontSize = 10.sp,
                        color = SiriTextMuted,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusAndPartialDisplay(
    isListening: Boolean,
    isSpeaking: Boolean,
    isThinking: Boolean,
    partialSpeech: String,
    speechError: String?,
    onStopSpeaking: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isSpeaking) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SiriMagenta.copy(alpha = 0.15f))
                    .clickable { onStopSpeaking() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = SiriMagenta,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("અવાજ બંધ કરો (Stop Speaking)", fontSize = 11.sp, color = SiriMagenta)
            }
        }

        if (isListening && partialSpeech.isNotBlank()) {
            Text(
                text = "\"$partialSpeech...\"",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = SiriCyan,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (speechError != null && !isListening) {
            Text(
                text = "⚠️ $speechError",
                fontSize = 12.sp,
                color = SiriAmber,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun QuickActionChipsRow(
    onActionSelected: (String) -> Unit,
    onPromptSelected: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickChip(
            icon = Icons.Default.FlashlightOn,
            label = "ટોર્ચ (Torch)",
            color = SiriAmber,
            onClick = { onActionSelected("TORCH_TOGGLE") }
        )
        QuickChip(
            icon = Icons.Default.BatteryFull,
            label = "બેટરી %",
            color = SiriGreen,
            onClick = { onActionSelected("BATTERY") }
        )
        QuickChip(
            icon = Icons.Default.PhoneAndroid,
            label = "Vivo T3 Specs",
            color = SiriCyan,
            onClick = { onActionSelected("VIVO_SPECS") }
        )
        QuickChip(
            icon = Icons.Default.CameraAlt,
            label = "કેમેરો (Camera)",
            color = SiriViolet,
            onClick = { onActionSelected("CAMERA") }
        )
        QuickChip(
            icon = Icons.Default.Timer,
            label = "ટાઈમર મુકો",
            color = SiriBlue,
            onClick = { onPromptSelected("5 મિનિટ નો ટાઈમર મુકો") }
        )
        QuickChip(
            icon = Icons.Default.SentimentSatisfied,
            label = "જોક (Joke)",
            color = SiriMagenta,
            onClick = { onActionSelected("JOKE") }
        )
    }
}

@Composable
private fun QuickChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SiriSurfaceElevated)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SiriTextPrimary
            )
        }
    }
}

@Composable
private fun BottomInputBar(
    inputText: String,
    isListening: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit
) {
    Surface(
        color = SiriSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChanged,
                placeholder = {
                    Text(
                        "સવાલ પૂછો અથવા આદેશ આપો...",
                        fontSize = 13.sp,
                        color = SiriTextMuted
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_text_input"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SiriCyan,
                    unfocusedBorderColor = SiriBorder,
                    focusedContainerColor = SiriSurfaceElevated,
                    unfocusedContainerColor = SiriSurfaceElevated,
                    focusedTextColor = SiriTextPrimary,
                    unfocusedTextColor = SiriTextPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (inputText.isNotBlank()) {
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SiriCyan)
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isListening) SiriMagenta else SiriSurfaceElevated)
                        .testTag("mic_input_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Speak",
                        tint = if (isListening) Color.White else SiriCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView(onQuickPrompt: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(SiriCyan.copy(alpha = 0.3f), Color.Transparent))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = SiriCyan,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "iPhone Siri જેવી AI આસિસ્ટન્ટ",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SiriTextPrimary
        )
        Text(
            text = "Vivo T3 5G માટે ખાસ તૈયાર કરેલ • 100% Offline કાર્યક્ષમ",
            fontSize = 12.sp,
            color = SiriCyan,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Example prompt buttons
        val examples = listOf(
            "🔦 ટોર્ચ ચાલુ કરો",
            "🔋 બેટરી કેટલી છે?",
            "📱 Vivo T3 ની માહિતી",
            "⏰ 7 વાગ્યા નું એલાર્મ મુકો",
            "🔢 450 ગુણ્યા 12 કેટલા થાય?",
            "📝 યાદ રાખો: આજે દૂધ લાવવાનું છે"
        )
        examples.forEach { example ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SiriSurfaceElevated)
                    .clickable { onQuickPrompt(example) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = example,
                    fontSize = 13.sp,
                    color = SiriTextSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesBottomSheet(
    notes: List<VoiceNote>,
    onDismiss: () -> Unit,
    onDeleteNote: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SiriSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📝 સાચવેલી ઑફલાઇન નોંધો (${notes.size})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = SiriTextPrimary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            if (notes.isEmpty()) {
                Text(
                    text = "હજી સુધી કોઈ નોંધ નથી. 'નોટ લખો...' બોલીને ઑફલાઇન નોંધ સાચવી શકો છો.",
                    fontSize = 13.sp,
                    color = SiriTextMuted,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(notes, key = { it.id }) { note ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SiriSurfaceElevated),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = note.title,
                                        fontSize = 12.sp,
                                        color = SiriCyan,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = note.content,
                                        fontSize = 14.sp,
                                        color = SiriTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(note.timestamp)),
                                        fontSize = 11.sp,
                                        color = SiriTextMuted
                                    )
                                }
                                IconButton(onClick = { onDeleteNote(note.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = SiriMagenta
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
