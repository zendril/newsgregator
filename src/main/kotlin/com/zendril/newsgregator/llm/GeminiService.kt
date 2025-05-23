package com.zendril.newsgregator.llm

import com.zendril.newsgregator.models.ContentItem
import com.zendril.newsgregator.models.LlmConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Implementation of LLM service using Google's Gemini model
 */
class GeminiService(private val config: LlmConfig) : LlmService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    private val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models"

    override suspend fun summarizeContent(contentItems: List<ContentItem>): String {
        if (contentItems.isEmpty()) {
            return "No content to summarize."
        }

        // For multiple items, create a combined prompt with all content
        val combinedContent = contentItems.joinToString("\n\n") { 
            "Title: ${it.title}\nSource: ${it.sourceName}\nContent: ${it.content}"
        }

        val prompt = "${config.summaryPrompt}\n\n$combinedContent\n\nPlease provide a concise summary of these articles, highlighting the key points and any common themes."
        return generateSummary(prompt)
    }

    override suspend fun summarizeContentItem(contentItem: ContentItem): String {
        val prompt = "${config.summaryPrompt}\n\nTitle: ${contentItem.title}\nSource: ${contentItem.sourceName}\nContent: ${contentItem.content}"
        return generateSummary(prompt)
    }

    private suspend fun generateSummary(prompt: String): String {
        // Use the specified model or default to gemini-2.5-flash-preview-05-20
        val modelName = config.model

        val requestUrl = "$apiUrl/$modelName:generateContent?key=${config.apiKey}"

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            ),
            generationConfig = GeminiGenerationConfig(
                maxOutputTokens = config.maxTokens,
                temperature = config.temperature
            )
        )

        return try {
            val response = client.post(requestUrl) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val geminiResponse: GeminiResponse = response.body()
            geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Failed to generate summary."
        } catch (e: Exception) {
            "Error generating summary: ${e.message}"
        }
    }

    @Serializable
    private data class GeminiRequest(
        val contents: List<GeminiContent>,
        val generationConfig: GeminiGenerationConfig
    )

    @Serializable
    private data class GeminiContent(
        val parts: List<GeminiPart>
    )

    @Serializable
    private data class GeminiPart(
        val text: String
    )

    @Serializable
    private data class GeminiGenerationConfig(
        val maxOutputTokens: Int,
        val temperature: Double
    )

    @Serializable
    private data class GeminiResponse(
        val candidates: List<GeminiCandidate> = emptyList()
    )

    @Serializable
    private data class GeminiCandidate(
        val content: GeminiContent
    )
}
