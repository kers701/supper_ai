package com.aichat.app.network

import com.aichat.app.data.ChatMessage
import com.aichat.app.data.ModelConfig
import com.aichat.app.data.Role
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ChatRepository {

    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/") // placeholder, we use full @Url
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(OpenAIApi::class.java)

    fun streamChat(
        config: ModelConfig,
        history: List<ChatMessage>
    ): Flow<String> = flow {
        val messages = mutableListOf<ApiMessage>()
        if (config.systemPrompt.isNotBlank()) {
            messages.add(ApiMessage("system", config.systemPrompt))
        }
        history.forEach { msg ->
            val role = when (msg.role) {
                Role.USER -> "user"
                Role.ASSISTANT -> "assistant"
                Role.SYSTEM -> "system"
            }
            messages.add(ApiMessage(role, msg.content))
        }

        val request = ChatCompletionRequest(
            model = config.model,
            messages = messages,
            temperature = config.temperature,
            maxTokens = config.maxTokens,
            stream = true
        )

        val base = config.baseUrl.trimEnd('/')
        val url = "$base/chat/completions"
        val auth = "Bearer ${config.apiKey}"

        val responseBody = api.chatCompletionsStream(url, auth, request)
        val source = responseBody.source()

        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            if (line.startsWith("data: ")) {
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break
                try {
                    val chunk = gson.fromJson(data, ChatCompletionResponse::class.java)
                    val content = chunk.choices?.firstOrNull()?.delta?.content
                    if (!content.isNullOrEmpty()) {
                        emit(content)
                    }
                    if (chunk.error != null) {
                        emit("\n\n[Error] ${chunk.error.message ?: "Unknown error"}")
                        break
                    }
                } catch (_: Exception) {
                    // ignore parse errors for incomplete chunks
                }
            }
        }
        responseBody.close()
    }.flowOn(Dispatchers.IO)

    suspend fun chatOnce(
        config: ModelConfig,
        history: List<ChatMessage>
    ): String {
        val messages = mutableListOf<ApiMessage>()
        if (config.systemPrompt.isNotBlank()) {
            messages.add(ApiMessage("system", config.systemPrompt))
        }
        history.forEach { msg ->
            val role = when (msg.role) {
                Role.USER -> "user"
                Role.ASSISTANT -> "assistant"
                Role.SYSTEM -> "system"
            }
            messages.add(ApiMessage(role, msg.content))
        }

        val request = ChatCompletionRequest(
            model = config.model,
            messages = messages,
            temperature = config.temperature,
            maxTokens = config.maxTokens,
            stream = false
        )

        val base = config.baseUrl.trimEnd('/')
        val url = "$base/chat/completions"
        val auth = "Bearer ${config.apiKey}"

        val response = api.chatCompletions(url, auth, request)
        if (response.error != null) {
            return "[Error] ${response.error.message ?: "Unknown error"}"
        }
        return response.choices?.firstOrNull()?.message?.content ?: ""
    }
}
