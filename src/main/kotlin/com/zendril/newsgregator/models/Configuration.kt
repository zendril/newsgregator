package com.zendril.newsgregator.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
 * Base configuration for a content source
 */
@Serializable
data class SourceConfig(
    val name: String,
    val type: SourceType,
    val url: String? = null,

    // YouTube specific properties
    val channelId: String? = null,
    val playlistId: String? = null,

    // Reddit specific properties
    val subreddit: String? = null,
    val sortBy: String? = null,

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
    val model: String = "gemini-2.5-flash-preview-05-20",
    val maxTokens: Int = 500,
    val temperature: Double = 0.7,
    var dryRun: Boolean = false,
    val summaryPrompt: String = "Summarize the following news content in a concise way:"
)
