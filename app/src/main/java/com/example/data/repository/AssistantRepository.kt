package com.example.data.repository

import com.example.data.db.AssistantDao
import com.example.data.model.ChatMessage
import com.example.data.model.VoiceNote
import kotlinx.coroutines.flow.Flow

class AssistantRepository(private val dao: AssistantDao) {
    val allMessages: Flow<List<ChatMessage>> = dao.getAllMessages()
    val allNotes: Flow<List<VoiceNote>> = dao.getAllNotes()

    suspend fun addMessage(text: String, isUser: Boolean, isOffline: Boolean = true, actionType: String? = null, actionData: String? = null): Long {
        val msg = ChatMessage(
            text = text,
            isUser = isUser,
            timestamp = System.currentTimeMillis(),
            isOffline = isOffline,
            actionType = actionType,
            actionData = actionData
        )
        return dao.insertMessage(msg)
    }

    suspend fun clearHistory() {
        dao.clearMessages()
    }

    suspend fun addNote(title: String, content: String, category: String = "note"): Long {
        val note = VoiceNote(
            title = title,
            content = content,
            timestamp = System.currentTimeMillis(),
            category = category
        )
        return dao.insertNote(note)
    }

    suspend fun deleteNote(id: Long) {
        dao.deleteNote(id)
    }
}
