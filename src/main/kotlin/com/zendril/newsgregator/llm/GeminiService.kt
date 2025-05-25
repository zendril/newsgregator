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

    // Create a new instance of GenerativeModel
    private fun createFreshModel(): GenerativeModel {
        return GenerativeModel(
            modelName = config.model,
            apiKey = config.apiKey,
            generationConfig = generationConfig {
                responseMimeType = "text/plain"
            }
//            generationConfig = generationConfig {
//                temperature = config.temperature.toFloat()
//                topK = 40
//                topP = 0.95f
//                maxOutputTokens = config.maxTokens
//            }
        )
    }

    override suspend fun summarizeContent(contentItems: List<ContentItem>, prompt: String?): String {
        if (contentItems.isEmpty()) {
            return "No content to summarize."
        }
    
        // For multiple items, create a combined prompt with all content
        val combinedContent = contentItems.joinToString("\n\n") { 
            "Title: ${it.title}\nSource: ${it.sourceName}\nSource Url: ${it.url}\nContent: ${it.content}\nMetadata: ${it.metadata}"
        }
    
        val effectivePrompt = "${prompt ?: config.summaryPrompt}\n\n$combinedContent"
        return generateSummary(effectivePrompt)
    }

    override suspend fun summarizeContentItem(contentItem: ContentItem, prompt: String?): String {
        val effectivePrompt = "${prompt ?: config.summaryPrompt}\n\nTitle: ${contentItem.title}\nSource: ${contentItem.sourceName}\nContent: ${contentItem.content}\nMetadata: ${contentItem.metadata}"
        return generateSummary(effectivePrompt)
    }
    
    override suspend fun transcribeVideo(url: String, customPrompt: String?): String {
        val defaultPrompt = "Provide a full transcript for this video: $url"
        val prompt = customPrompt ?: defaultPrompt
        println("DEBUG: Sending prompt to llm: $prompt")

        val message = content("user") {
            fileData(
                uri = "$url",
                mimeType = "video/*"
            )
            text("Provide a full transcript for this video: ")
        }

        return try {
            withContext(Dispatchers.IO) {
                // Use a fresh model instance for each transcription request
                val freshModel = createFreshModel()
                val response = freshModel.generateContent(message)
                val llmResponse = response.text ?: "Failed to generate summary."
                println("DEBUG: llmResponse: $llmResponse")
                llmResponse
            }
        } catch (e: Exception) {
            "Error generating summary: ${e.message}"
            "Error generating summary: ${e.message}\n${e.stackTraceToString().lines().take(5).joinToString("\n")}"
        }

    }

    private suspend fun generateSummary(prompt: String): String {
        return try {
            withContext(Dispatchers.IO) {
                // Use a fresh model instance for each summary request
                val freshModel = createFreshModel()
                val response = freshModel.generateContent(content {
                    text(prompt)
                })
                response.text ?: "Failed to generate summary."
            }
        } catch (e: Exception) {
            "Error generating summary: ${e.message}"
            "Error generating summary: ${e.message}\n${e.stackTraceToString().lines().take(5).joinToString("\n")}"        }
    }
}
