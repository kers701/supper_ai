package com.aichat.app.data

import java.util.UUID

data class ModelConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val temperature: Float = 0.7f,
    val maxTokens: Int? = null,
    val systemPrompt: String = "You are a helpful assistant.",
    val isDefault: Boolean = false,
    /** 深度思考（DeepSeek thinking / 推理模式） */
    val enableThinking: Boolean = false,
    /** 联网搜索（部分中转/国产 API 的 enable_search） */
    val enableWebSearch: Boolean = false
) {
    companion object {
        fun defaultConfigs(): List<ModelConfig> = listOf(
            ModelConfig(
                name = "OpenAI GPT-4o",
                baseUrl = "https://api.openai.com/v1",
                apiKey = "",
                model = "gpt-4o",
                isDefault = true
            ),
            ModelConfig(
                name = "DeepSeek V4 Flash",
                baseUrl = "https://api.deepseek.com",
                apiKey = "",
                model = "deepseek-v4-flash",
                enableThinking = false
            ),
            ModelConfig(
                name = "SiliconFlow",
                baseUrl = "https://api.siliconflow.cn/v1",
                apiKey = "",
                model = "deepseek-ai/DeepSeek-V3"
            )
        )
    }
}
