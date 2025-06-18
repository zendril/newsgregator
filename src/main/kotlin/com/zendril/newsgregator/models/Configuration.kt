package com.zendril.newsgregator.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File

/**
 * Main configuration class that holds all settings for the application
 */
@Serializable
data class Configuration(
    val sources: List<SourceConfig> = emptyList(),
    val global: GlobalConfig = GlobalConfig(),
    val llm: LlmConfig = LlmConfig()
)

/**
 * Global configuration settings that apply to all sources
 */
@Serializable
data class GlobalConfig(
    val cacheTimeMinutes: Int = 60,
    val userAgent: String = "Newsgregator/1.0"
)

/**
 * Enum representing the different types of content sources
 */
@Serializable
enum class SourceType {
    @SerialName("youtube")
    YOUTUBE,

    @SerialName("reddit")
    REDDIT,

    @SerialName("rss")
    RSS,

    @SerialName("scrape")
    SCRAPE
}

/**
 * Enum representing the different client types for RSS feeds
 */
@Serializable
enum class ClientType {
    @SerialName("http")
    HTTP,

    @SerialName("playwright")
    PLAYWRIGHT
}

/**
 * Base configuration for a content source
 */
@Serializable
data class SourceConfig(
    val name: String,
    val type: SourceType,
    val url: String? = null,

    // YouTube specific properties
    val channelId: String? = null,
    val channelHandle: String? = null, // Channel handle (e.g., "@AICodeKing")

    // Reddit specific properties
    val subreddit: String? = null,
    val sortBy: String? = null,

    // RSS specific properties
    val clientType: ClientType = ClientType.HTTP, // Default to HTTP client

    // Common properties
    val maxResults: Int = 10,
    val timeRangeDays: Int = 2, // Number of days to retrieve content for

    // Web scraping specific properties
    val selectors: ScrapingSelectors? = null
)

/**
 * CSS selectors for web scraping
 */
@Serializable
data class ScrapingSelectors(
    val articles: String,
    val title: String,
    val content: String,
    val date: String? = null
)

/**
 * Configuration for LLM (Large Language Model) integration
 */
@Serializable
data class LlmConfig(
    val provider: String = "gemini",
    var apiKey: String = "", // Making apiKey mutable so we can update it at runtime
    val model: String = "gemini-2.5-flash",
//    val maxTokens: Int = 500,
//    val temperature: Double = 0.7,
    val summaryPromptFile: String = "summary-prompt.md"
) {
    @Transient
    private var loadedPrompt: String? = null
    
    /**
     * Gets the summary prompt by loading it from the file if needed
     */
    fun getSummaryPrompt(): String {
        if (loadedPrompt == null) {
            try {
                loadedPrompt = File(summaryPromptFile).readText()
            } catch (e: Exception) {
                // Fail and exit the program if file cannot be read
                println("Error: Could not read summary prompt file: ${e.message}")
                System.exit(1) // Exit the program with non-zero status code
            }
        }
        return loadedPrompt!!
    }
}
