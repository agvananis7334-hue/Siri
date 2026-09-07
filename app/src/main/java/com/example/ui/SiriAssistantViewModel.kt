package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AssistantDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.VoiceNote
import com.example.data.repository.AssistantRepository
import com.example.service.BatteryInfo
import com.example.service.DeviceActionController
import com.example.service.GeminiHybridService
import com.example.service.OfflineIntentEngine
import com.example.service.SpeechVoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SiriAssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AssistantDatabase.getDatabase(application)
    private val repository = AssistantRepository(db.assistantDao())
    val deviceController = DeviceActionController(application)
    private val intentEngine = OfflineIntentEngine(deviceController)
    private val geminiService = GeminiHybridService(application)

    val messages: StateFlow<List<ChatMessage>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val voiceNotes: StateFlow<List<VoiceNote>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _isOfflineOnly = MutableStateFlow(true) // Defaults to offline as user specifically asked for offline
    val isOfflineOnly: StateFlow<Boolean> = _isOfflineOnly.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("gu") // Gujarati default
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _partialSpeech = MutableStateFlow("")
    val partialSpeech: StateFlow<String> = _partialSpeech.asStateFlow()

    private val _isTorchOn = MutableStateFlow(deviceController.isTorchActive())
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _batteryInfo = MutableStateFlow(deviceController.getBatteryInfo())
    val batteryInfo: StateFlow<BatteryInfo> = _batteryInfo.asStateFlow()

    val speechManager = SpeechVoiceManager(
        context = application,
        onSpeechResult = { text ->
            _partialSpeech.value = ""
            processUserInput(text)
        },
        onPartialSpeech = { partial ->
            _partialSpeech.value = partial
        }
    )

    val isListening: StateFlow<Boolean> = speechManager.isListening
    val isSpeaking: StateFlow<Boolean> = speechManager.isSpeaking
    val audioRms: StateFlow<Float> = speechManager.audioRms
    val speechError: StateFlow<String?> = speechManager.speechError

    init {
        // Welcome greeting if messages are empty
        viewModelScope.launch {
            repository.allMessages.collect { list ->
                if (list.isEmpty()) {
                    val initialGreeting = "✨ નમસ્તે! હું તમારી Siri AI આસિસ્ટન્ટ છું. Vivo T3 પર હું સંપૂર્ણપણે Offline પણ કામ કરું છું.\n\nટોર્ચ ચાલુ/બંધ કરવા, બેટરી જોવા, એપ ખોલવા, ટાઈમર કે ગણતરી કરવા માટે ઑર્બ દબાવો અથવા નીચે બોલો!"
                    repository.addMessage(
                        text = initialGreeting,
                        isUser = false,
                        isOffline = true,
                        actionType = "VIVO_INFO",
                        actionData = "Vivo T3 5G"
                    )
                }
            }
        }
    }

    fun toggleListening() {
        if (isListening.value) {
            speechManager.stopListening()
        } else {
            speechManager.stopSpeaking()
            val langTag = when (_selectedLanguage.value) {
                "gu" -> "gu-IN"
                "hi" -> "hi-IN"
                else -> "en-US"
            }
            speechManager.startListening(langTag)
        }
    }

    fun setLanguage(lang: String) {
        _selectedLanguage.value = lang
        speechManager.setLanguage(lang)
    }

    fun toggleOfflineMode() {
        _isOfflineOnly.value = !_isOfflineOnly.value
    }

    fun onTextInputSubmitted(query: String) {
        if (query.isNotBlank()) {
            processUserInput(query.trim())
        }
    }

    private fun processUserInput(rawText: String) {
        viewModelScope.launch {
            speechManager.stopSpeaking()
            _isThinking.value = true

            // 1. Add user message to DB
            repository.addMessage(text = rawText, isUser = true)

            // 2. Try Offline Intent Engine first (device actions, fast local commands)
            val offlineResult = intentEngine.processCommand(rawText)

            // If offline result is a direct actionable intent (torch, battery, time, app, timer, note, specs, calc)
            // or if user forced offline mode, or if offline fallback
            val isActionableCommand = offlineResult.actionType != null || offlineResult.noteContent != null

            if (isActionableCommand || _isOfflineOnly.value || !geminiService.isOnline()) {
                // Execute and record
                handleOfflineResult(offlineResult)
                _isThinking.value = false
            } else {
                // If offline engine had no direct action and Hybrid Mode is active & device is online,
                // try Gemini AI for a deep creative answer!
                val geminiResult = geminiService.generateResponse(rawText)
                geminiResult.fold(
                    onSuccess = { reply ->
                        repository.addMessage(
                            text = reply,
                            isUser = false,
                            isOffline = false
                        )
                        speechManager.speak(reply)
                    },
                    onFailure = {
                        // Fall back gracefully to offline response
                        handleOfflineResult(offlineResult)
                    }
                )
                _isThinking.value = false
            }

            // Refresh battery & torch state
            _isTorchOn.value = deviceController.isTorchActive()
            _batteryInfo.value = deviceController.getBatteryInfo()
        }
    }

    private suspend fun handleOfflineResult(result: com.example.service.IntentResult) {
        // If it was a note, save it to Room notes table
        if (result.noteContent != null) {
            repository.addNote(
                title = "Voice Note (${java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())})",
                content = result.noteContent
            )
        }

        repository.addMessage(
            text = result.replyText,
            isUser = false,
            isOffline = true,
            actionType = result.actionType,
            actionData = result.actionData
        )

        speechManager.speak(result.spokenText)
    }

    fun executeQuickAction(actionCode: String) {
        when (actionCode) {
            "TORCH_TOGGLE", "TOGGLE_TORCH" -> {
                val newState = !_isTorchOn.value
                val success = deviceController.toggleTorch(newState)
                _isTorchOn.value = newState
                val text = if (newState) "🔦 ટોર્ચ ચાલુ કરી દીધી છે." else "🔦 ટોર્ચ બંધ કરી દીધી છે."
                viewModelScope.launch {
                    repository.addMessage(
                        text = text,
                        isUser = false,
                        isOffline = true,
                        actionType = "TORCH",
                        actionData = if (newState) "ON" else "OFF"
                    )
                }
                speechManager.speak(if (newState) "Torch chalu kari" else "Torch bandh kari")
            }
            "BATTERY" -> {
                processUserInput("બેટરી કેટલી છે")
            }
            "VIVO_SPECS" -> {
                processUserInput("Vivo T3 specs")
            }
            "TIME" -> {
                processUserInput("સમય શું છે")
            }
            "JOKE" -> {
                processUserInput("એક જોક કહો")
            }
            "CAMERA" -> {
                deviceController.openCamera()
                viewModelScope.launch {
                    repository.addMessage(
                        text = "📷 કેમેરો ખોલ્યો છે.",
                        isUser = false,
                        isOffline = true,
                        actionType = "APP",
                        actionData = "Camera"
                    )
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }

    fun stopSpeaking() {
        speechManager.stopSpeaking()
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
    }
}
