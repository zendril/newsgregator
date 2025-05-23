package com.zendril.newsgregator.llm

import com.zendril.newsgregator.models.ContentItem
import com.zendril.newsgregator.models.LlmConfig
import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.content
import dev.shreyaspatil.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of LLM service using Google's Gemini model with generative-ai-kmp
 */
class GeminiService(private val config: LlmConfig) : LlmService {
    // Initialize the Generative AI model
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = config.model,
            apiKey = config.apiKey,
//            generationConfig = generationConfig {
//                temperature = config.temperature.toFloat()
//                topK = 40
//                topP = 0.95f
//                maxOutputTokens = config.maxTokens
//            }
        )
    }

    override suspend fun summarizeContent(contentItems: List<ContentItem>): String {
        if (contentItems.isEmpty()) {
            return "No content to summarize."
        }

        // For multiple items, create a combined prompt with all content
        val combinedContent = contentItems.joinToString("\n\n") { 
            "Title: ${it.title}\nSource: ${it.sourceName}\nSource Url: ${it.url}\nContent: ${it.content}"
        }

//        val prompt = "${config.summaryPrompt}\n\n$combinedContent\n\nPlease provide a concise summary of these articles, highlighting the key points and any common themes."
        val prompt = "${config.summaryPrompt}\n\n$combinedContent\n\nPlease provide a briefing showing each article concisely, highlighting the key points and including a link to the article, post or video. At the beginning, provide an overview summary for all articles highlighting key themes. Following that please provide a list of anything that appears to be a product announcement or release. Provide in Github markdown format."
        return generateSummary(prompt)
    }

    override suspend fun summarizeContentItem(contentItem: ContentItem): String {
        val prompt = "${config.summaryPrompt}\n\nTitle: ${contentItem.title}\nSource: ${contentItem.sourceName}\nContent: ${contentItem.content}"
        return generateSummary(prompt)
    }

    private suspend fun generateSummary(prompt: String): String {
        return try {
            withContext(Dispatchers.IO) {
                val response = generativeModel.generateContent(content {
                    text(prompt)
                })
                response.text ?: "Failed to generate summary."
            }
        } catch (e: Exception) {
            "Error generating summary: ${e.message}"
            "Error generating summary: ${e.message}\n${e.stackTraceToString().lines().take(5).joinToString("\n")}"        }
    }
}
