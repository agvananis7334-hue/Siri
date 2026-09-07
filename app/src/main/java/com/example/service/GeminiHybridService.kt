package com.example.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiHybridService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isOnline(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun generateResponse(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("NO_API_KEY"))
        }

        if (!isOnline()) {
            return@withContext Result.failure(Exception("OFFLINE"))
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val systemPart = JSONObject().put("text", "You are Siri, a witty, brilliant, helpful voice AI assistant optimized for the Vivo T3 5G smartphone. You speak Gujarati, Hindi, and English fluently. Answer naturally, warmly, and concisely (2 to 4 sentences) so your answer is pleasant when read aloud via speech synthesis.")
            val systemInstruction = JSONObject().put("parts", JSONArray().put(systemPart))

            val userPart = JSONObject().put("text", prompt)
            val userContent = JSONObject().put("role", "user").put("parts", JSONArray().put(userPart))
            val contents = JSONArray().put(userContent)

            val payload = JSONObject().apply {
                put("systemInstruction", systemInstruction)
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 250)
                })
            }

            val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: "HTTP ${response.code}"
                Log.e("GeminiHybridService", "Gemini API failed: $err")
                return@withContext Result.failure(Exception("API_ERROR: ${response.code}"))
            }

            val responseBody = response.body?.string() ?: ""
            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val replyText = parts?.optJSONObject(0)?.optString("text")?.trim()

            if (!replyText.isNullOrBlank()) {
                Result.success(replyText)
            } else {
                Result.failure(Exception("EMPTY_RESPONSE"))
            }
        } catch (e: Exception) {
            Log.e("GeminiHybridService", "Error calling Gemini: ${e.message}")
            Result.failure(e)
        }
    }
}
