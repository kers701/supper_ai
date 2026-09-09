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
    val isDefault: Boolean = false
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
                name = "DeepSeek",
                baseUrl = "https://api.deepseek.com/v1",
                apiKey = "",
                model = "deepseek-chat"
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
