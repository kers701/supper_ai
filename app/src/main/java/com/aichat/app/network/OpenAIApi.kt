package com.aichat.app.network

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url

data class ThinkingConfig(
    val type: String = "enabled"
)

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ApiMessage>,
    val temperature: Float = 0.7f,
    @SerializedName("max_tokens") val maxTokens: Int? = null,
    val stream: Boolean = true,
    /** DeepSeek 等：深度思考 */
    val thinking: ThinkingConfig? = null,
    /** 通义/部分中转：联网搜索 */
    @SerializedName("enable_search") val enableSearch: Boolean? = null
)

data class ApiMessage(
    val role: String,
    val content: String
)

data class ChatCompletionResponse(
    val id: String?,
    val choices: List<Choice>?,
    val error: ApiError?
)

data class Choice(
    val index: Int?,
    val message: ApiMessage?,
    val delta: Delta?,
    @SerializedName("finish_reason") val finishReason: String?
)

data class Delta(
    val role: String?,
    val content: String?,
    /** 部分模型会把思考过程放在 reasoning_content */
    @SerializedName("reasoning_content") val reasoningContent: String?
)

data class ApiError(
    val message: String?,
    val type: String?,
    val code: String?
)

interface OpenAIApi {
    @POST
    @Streaming
    suspend fun chatCompletionsStream(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body body: ChatCompletionRequest
    ): ResponseBody

    @POST
    suspend fun chatCompletions(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body body: ChatCompletionRequest
    ): ChatCompletionResponse
}
