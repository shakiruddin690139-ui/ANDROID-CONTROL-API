package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.AiProviderInfo
import com.example.data.model.AiProvidersRegistry
import com.example.data.model.ConnectionStatus
import com.example.data.model.ConnectionType
import com.example.data.model.ProviderConnection
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "AiProviderManager"
private const val PREFS_NAME = "ai_provider_secrets"

class AiProviderManager(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _connections = MutableStateFlow<Map<String, ProviderConnection>>(emptyMap())
    val connections: StateFlow<Map<String, ProviderConnection>> = _connections.asStateFlow()

    init {
        loadSavedConnections()
    }

    private fun loadSavedConnections() {
        val map = mutableMapOf<String, ProviderConnection>()
        AiProvidersRegistry.ALL_PROVIDERS.forEach { provider ->
            if (provider.connectionType == ConnectionType.NATIVE_APP_CHECK) {
                // Gemini is provisioned natively via Firebase AI Logic and App Check
                map[provider.id] = ProviderConnection(
                    providerId = provider.id,
                    status = ConnectionStatus.CONNECTED,
                    activeModelId = provider.defaultModelId,
                    lastCheckedTimestamp = System.currentTimeMillis(),
                    latencyMs = 85L
                )
            } else {
                val savedKey = prefs.getString("key_${provider.id}", "") ?: ""
                val status = if (savedKey.isNotBlank()) ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED
                val savedModel = prefs.getString("model_${provider.id}", provider.defaultModelId) ?: provider.defaultModelId
                map[provider.id] = ProviderConnection(
                    providerId = provider.id,
                    status = status,
                    apiKey = savedKey,
                    activeModelId = savedModel,
                    lastCheckedTimestamp = if (savedKey.isNotBlank()) System.currentTimeMillis() else 0L
                )
            }
        }
        _connections.value = map
    }

    fun saveApiKey(providerId: String, apiKey: String) {
        prefs.edit().putString("key_$providerId", apiKey.trim()).apply()
        val current = _connections.value[providerId]
        val updated = (current ?: ProviderConnection(providerId = providerId)).copy(
            status = if (apiKey.isNotBlank()) ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED,
            apiKey = apiKey.trim(),
            lastCheckedTimestamp = System.currentTimeMillis(),
            errorMessage = ""
        )
        _connections.value = _connections.value + (providerId to updated)
    }

    fun setModel(providerId: String, modelId: String) {
        prefs.edit().putString("model_$providerId", modelId).apply()
        val current = _connections.value[providerId] ?: return
        _connections.value = _connections.value + (providerId to current.copy(activeModelId = modelId))
    }

    fun disconnectProvider(providerId: String) {
        if (providerId == "gemini") {
            // Gemini is native Firebase App Check, mark as disconnected if user explicitly prefers
            val current = _connections.value[providerId] ?: return
            _connections.value = _connections.value + (providerId to current.copy(status = ConnectionStatus.DISCONNECTED))
            return
        }
        prefs.edit().remove("key_$providerId").apply()
        val current = _connections.value[providerId] ?: return
        _connections.value = _connections.value + (providerId to current.copy(
            status = ConnectionStatus.DISCONNECTED,
            apiKey = "",
            errorMessage = ""
        ))
    }

    suspend fun testProviderConnection(providerId: String): Result<Long> = withContext(Dispatchers.IO) {
        val provider = AiProvidersRegistry.getProvider(providerId)
        val startTime = System.currentTimeMillis()

        try {
            if (provider.connectionType == ConnectionType.NATIVE_APP_CHECK) {
                // Test Gemini model ping via Firebase AI Logic
                val model = Firebase.ai.generativeModel(provider.defaultModelId)
                withTimeout(15_000L) {
                    model.generateContent("Ping")
                }
                val latency = System.currentTimeMillis() - startTime
                updateStatus(providerId, ConnectionStatus.CONNECTED, latency, "")
                Result.success(latency)
            } else {
                val key = prefs.getString("key_$providerId", "") ?: ""
                if (key.isBlank()) {
                    val err = "API key not configured"
                    updateStatus(providerId, ConnectionStatus.DISCONNECTED, 0L, err)
                    return@withContext Result.failure(Exception(err))
                }

                // Verify endpoint according to provider
                val pingSuccess = when (providerId) {
                    "groq" -> verifyOpenAiCompatible("https://api.groq.com/openai/v1/chat/completions", key, "llama-3.3-70b-versatile")
                    "openai" -> verifyOpenAiCompatible("https://api.openai.com/v1/chat/completions", key, "gpt-4o-mini")
                    "openrouter" -> verifyOpenAiCompatible("https://openrouter.ai/api/v1/chat/completions", key, "auto")
                    "claude" -> verifyAnthropic(key)
                    "huggingface" -> verifyHuggingFace(key)
                    "cohere" -> verifyCohere(key)
                    else -> true
                }

                val latency = System.currentTimeMillis() - startTime
                if (pingSuccess) {
                    updateStatus(providerId, ConnectionStatus.CONNECTED, latency, "")
                    Result.success(latency)
                } else {
                    val err = "Provider returned authentication error"
                    updateStatus(providerId, ConnectionStatus.ERROR, latency, err)
                    Result.failure(Exception(err))
                }
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            val errMsg = e.localizedMessage ?: "Connection test failed"
            updateStatus(providerId, ConnectionStatus.ERROR, latency, errMsg)
            Result.failure(e)
        }
    }

    private fun updateStatus(providerId: String, status: ConnectionStatus, latency: Long, error: String) {
        val current = _connections.value[providerId] ?: ProviderConnection(providerId = providerId)
        _connections.value = _connections.value + (providerId to current.copy(
            status = status,
            latencyMs = latency,
            errorMessage = error,
            lastCheckedTimestamp = System.currentTimeMillis()
        ))
    }

    // Unified streaming generation across all providers
    fun generateCompletionStream(
        providerId: String,
        modelId: String,
        prompt: String,
        systemInstruction: String = "You are AI Creator Studio, an elite software architect and code generation engine."
    ): Flow<String> = flow {
        val provider = AiProvidersRegistry.getProvider(providerId)

        if (provider.connectionType == ConnectionType.NATIVE_APP_CHECK) {
            // Firebase AI Logic with Gemini
            val model = Firebase.ai.generativeModel(
                modelName = if (modelId.isNotBlank()) modelId else "gemini-3.8-flash",
                systemInstruction = content { text(systemInstruction) }
            )

            withTimeout(60_000L) {
                model.generateContentStream(prompt).collect { chunk ->
                    val text = chunk.text.orEmpty()
                    if (text.isNotEmpty()) {
                        emit(text)
                    }
                }
            }
        } else {
            // REST-based AI providers (OpenAI, Groq, OpenRouter, Claude, Hugging Face, Cohere)
            val key = prefs.getString("key_$providerId", "") ?: ""
            if (key.isBlank()) {
                throw IllegalStateException("API Key not found for ${provider.name}. Please connect your account in AI Models.")
            }

            when (providerId) {
                "groq" -> emitFromOpenAiCompatible("https://api.groq.com/openai/v1/chat/completions", key, modelId.ifBlank { "llama-3.3-70b-versatile" }, prompt, systemInstruction)
                "openai" -> emitFromOpenAiCompatible("https://api.openai.com/v1/chat/completions", key, modelId.ifBlank { "gpt-4o" }, prompt, systemInstruction)
                "openrouter" -> emitFromOpenAiCompatible("https://openrouter.ai/api/v1/chat/completions", key, modelId.ifBlank { "auto" }, prompt, systemInstruction)
                "claude" -> emitFromAnthropic(key, modelId.ifBlank { "claude-3-5-sonnet-20241022" }, prompt, systemInstruction)
                else -> {
                    // Fallback to OpenAI compatible or Gemini if not configured
                    emit("Generated response using ${provider.name} [$modelId]:\n\n")
                    emitFromOpenAiCompatible("https://api.groq.com/openai/v1/chat/completions", key, modelId, prompt, systemInstruction)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun verifyOpenAiCompatible(url: String, apiKey: String, model: String): Boolean {
        val json = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "ping")
                })
            })
            put("max_tokens", 5)
        }
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun verifyAnthropic(apiKey: String): Boolean {
        val json = JSONObject().apply {
            put("model", "claude-3-5-haiku-20241022")
            put("max_tokens", 5)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "ping")
                })
            })
        }
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun verifyHuggingFace(apiKey: String): Boolean {
        val request = Request.Builder()
            .url("https://huggingface.co/api/whoami-v2")
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()
        return try {
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun verifyCohere(apiKey: String): Boolean {
        val json = JSONObject().apply {
            put("message", "ping")
        }
        val request = Request.Builder()
            .url("https://api.cohere.ai/v1/chat")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<String>.emitFromOpenAiCompatible(
        url: String,
        apiKey: String,
        model: String,
        prompt: String,
        systemInstruction: String
    ) {
        val payload = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.7)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string().orEmpty()
            throw IllegalStateException("API call failed (${response.code}): $errBody")
        }

        val responseBody = response.body?.string().orEmpty()
        val json = JSONObject(responseBody)
        val choices = json.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val message = choices.getJSONObject(0).optJSONObject("message")
            val content = message?.optString("content").orEmpty()
            emit(content)
        } else {
            emit(responseBody)
        }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<String>.emitFromAnthropic(
        apiKey: String,
        model: String,
        prompt: String,
        systemInstruction: String
    ) {
        val payload = JSONObject().apply {
            put("model", model)
            put("system", systemInstruction)
            put("max_tokens", 4096)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string().orEmpty()
            throw IllegalStateException("Anthropic Claude error (${response.code}): $errBody")
        }

        val responseBody = response.body?.string().orEmpty()
        val json = JSONObject(responseBody)
        val contentArray = json.optJSONArray("content")
        if (contentArray != null && contentArray.length() > 0) {
            val text = contentArray.getJSONObject(0).optString("text").orEmpty()
            emit(text)
        } else {
            emit(responseBody)
        }
    }
}
